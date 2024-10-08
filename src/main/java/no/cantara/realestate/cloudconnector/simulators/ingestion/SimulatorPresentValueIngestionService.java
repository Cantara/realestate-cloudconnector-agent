package no.cantara.realestate.cloudconnector.simulators.ingestion;

import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.ObservedPresentValue;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.ingestion.PresentValueIngestionService;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.sensors.SensorId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SimulatorPresentValueIngestionService implements PresentValueIngestionService {

    private boolean isInitialized = false;
    private long numberOfMessagesImported = 0;
    private long numberOfMessagesFailed = 0;

    private List<SensorId> sensorIds = new ArrayList<>();
    private ObservationListener observationListener;
    private NotificationListener notificationListener;
    private Instant lastImportedTime;

    @Override
    public String getName() {
        return "SimulatorPresentValueIngestionService";
    }

    @Override
    public boolean initialize(PluginConfig properties) {
        isInitialized = true;
        return true;
    }

    @Override
    public void openConnection(ObservationListener observationListener, NotificationListener notificationListener) {
        this.observationListener = observationListener;
        this.notificationListener = notificationListener;
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
        int max = 35;
        int min = 10;
        for (SensorId sensorId : sensorIds) {
            ObservedPresentValue observedValue = new ObservedPresentValue(sensorId, ((Math.random() * (max - min)) + min));
            observationListener.observedValue(observedValue);
            addIngestionCount();
            updateLastImportedTime();
        }
    }

    private void addIngestionCount() {
        if (numberOfMessagesImported == Long.MAX_VALUE) {
            numberOfMessagesImported = 0;
        }
        numberOfMessagesImported++;
    }

    protected synchronized void updateLastImportedTime() {
        lastImportedTime = Instant.ofEpochMilli(System.currentTimeMillis());
    }

    @Override
    public Instant getWhenLastMessageImported() {
        return lastImportedTime;
    }
}
