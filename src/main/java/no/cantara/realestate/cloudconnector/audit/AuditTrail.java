package no.cantara.realestate.cloudconnector.audit;

import java.util.Map;
import java.util.Optional;

public interface AuditTrail {
    void logCreated(String sensorId, String detail);

    void logSubscribed(String sensorId, String detail);

    void logObservedTrend(String sensorId, String detail);

    void logObservedStream(String sensorId, String detail);

    void logObservedPresentValue(String sensorId, String detail);

    Optional<AuditState> getState(String sensorId);

    AuditState getNullSafeState(String sensorId);

    Map<String, AuditState> getAll();

    void logFailed(String sensorId, String comment);
}
