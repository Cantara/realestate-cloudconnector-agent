package no.cantara.realestate.cloudconnector.ingestion;

import no.cantara.realestate.SensorId;
import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.plugins.ingestion.PresentValueIngestionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SimulatorPresentValueIngestionService implements PresentValueIngestionService {

    private boolean isInitialized = false;
    private long numberOfMessagesImported = 0;
    private long numberOfMessagesFailed = 0;

    private List<SensorId> sensorIds = new ArrayList<>();
    private ObservationListener observationListener;

    @Override
    public String getName() {
        return "SimulatorPresentValueIngestionService";
    }

    @Override
    public boolean initialize(Properties properties) {
        isInitialized = true;
        return true;
    }

    @Override
    public void openConnection(ObservationListener observationListener) {
        this.observationListener = observationListener;
    }

    @Override
    public void closeConnection() {

    }

    @Override
    public void addSubscriptions(List<SensorId> list) {

    }

    @Override
    public void addSubscription(SensorId sensorId) {

    }

    @Override
    public void removeSubscription(SensorId sensorId) {

    }


    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public boolean isHealthy() {

        return (isInitialized && observationListener != null);
    }

    @Override
    public long getNumberOfMessagesImported() {
        return numberOfMessagesImported;
    }

    @Override
    public long getNumberOfMessagesFailed() {
        return numberOfMessagesFailed;
    }

    @Override
    public void ingestPresentValues() {
        int max = 35;
        int min = 10;
        for (SensorId sensorId : sensorIds) {
            ObservedValue observedValue = new ObservedValue(sensorId, ((Math.random() * (max - min)) + min));
            observationListener.observedValue(observedValue);
        }
    }
}
