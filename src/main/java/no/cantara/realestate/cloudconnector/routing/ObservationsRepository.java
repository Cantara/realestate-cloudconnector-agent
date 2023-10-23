package no.cantara.realestate.cloudconnector.routing;

import no.cantara.realestate.observations.ConfigMessage;
import no.cantara.realestate.observations.ConfigValue;
import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.ObservedValue;
import org.slf4j.Logger;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class ObservationsRepository implements ObservationListener {
    private static final Logger log = getLogger(ObservationsRepository.class);

    public static final int MAX_CONCURRENT_OBSERVATIONS = 10000;

    private long observedValueCount = 0;
    private long observedConfigValueCount = 0;
    private long observedConfigMessageCount = 0;
    LinkedBlockingDeque<ObservedValue> observedValuesQueue;

    public ObservationsRepository() {
       this(MAX_CONCURRENT_OBSERVATIONS);
    }
    public ObservationsRepository(int maxConcurrentObservations) {
        log.info("Creating ObservedValues queue with maxConcurrentObservations={}", maxConcurrentObservations);
        observedValuesQueue = new LinkedBlockingDeque<>(maxConcurrentObservations);
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
        } catch (InterruptedException e) {
            log.warn("Could not add observedValue {}",observedValue, e);
        }
    }
    public boolean hasObservedValues() {
        boolean hasObservations = observedValuesQueue.size() > 0;
        //log.debug("hasObservations {}", hasObservations);
        return hasObservations;
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
    }

    @Override
    public void observedConfigMessage(ConfigMessage configMessage) {
        log.trace("Observed config message: {}", configMessage);
        addObservedConfigMessageCount();
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
}
