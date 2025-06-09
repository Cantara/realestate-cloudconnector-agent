package no.cantara.realestate.cloudconnector.sensorid;


import no.cantara.realestate.rec.RecObject;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorIdQuery;
import no.cantara.realestate.sensors.SensorSystem;
import no.cantara.realestate.sensors.UniqueKey;

import java.util.List;
import java.util.Map;

public interface SensorIdRepository {

    void add(SensorId sensorId);
    void addAll(List<SensorId> sensorIds);
    List<SensorId> find(SensorSystem system);

    List<SensorId> find(SensorIdQuery sensorIdQuery);
    List<SensorId> all();
    Map<SensorSystem,List<SensorId>> allBySystem();



    long size();
}
