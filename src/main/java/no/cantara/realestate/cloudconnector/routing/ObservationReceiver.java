package no.cantara.realestate.cloudconnector.routing;

import no.cantara.realestate.observations.ConfigMessage;
import no.cantara.realestate.observations.ConfigValue;
import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.ObservedValue;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class ObservationReceiver implements ObservationListener {
    private static final Logger log = getLogger(ObservationReceiver.class);

    private long observedValueCount = 0;
    private long observedConfigValueCount = 0;
    private long observedConfigMessageCount = 0;
    @Override
    public void observedValue(ObservedValue observedValue) {
        log.trace("Observed value: {}", observedValue);
        addObservedValueCount();
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
