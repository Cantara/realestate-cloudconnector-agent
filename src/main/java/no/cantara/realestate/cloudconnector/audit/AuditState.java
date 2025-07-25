package no.cantara.realestate.cloudconnector.audit;


import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class AuditState {

    private Map<AuditEvent.Type, LinkedList<AuditEvent>> events = new HashMap<>();
    private AuditEvent lastObservedTrendEvent = null;
    private AuditEvent lastObservedStreamEvent = null;
    private AuditEvent lastObservedPresentValueEvent = null;
    private volatile Instant lastObservedTimestamp = null;
    private AuditEvent lastPulledFromQueueEvent = null;
    private AuditEvent lastDistributedEvent = null;

    public AuditState(String sensorId) {
        // Initialize with a FOUND event to indicate the sensorId is being tracked
        addEvent(new AuditEvent(sensorId, AuditEvent.Type.INNITIALIZED, "Tracking started for sensorId: " + sensorId));
    }
    public AuditState(String sensorId, AuditEvent.Type type, String detail) {
        AuditEvent event = new AuditEvent(sensorId, type, detail);
        addEvent(event);
    }

    public void addEvent(AuditEvent event) {
        if (event == null ) {
            return;
        }
        AuditEvent.Type eventType = event.getType();
        LinkedList<AuditEvent> eventsForType = events.get(eventType);
        if (eventsForType == null) {
            eventsForType = new LinkedList<>();
            events.put(eventType, eventsForType);
        }
        eventsForType.add(event);
        if (eventsForType.size() > 10) {
            eventsForType.removeFirst();
        }
    }

    public void setSubscribed(String sensorId, String comment) {
        AuditEvent event = new AuditEvent(sensorId, AuditEvent.Type.SUBSCRIBED, comment);
        addEvent(event);
    }


    public void setCreated(String sensorId, String comment) {
        addEvent(new AuditEvent(sensorId, AuditEvent.Type.CREATED, comment));
    }

    public List<AuditEvent> allEventsByTimestamp() {
        List<AuditEvent> allEvents = events.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        if (lastObservedTrendEvent != null) {
            allEvents.add(lastObservedTrendEvent);
        }
        if (lastObservedStreamEvent != null) {
            allEvents.add(lastObservedStreamEvent);
        }
        if (lastObservedPresentValueEvent != null) {
            allEvents.add(lastObservedPresentValueEvent);
        }
        if (lastPulledFromQueueEvent != null) {
            allEvents.add(lastPulledFromQueueEvent);
        }
        if (lastDistributedEvent != null) {
            allEvents.add(lastDistributedEvent);
        }
        return allEvents.stream().sorted(Comparator.comparing(AuditEvent::getTimestamp)).collect(Collectors.toList());
    }

    public AuditEvent getLastObservedPresentValueEvent() {
        return lastObservedPresentValueEvent;
    }

    public void setLastObservedPresentValueEvent(AuditEvent lastObservedPresentValueEvent) {
        this.lastObservedPresentValueEvent = lastObservedPresentValueEvent;
        setLastObservedTimestamp(lastObservedPresentValueEvent.getTimestamp());
    }

    public AuditEvent getLastObservedStreamEvent() {
        return lastObservedStreamEvent;
    }

    public void setLastObservedStreamEvent(AuditEvent lastObservedStreamEvent) {
        this.lastObservedStreamEvent = lastObservedStreamEvent;
        setLastObservedTimestamp(lastObservedStreamEvent.getTimestamp());
    }

    public Instant getLastObservedTimestamp() {
        return lastObservedTimestamp;
    }

    public void setLastObservedTimestamp(Instant lastObservedTimestamp) {
        this.lastObservedTimestamp = lastObservedTimestamp;
    }

    public AuditEvent getLastObservedTrendEvent() {
        return lastObservedTrendEvent;
    }

    public void setLastObservedTrendEvent(AuditEvent lastObservedTrendEvent) {
        this.lastObservedTrendEvent = lastObservedTrendEvent;
        setLastObservedTimestamp(lastObservedTrendEvent.getTimestamp());
    }

    public void setLastPulledFromQueue(AuditEvent event) {
        this.lastPulledFromQueueEvent = event;
    }

    public void setLastDistributed(AuditEvent event) {
        this.lastDistributedEvent = event;
    }
}

