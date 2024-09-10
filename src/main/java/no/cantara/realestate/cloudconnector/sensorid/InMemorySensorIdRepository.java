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
    public void addAll(List<SensorId> sensorIds) {
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
