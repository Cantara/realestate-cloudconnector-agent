package no.cantara.realestate.cloudconnector.rec;

import no.cantara.realestate.rec.RecRepository;
import no.cantara.realestate.rec.RecTags;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorSystem;
import no.cantara.realestate.sensors.SensorSystemId;

import java.util.*;
import java.util.stream.Collectors;

public class RecRepositoryInMemory implements RecRepository {

    private Map<SensorId, RecTags> recTags = new HashMap<>();

    @Override
    public void addRecTags(SensorId sensorId, RecTags tags) {
        recTags.put(sensorId, tags);
    }

    @Override
    public List<RecTags> getRecTagsByTwinId(String twinId) {
        return recTags.keySet().stream().filter(sensorId -> sensorId.getTwinId() != null && sensorId.getTwinId().equals(twinId))
                .map(recTags::get)
                .collect(Collectors.toList());
    }

    @Override
    public RecTags getRecTagsBySensorId(SensorId sensorId) {
        return recTags.get(sensorId);
    }

    @Override
    public List<RecTags> findBySensorSystem(SensorSystem sensorSystem) {
        return recTags.values().stream().filter(r -> r.getSensorSystem().equals(sensorSystem.name())).collect(Collectors.toList());
    }

    @Override
    public List<RecTags> findBySensorSystemId(SensorSystemId sensorSystemId) {
        SensorSystem sensorSystem = sensorSystemId.getSensorSystem();
        List<String> identifierKeys = sensorSystemId.getIdentifierKeys();
        Set<SensorId> keys = recTags.keySet();
        List<SensorId> applicableSensorIds = new ArrayList<>();
        for (SensorId sensorId : keys) {
            if (sensorId.getSensorSystem().equals(sensorSystem)) {
                boolean matches = true;
                for (String key : identifierKeys) {
                    String value = sensorId.getIdentifier(key);
                    if (value == null || !value.equals(sensorSystemId.getIdentifier(key))) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    applicableSensorIds.add(sensorId);
                }
            }
        }
        if (applicableSensorIds.isEmpty()) {
            return Collections.emptyList();
        }
        // Filter recTags based on the applicable sensor IDs
        List<RecTags> filteredRecTags = new ArrayList<>();
        for (SensorId applicableSensorId : applicableSensorIds) {
            RecTags tags = recTags.get(applicableSensorId);
            if (tags != null) {
                filteredRecTags.add( tags);
            }
        }
        // Return the values of the filtered recTags
        return filteredRecTags;
    }

    @Override
    public long size() {
        return recTags.size();
    }
}
