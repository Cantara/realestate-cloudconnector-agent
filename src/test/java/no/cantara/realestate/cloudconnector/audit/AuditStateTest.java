package no.cantara.realestate.cloudconnector.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditStateTest {

    private final String sensorId = "sensor123";
    private AuditState auditState;

    @BeforeEach
    void setUp() {
        auditState = new AuditState(sensorId);
    }

    @Test
    void addEvent() {
        assertEquals(1, auditState.allEventsByTimestamp().size());
        auditState.addEvent(new AuditEvent(sensorId, AuditEvent.Type.FOUND, "Sensor found"));
        assertEquals(2, auditState.allEventsByTimestamp().size());
        auditState.addEvent(new AuditEvent(sensorId, AuditEvent.Type.MISSING, "Sensor missing"));
        assertEquals(3, auditState.allEventsByTimestamp().size());
    }

    @Test
    void validateMax10EventsPrType() {
        assertEquals(1,auditState.allEventsByTimestamp().size());
        for (int i = 1; i <= 15; i++) {
            auditState.setSubscribed(sensorId, "Event " + i);
        }
        assertEquals(11,auditState.allEventsByTimestamp().size());
        for (int i = 1; i <= 15; i++) {
            auditState.addEvent(new AuditEvent(sensorId, AuditEvent.Type.FAILED, "TrendNotFound-test-" + i));
        }
        for (AuditEvent auditEvent : auditState.allEventsByTimestamp()) {
            System.out.println(auditEvent.getType() + " " + auditEvent.getDetail());
        }
        assertEquals(21,auditState.allEventsByTimestamp().size());
    }

    @Test
    void setSubscribed() {
        assertEquals(1,auditState.allEventsByTimestamp().size());
        auditState.setSubscribed(sensorId, "Subscribed to sensor");
        assertEquals(2,auditState.allEventsByTimestamp().size());
    }
}