package no.cantara.realestate.cloudconnector.simulators.distribution;

import no.cantara.realestate.observations.ObservationMessage;
import no.cantara.realestate.plugins.distribution.DistributionService;

import java.time.Instant;
import java.util.Properties;

public class SimulatorDistributionService implements DistributionService {

    private boolean isInitialized = false;
    private long numberOfMessagesPublished = 0;
    private long numberOfMessagesFailed = 0;
    private Instant lastDistributedTime;
    @Override
    public String getName() {
        return "SimulatorDistributionService";
    }

    @Override
    public void initialize(Properties properties) {
        isInitialized = true;
    }

    @Override
    public void publish(ObservationMessage observationMessage) {
        numberOfMessagesPublished++;
        lastDistributedTime = Instant.ofEpochMilli(System.currentTimeMillis());
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public long getNumberOfMessagesPublished() {
        return numberOfMessagesPublished;
    }

    @Override
    public long getNumberOfMessagesFailed() {
        return numberOfMessagesFailed;
    }

    @Override
    public long getNumberOfMessagesInQueue() {
        return 0;
    }

    @Override
    public Instant getWhenLastMessageDistributed() {
        return lastDistributedTime;
    }
}
