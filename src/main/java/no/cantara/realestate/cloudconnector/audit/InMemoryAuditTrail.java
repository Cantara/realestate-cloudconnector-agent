package no.cantara.realestate.cloudconnector.audit;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAuditTrail implements AuditTrail {
    private final Map<String, AuditState> states = new ConcurrentHashMap<>();

    @Override
    public void logCreated(String sensorId, String detail) {
        AuditState sensorIdState = getNullSafeState(sensorId);
        sensorIdState.setCreated(sensorId, detail);
    }

    @Override
    public void logSubscribed(String sensorId, String detail) {
        AuditState sensorIdState = getNullSafeState(sensorId);
        sensorIdState.setSubscribed(sensorId, detail);
    }

    @Override
    public void logObservedTrend(String sensorId, String detail) {
        AuditState sensorIdState = getNullSafeState(sensorId);
        AuditEvent event = new AuditEvent(sensorId, AuditEvent.Type.OBSERVED, "Trend:" + detail);
        sensorIdState.setLastObservedTrendEvent(event);
    }

    @Override
    public void logObservedStream(String sensorId, String detail) {
        AuditState sensorIdState = getNullSafeState(sensorId);
        AuditEvent event = new AuditEvent(sensorId, AuditEvent.Type.OBSERVED, "Stream:" + detail);
        sensorIdState.setLastObservedStreamEvent(event);
    }

    @Override
    public void logObservedPresentValue(String sensorId, String detail) {
        AuditState sensorIdState = getNullSafeState(sensorId);
        AuditEvent event = new AuditEvent(sensorId, AuditEvent.Type.OBSERVED, "PresentValue:" + detail);
        sensorIdState.setLastObservedPresentValueEvent(event);
    }

    @Override
    public Optional<AuditState> getState(String sensorId) {
        return Optional.ofNullable(states.get(sensorId));
    }

    @Override
    public AuditState getNullSafeState(String sensorId) {
        {
            if (sensorId == null || sensorId.isBlank()) {
                return null;
            }
            AuditState sensorIdState = states.get(sensorId);
            if (sensorIdState == null) {
                sensorIdState = new AuditState(sensorId);
                states.put(sensorId, sensorIdState);
            }
            return sensorIdState;
        }
    }

    @Override
    public Map<String, AuditState> getAll() {
        return Collections.unmodifiableMap(states);
    }

    @Override
    public void logFailed(String sensorId, String comment) {
        AuditState sensorIdState = getNullSafeState(sensorId);
        sensorIdState.addEvent(new AuditEvent(sensorId, AuditEvent.Type.FAILED, comment));
    }
}

