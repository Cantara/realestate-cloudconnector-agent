package no.cantara.realestate.cloudconnector.routing;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import no.cantara.realestate.observations.*;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class ObservationsRepository implements ObservationListener {
    private static final Logger log = getLogger(ObservationsRepository.class);

    public static final int MAX_CONCURRENT_OBSERVATIONS = 10000;

    private long observedValueCount = 0;
    private long observedConfigValueCount = 0;
    private long observedConfigMessageCount = 0;
    private LinkedBlockingDeque<ObservedValue> observedValuesQueue;

    private MetricRegistry metricRegistry;
    private Meter observedConfigValueMeter;
    private Meter observedConfigMessageMeter;
    private Meter observedValueReceivedMeter;
    private Meter presentValueReceived;
    private final Meter trendedValueReceivedMeter;
    private final Meter streamValueReceivedMeter;
    private Instant lastObservedTime;


    public ObservationsRepository(MetricRegistry metricRegistry) {
       this(MAX_CONCURRENT_OBSERVATIONS, metricRegistry);
    }
    public ObservationsRepository(int maxConcurrentObservations, MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        log.info("Creating ObservedValues queue with maxConcurrentObservations={}", maxConcurrentObservations);
        observedValuesQueue = new LinkedBlockingDeque<>(maxConcurrentObservations);
        this.metricRegistry = metricRegistry;
        presentValueReceived = metricRegistry.meter("PresentValueObservationReceived");
        trendedValueReceivedMeter = metricRegistry.meter("TrendedValueObservationReceived");
        streamValueReceivedMeter = metricRegistry.meter("StreamValueObservationReceived");
        observedConfigValueMeter = metricRegistry.meter("ConfigReceived");
        observedConfigMessageMeter = metricRegistry.meter("ConfigMessageReceived");
        observedValueReceivedMeter = metricRegistry.meter("ObservationReceived");
    }

    @Override
    public void observedValue(ObservedValue observedValue) {
        log.trace("Observed value: {}", observedValue);
        addObservedValueCount();
        boolean isObserved = false;
        try {
            log.trace("Add to observedQueue {}", observedValue);
            isObserved = observedValuesQueue.offer(observedValue, 1, TimeUnit.MILLISECONDS);
            log.trace("Attempt to add {}, estimated totalSize {}, was added [{}]", observedValue, observedValuesQueue.size(), isObserved);
            if (isObserved) {
                updateLastObservedTime();
                if (observedValue instanceof ObservedPresentValue) {
                    presentValueReceived.mark();
                } else if (observedValue instanceof ObservedTrendedValue) {
                    trendedValueReceivedMeter.mark();
                } else if (observedValue instanceof ObservedStreamValue) {
                    streamValueReceivedMeter.mark();
                } else {
                    observedValueReceivedMeter.mark();
                }
            }
        } catch (InterruptedException e) {
            log.warn("Could not add observedValue {}",observedValue, e);
        }
    }

    public boolean hasObservedValues() {
        boolean hasObservations = observedValuesQueue.size() > 0;
        //log.debug("hasObservations {}", hasObservations);
        return hasObservations;
    }

    /**
     * Hand an observation back to the head of the queue after a distribution attempt that may
     * succeed on retry (the connection was not established yet). It goes in front of anything that
     * arrived in the meantime, so per-sensor ordering survives the retry.
     *
     * <p>Never blocks: if the queue filled up while the attempt was in flight, the observation is
     * dropped and {@code false} is returned, and the caller decides what to log. Blocking here
     * would stall the distributor thread, which is the only consumer of the queue.
     *
     * @param observedValue the observation that was taken with {@link #takeFirstObservedValue()}
     * @return {@code true} if it is back at the head of the queue, {@code false} if the queue is full
     */
    public boolean requeueFirst(ObservedValue observedValue) {
        if (observedValue == null) {
            return false;
        }
        boolean requeued = observedValuesQueue.offerFirst(observedValue);
        if (!requeued) {
            log.warn("Queue is full ({} observations). Dropping {} instead of requeueing it.", observedValuesQueue.size(), observedValue);
        }
        return requeued;
    }

    public ObservedValue takeFirstObservedValue() {
        try {
            ObservedValue observedValue = observedValuesQueue.poll(1,TimeUnit.MILLISECONDS);
            //log.debug("takeFirst-observedMethod {}", observedMethod.toString());
            return observedValue;
        } catch (InterruptedException e) {
            log.trace("Interupted - Nothing to take {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void observedConfigValue(ConfigValue configValue) {
        log.trace("Observed config value: {}", configValue);
        addObservedConfigValueCount();
        observedConfigValueMeter.mark();
    }

    @Override
    public void observedConfigMessage(ConfigMessage configMessage) {
        log.trace("Observed config message: {}", configMessage);
        addObservedConfigMessageCount();
        observedConfigMessageMeter.mark();
    }



    public long getObservedValueCount() {
        return observedValueCount;
    }

    private void addObservedValueCount() {
        if (this.observedValueCount == Long.MAX_VALUE) {
            this.observedValueCount = 0;
        }
        this.observedValueCount++;
    }
    protected void setObservedValueCount(long observedValueCount) {
        this.observedValueCount = observedValueCount;
    }

    public long getObservedConfigValueCount() {
        return observedConfigValueCount;
    }

    private void addObservedConfigValueCount() {
        if (this.observedConfigValueCount == Long.MAX_VALUE) {
            this.observedConfigValueCount = 0;
        }
        this.observedConfigValueCount++;
    }

    public void setObservedConfigValueCount(long observedConfigValueCount) {
        this.observedConfigValueCount = observedConfigValueCount;
    }

    public long getObservedConfigMessageCount() {
        return observedConfigMessageCount;
    }

    private void addObservedConfigMessageCount() {
        if (this.observedConfigMessageCount == Long.MAX_VALUE) {
            this.observedConfigMessageCount = 0;
        }
        this.observedConfigMessageCount++;
    }

    public void setObservedConfigMessageCount(long observedConfigMessageCount) {
        this.observedConfigMessageCount = observedConfigMessageCount;
    }

    public boolean isHealthy() {
        return true;
    }

    public long getObservedValuesQueueSize() {
        return observedValuesQueue.size();
    }

    protected synchronized void updateLastObservedTime() {
        lastObservedTime = Instant.ofEpochMilli(System.currentTimeMillis());
    }

    @Override
    public Instant getWhenLastMessageObserved() {
        return lastObservedTime;
    }
}
