package no.cantara.realestate.cloudconnector.mappedid;

import no.cantara.realestate.rec.RecObject;
import no.cantara.realestate.sensors.MappedIdQuery;
import no.cantara.realestate.sensors.MappedSensorId;
import no.cantara.realestate.sensors.UniqueKey;

import java.util.List;

public interface MappedIdRepository {

    void add(MappedSensorId sensorId);
    void addAll(List<MappedSensorId> sensorIds);
    List<MappedSensorId> find(UniqueKey mappingKey);

    List<MappedSensorId> find(MappedIdQuery mappedIdQuery);

    long updateRec(RecObject recObject);

    long size();
}
