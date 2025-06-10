package no.cantara.realestate.cloudconnector.routing;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
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
    private long sleepPeriod;
    private long observedValueDistributedCount;
    MetricRegistry metricRegistry;
    Meter distributedMeter;
    private boolean healthy = true;

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

    @Override
    public void run() {
        log.info("Starting ObservationDistributor");
        do {
            try {
                while (observationsRepository.hasObservedValues()) {
                    ObservedValue observedValue = observationsRepository.takeFirstObservedValue();
                    addSemanticsAndDistribute(observedValue);
                }
                Thread.sleep(sleepPeriod);
            } catch (RealEstateException e) {
                this.setHealthy(false);
                log.warn("Failed to distribute observedValue", e);
            } catch (InterruptedException e) {
                //Interupted sleep. No probblem, and ignored.
            }
        } while (true);

    }

    public void setHealthy(boolean healty) {
        this.healthy = healty;
    }



    protected void addSemanticsAndDistribute(ObservedValue observedValue) {
        log.trace("Fetched observedValue {} from the queue", observedValue);
        auditLog.trace("Distribute__Observed__{}__{}__{}__{}__{}", observedValue.getClass(), observedValue.getSensorId().getId(), observedValue.getSensorId().getId(),observedValue.getValue(), observedValue.getObservedAt());

        SensorId sensorId = observedValue.getSensorId();
        if (sensorId != null) {
            String twinId = sensorId.getTwinId();
            auditTrail.logObservationFetchedFromQueue(twinId,"");
        }
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
            azureObservationsClient.publish(observationMessage);
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

    public boolean isHealthy() {
        return healthy;
    }
}
