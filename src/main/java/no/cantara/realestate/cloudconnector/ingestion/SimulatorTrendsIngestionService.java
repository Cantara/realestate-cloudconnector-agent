package no.cantara.realestate.cloudconnector.ingestion;

import no.cantara.realestate.SensorId;
import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.plugins.ingestion.PresentValueIngestionService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SimulatorTrendsIngestionService implements PresentValueIngestionService {

    private boolean isInitialized = false;
    private long numberOfMessagesImported = 0;
    private long numberOfMessagesFailed = 0;

    private List<SensorId> sensorIds = new ArrayList<>();
    private ObservationListener observationListener;

    @Override
    public String getName() {
        return "SimulatorTrendsIngestionService";
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
        sensorIds.addAll(list);
    }

    @Override
    public void addSubscription(SensorId sensorId) {
        sensorIds.add(sensorId);
    }

    @Override
    public void removeSubscription(SensorId sensorId) {
        sensorIds.remove(sensorId);
    }

    @Override
    public long getSubscriptionsCount() {
        return sensorIds.size();
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
        //Simulate CO2 sensor
        int max = 1100;
        int min = 415;
        for (SensorId sensorId : sensorIds) {
            int co2Value = (int) ((Math.random() * (max - min)) + min);
            ObservedValue olderValue = new ObservedValue(sensorId, co2Value);
            olderValue.setObservedAt(Instant.now().minusSeconds(60*5));
            observationListener.observedValue(olderValue);
            ObservedValue latestValue = new ObservedValue(sensorId, co2Value + 100);
            observationListener.observedValue(latestValue);
        }
    }
}
