package no.cantara.realestate.cloudconnector.sensorid;


import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorIdQuery;
import no.cantara.realestate.sensors.SensorSystem;

import java.util.List;
import java.util.Map;

public interface SensorIdRepository<T extends SensorId> {

    void add(SensorId sensorId);
    void addAll(List<T> sensorIds);
    List<T> find(SensorSystem system);

    List<T> find(SensorIdQuery sensorIdQuery);
    List<T> all();
    Map<SensorSystem,List<T>> allBySystem();



    long size();
}
