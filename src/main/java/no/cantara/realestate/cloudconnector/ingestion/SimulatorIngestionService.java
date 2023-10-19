package no.cantara.realestate.cloudconnector.ingestion;

import no.cantara.realestate.cloudconnector.observations.ObservationMesstageStubs;
import no.cantara.realestate.observations.ObservationMessage;
import no.cantara.realestate.plugins.ingestion.IngestionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SimulatorIngestionService implements IngestionService {

    private boolean isInitialized = false;
    private long numberOfMessagesImported = 0;
    private long numberOfMessagesFailed = 0;
    @Override
    public String getName() {
        return "SimulatorIngestionService";
    }

    @Override
    public void initialize(Properties properties) {
        isInitialized = true;
    }

    @Override
    public List<ObservationMessage> readAll() {
        ArrayList<ObservationMessage> stubs = new ArrayList<>();
        stubs.add(ObservationMesstageStubs.buildStubObservation());
        return stubs;
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
    public long getNumberOfMessagesImported() {
        return numberOfMessagesImported;
    }

    @Override
    public long getNumberOfMessagesFailed() {
        return numberOfMessagesFailed;
    }

}
