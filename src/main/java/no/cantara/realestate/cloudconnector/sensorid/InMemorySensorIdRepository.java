package no.cantara.realestate.cloudconnector.sensorid;

import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorIdQuery;
import no.cantara.realestate.sensors.SensorSystem;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class InMemorySensorIdRepository implements SensorIdRepository {
    private static final Logger log = getLogger(InMemorySensorIdRepository.class);
    private Set<SensorId> sensorIds;

    public InMemorySensorIdRepository() {
        sensorIds = new HashSet<>();
    }

    @Override
    public void add(SensorId sensorId) {
        sensorIds.add(sensorId);
    }

    @Override
    public void addAll(List sensorIds) {
        this.sensorIds.addAll(sensorIds);
    }


    @Override
    public List<SensorId> find(SensorSystem system) {
        if (system == null) {
            return List.of();
        }
        return sensorIds.stream()
                .filter(sensorId -> sensorId.getSensorSystem() != null && system.equals(sensorId.getSensorSystem()))
                .collect(Collectors.toList());
    }

    /**
     * Find sensors that match a specific identifier key and value.
     *
     * @param identifierKey The name of the identifier to match
     * @param identifierValue The value of the identifier to match
     * @return A list of sensors matching the criteria
     */
    public List<SensorId> find(String identifierKey, String identifierValue) {
        if (identifierKey == null || identifierValue == null) {
            return Collections.emptyList();
        }

        return sensorIds.stream()
                .filter(sensorId -> matchesidentifier(sensorId, identifierKey, identifierValue))
                .collect(Collectors.toList());
    }

    /**
     * Find sensors that match two specific identifier identifiers and values.
     *
     * @param identifierName1 The name of the first identifier to match
     * @param identifierValue1 The value of the first identifier to match
     * @param identifierName2 The name of the second identifier to match
     * @param identifierValue2 The value of the second identifier to match
     * @return A list of sensors matching all criteria
     */
    public List<SensorId> find(String identifierName1, String identifierValue1,
                               String identifierName2, String identifierValue2) {
        if (identifierName1 == null || identifierValue1 == null ||
                identifierName2 == null || identifierValue2 == null) {
            return Collections.emptyList();
        }

        return sensorIds.stream()
                .filter(sensorId -> matchesidentifier(sensorId, identifierName1, identifierValue1) &&
                        matchesidentifier(sensorId, identifierName2, identifierValue2))
                .collect(Collectors.toList());
    }

    /**
     * Helper method to check if a sensor matches a specific identifier name and value.
     *
     * @param sensor The sensor to check
     * @param identifierKey The name of the identifier to match
     * @param identifierValue The value of the identifier to match
     * @return true if the sensor has the identifier with the specified value
     */
    private boolean matchesidentifier(SensorId sensor, String identifierKey, String identifierValue) {
        if (sensor == null) {
            return false;
        }
        boolean isAMatch = identifierKey != null && sensor.getIdentifier(identifierKey) != null &&
                sensor.getIdentifier(identifierKey).equals(identifierValue);
        return isAMatch;
        
    }

    @Override
    public List<SensorId> find(SensorIdQuery sensorIdQuery) {
        return List.of();
    }

    @Override
    public List<SensorId> all() {
        if (sensorIds == null) {
            return List.of();
        }
        return new ArrayList<>(sensorIds);
    }

    @Override
    public Map<SensorSystem, List<SensorId>> allBySystem() {
        if (sensorIds == null) {
            return Map.of();
        }
        return sensorIds.stream()
                .filter(sensorId -> sensorId.getSensorSystem() != null)
                .collect(Collectors.groupingBy(SensorId::getSensorSystem));
    }

    @Override
    public long size() {
        if (sensorIds != null) {
            return sensorIds.size();
        }
        return 0;
    }
}
