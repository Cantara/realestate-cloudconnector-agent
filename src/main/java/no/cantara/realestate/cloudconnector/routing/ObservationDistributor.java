package no.cantara.realestate.cloudconnector.routing;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import no.cantara.realestate.ExceptionStatusType;
import no.cantara.realestate.MqttUnavailableException;
import no.cantara.realestate.RealEstateException;
import no.cantara.realestate.azure.AzureObservationDistributionClient;
import no.cantara.realestate.cloudconnector.audit.AuditTrail;
import no.cantara.realestate.cloudconnector.semantics.ObservationMapper;
import no.cantara.realestate.cloudconnector.simulators.distribution.ObservationDistributionServiceStub;
import no.cantara.realestate.observations.ObservationMessage;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.plugins.distribution.DistributionService;
import no.cantara.realestate.rec.RecRepository;
import no.cantara.realestate.rec.RecTags;
import no.cantara.realestate.sensors.SensorId;
import org.slf4j.Logger;

import java.util.List;

import static no.cantara.realestate.cloudconnector.RealestateCloudconnectorApplication.auditLog;
import static org.slf4j.LoggerFactory.getLogger;

public class ObservationDistributor implements Runnable {

    private static final Logger log = getLogger(ObservationDistributor.class);
    private final ObservationsRepository observationsRepository;
    private final List<DistributionService> distributionServices;
    private AzureObservationDistributionClient azureObservationsClient = null;
    private ObservationDistributionServiceStub observationDistributionServiceStub = null;
    private AuditTrail auditTrail = null;
    //    private final MappedIdRepository mappedIdRepository;
    private final RecRepository recRepository;
    private static final long DEFAULT_SLEEP_PERIOD_MS = 100;
    /** Pause before retrying a publish that failed with {@code RETRY_MAY_FIX_ISSUE} (SDK is mid-reconnect). */
    private static final long DEFAULT_RETRY_DELAY_MS = 500;
    /** First and longest pause between close and re-open after the link was reported unstable. */
    private static final long DEFAULT_RECONNECT_BACKOFF_MIN_MS = 1_000;
    private static final long DEFAULT_RECONNECT_BACKOFF_MAX_MS = 60_000;
    private long sleepPeriod;
    private long retryDelayMs = DEFAULT_RETRY_DELAY_MS;
    private long reconnectBackoffMinMs = DEFAULT_RECONNECT_BACKOFF_MIN_MS;
    private long reconnectBackoffMaxMs = DEFAULT_RECONNECT_BACKOFF_MAX_MS;
    private long observedValueDistributedCount;
    MetricRegistry metricRegistry;
    Meter distributedMeter;
    /**
     * Health is derived, not stored. The loop owns exactly one signal: whether the last hand-off to
     * the distribution service was accepted. Delivery (the IoT Hub ack) is the service's own signal
     * and is read through {@link DistributionService#isHealthy()}. See {@link #isHealthy()}.
     */
    private volatile boolean handoffFaulted = false;
    /** Set after a close so that a later retryable failure re-opens instead of just waiting. */
    private volatile boolean reopenPending = false;
    private volatile Thread distributorThread;

    public ObservationDistributor(ObservationsRepository observationsRepository, List<DistributionService> distributionServices, RecRepository recRepository, MetricRegistry metricRegistry, AuditTrail auditTrail) {
        this.observationsRepository = observationsRepository;
        this.distributionServices = distributionServices;
        this.recRepository = recRepository;
//        this.mappedIdRepository = mappedIdRepository;
        sleepPeriod = DEFAULT_SLEEP_PERIOD_MS;
        this.metricRegistry = metricRegistry;
        for (DistributionService distributionService : distributionServices) {
            if (distributionService instanceof AzureObservationDistributionClient) {
                this.azureObservationsClient = (AzureObservationDistributionClient) distributionService;
                log.info("Using AzureObservationDistributionClient for distribution");
            } else if (distributionService instanceof ObservationDistributionServiceStub) {
                this.observationDistributionServiceStub = (ObservationDistributionServiceStub) distributionService;
                log.info("Using ObservationDistributionServiceStub for distribution");
            } else {
                log.info("Using DistributionService {} for distribution", distributionService.getName());
            }
        }
        distributedMeter = metricRegistry.meter("ObservationsDistributed");
        this.auditTrail = auditTrail;
        /*
        metricRegistry.register(MetricRegistry.name(ObservationDistributor.class, "ObservationsDistributed", "total"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return observedValueDistributedCount;
                    }
                });

         */
    }

    /**
     * Drain the queue, one observation at a time, and recover from every failure mode of
     * {@code publish} without leaving the loop:
     * <ul>
     *     <li>{@code MqttUnavailableException}: the link is unstable or was force-closed. Requeue,
     *     close and re-open with exponential backoff.</li>
     *     <li>{@code RealEstateException(RETRY_MAY_FIX_ISSUE)}: the SDK is mid-reconnect (routinely
     *     the hourly SAS-token renewal). Requeue and wait briefly.</li>
     *     <li>Any other {@code RealEstateException} or {@code RuntimeException}: the message itself is
     *     the problem. Log and drop it, then continue with the next one.</li>
     * </ul>
     * Only an {@code Error} or an interrupt ends the loop, and {@link #isHealthy()} reports that.
     */
    @Override
    public void run() {
        distributorThread = Thread.currentThread();
        log.info("Starting ObservationDistributor");
        long reconnectBackoffMs = reconnectBackoffMinMs;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    while (observationsRepository.hasObservedValues() && !Thread.currentThread().isInterrupted()) {
                        ObservedValue observedValue = observationsRepository.takeFirstObservedValue();
                        if (observedValue == null) {
                            break;
                        }
                        try {
                            addSemanticsAndDistribute(observedValue);
                            handoffFaulted = false;
                            reconnectBackoffMs = reconnectBackoffMinMs;
                        } catch (MqttUnavailableException mue) {
                            handoffFaulted = true;
                            log.warn("Failed to publish observationMessage to AzureObservationDistributionClient. Reason: {}. Will close and re-open the connection.", mue.getMessage());
                            observationsRepository.requeueFirst(observedValue);
                            reconnectBackoffMs = closeAndReopen(reconnectBackoffMs);
                            break;
                        } catch (RealEstateException ree) {
                            if (ree.getStatusType() == ExceptionStatusType.RETRY_MAY_FIX_ISSUE) {
                                handoffFaulted = true;
                                log.warn("Failed to publish observationMessage to AzureObservationDistributionClient. Reason: {}. Will retry.", ree.getMessage());
                                observationsRepository.requeueFirst(observedValue);
                                if (reopenPending) {
                                    reconnectBackoffMs = reopen(reconnectBackoffMs);
                                } else {
                                    Thread.sleep(retryDelayMs);
                                }
                                break;
                            }
                            if (ree.getStatusType() != ExceptionStatusType.data_error) {
                                handoffFaulted = true;
                            }
                            log.error("Failed to publish observationMessage to AzureObservationDistributionClient. Reason: {}. Dropping {}", ree.getMessage(), observedValue, ree);
                        } catch (RuntimeException re) {
                            log.error("Unexpected error while distributing observedValue. Dropping {}", observedValue, re);
                        }
                    }
                    Thread.sleep(sleepPeriod);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            log.warn("ObservationDistributor stopped. interrupted={}", Thread.currentThread().isInterrupted());
        }
    }

    /**
     * Close the connection, then re-open it after a backoff pause.
     *
     * @return the backoff to use next time: reset to the minimum on success, doubled on failure
     */
    private long closeAndReopen(long backoffMs) throws InterruptedException {
        if (azureObservationsClient == null) {
            return backoffMs;
        }
        try {
            azureObservationsClient.closeConnection();
        } catch (RuntimeException e) {
            log.warn("closeConnection() failed. Will still attempt to re-open.", e);
        }
        reopenPending = true;
        return reopen(backoffMs);
    }

    private long reopen(long backoffMs) throws InterruptedException {
        if (azureObservationsClient == null) {
            reopenPending = false;
            return backoffMs;
        }
        log.info("Waiting {} ms before re-opening the connection to Azure IoT Hub", backoffMs);
        Thread.sleep(backoffMs);
        try {
            azureObservationsClient.openConnection();
            reopenPending = false;
            log.info("Re-opened the connection to Azure IoT Hub");
            return reconnectBackoffMinMs;
        } catch (RuntimeException e) {
            long nextBackoffMs = Math.min(backoffMs * 2, reconnectBackoffMaxMs);
            log.error("Re-opening the connection to Azure IoT Hub failed. Next attempt in {} ms.", nextBackoffMs, e);
            return nextBackoffMs;
        }
    }

    /**
     * Override the loop's pauses. Package-private: production uses the defaults, tests shorten them
     * so recovery scenarios run in milliseconds.
     */
    void configureTiming(long sleepPeriodMs, long retryDelayMs, long reconnectBackoffMinMs, long reconnectBackoffMaxMs) {
        this.sleepPeriod = sleepPeriodMs;
        this.retryDelayMs = retryDelayMs;
        this.reconnectBackoffMinMs = reconnectBackoffMinMs;
        this.reconnectBackoffMaxMs = reconnectBackoffMaxMs;
    }

    protected void addSemanticsAndDistribute(ObservedValue observedValue) {
        if (observedValue == null) {
            log.trace("ObservedValue is null, skipping distribution");
            return;
        }
        SensorId sensorId = observedValue.getSensorId();
        if (sensorId == null) {
            log.warn("ObservedValue has no sensorId and cannot be distributed. Dropping {}", observedValue);
            return;
        }
        log.trace("Fetched observedValue {} from the queue", observedValue);

        auditLog.trace("Distribute__Observed__{}__{}__{}__{}__{}", observedValue.getClass(), sensorId.getId(), sensorId.getId(), observedValue.getValue(), observedValue.getObservedAt());
        auditTrail.logObservationFetchedFromQueue(sensorId.getTwinId(), "");

        RecTags recTags = recRepository.getRecTagsBySensorId(sensorId);
        ObservationMessage observationMessage = null;
        if (recTags == null) {
            auditLog.trace("Distribute__TwinIdObservationMessage__{}__{}", sensorId.getId(), sensorId.getClass());
            observationMessage = ObservationMapper.buildTwinIdObservation(observedValue);
        } else {
            auditLog.trace("Distribute__RecObservationMessage_{}__{}", sensorId.getId(), sensorId.getClass());
            observationMessage = ObservationMapper.buildRecTagsObservation(observedValue, recTags);
        }
//        AzureObservationDistributionClient azureObservationsClient = new AzureObservationDistributionClient();
        if (azureObservationsClient != null) {
            // Failures propagate to run(), which decides between requeue, re-open and drop (#427).
            azureObservationsClient.publish(observationMessage);
        }
        if (observationDistributionServiceStub != null) {
            observationDistributionServiceStub.publish(observationMessage);
        }
        //Support for InfluxDB
        if (recTags != null) {
            //TODO send to InfluxDB
        }
        //Support for Mqtt
        //TODO send to Mqtt

        // AzureIoTHub

        //Disable MappedIdRepository for now
        /*
        UniqueKey uniqueKey = sensorId.getMappingKey();
        List<MappedSensorId> mappedIds = mappedIdRepository.find(uniqueKey);
        if (mappedIds.size() > 1) {
            log.info("Found {} mappedIds for uniqueKey {}", mappedIds.size(), uniqueKey);
            return;
        } else if (mappedIds.size() == 0) {
            log.trace("No mappedIds found for uniqueKey {}", uniqueKey);
            return;
        }
        MappedSensorId mappedSensorId = mappedIds.get(0);
        log.trace("Found mappedSensorId {} for uniqueKey {}", mappedSensorId, uniqueKey);
        auditLog.trace("Distribute__Mapped__{}__{}__{}__{}__{}", mappedSensorId.getClass(), mappedSensorId.getSensorId().getId(), observedValue.getSensorId().getId(), observedValue.getValue(), observedValue.getObservedAt());
        ObservationMessage observationMessage = ObservationMapper.buildRecObservation(mappedSensorId, observedValue);
        for (DistributionService distributionService : distributionServices) {
            auditLog.trace("Distribute__Publish__{}__{}__{}__{}__{}", distributionService.getName(), observationMessage.getClass(), sensorId.getId(), observationMessage.getValue(), observationMessage.getObservationTime());
            distributedMeter.mark();
            distributionService.publish(observationMessage);
        }

         */
        addObservedValueDistributedCount();

    }

    protected void addObservedValueDistributedCount() {
        if (observedValueDistributedCount == Integer.MAX_VALUE) {
            observedValueDistributedCount = 0;
        }
        observedValueDistributedCount++;
    }

    public long getObservedValueDistributedCount() {
        return observedValueDistributedCount;
    }

    /**
     * Healthy when the loop is running, the last hand-off to the distribution service was accepted,
     * and every distribution service reports healthy (for Azure: last IoT Hub ack succeeded, circuit
     * breaker closed, link stable). Nothing is latched: the next accepted hand-off or the next ack
     * flips it back without anyone having to call a setter.
     */
    public boolean isHealthy() {
        boolean servicesHealthy = distributionServices.stream().allMatch(DistributionService::isHealthy);
        return isLoopAlive() && !handoffFaulted && servicesHealthy;
    }

    /**
     * @return {@code true} while {@link #run()} is executing on a live thread. {@code false} before
     * start, after an interrupt, and after an {@code Error} escaped the loop.
     */
    public boolean isLoopAlive() {
        Thread thread = distributorThread;
        return thread != null && thread.isAlive();
    }

    /**
     * @return {@code true} while the distribution service rejects hand-offs. Exposed for the
     * {@code /health} probe so the cause of a {@code DOWN} is visible without reading the log.
     */
    public boolean isHandoffFaulted() {
        return handoffFaulted;
    }
}
