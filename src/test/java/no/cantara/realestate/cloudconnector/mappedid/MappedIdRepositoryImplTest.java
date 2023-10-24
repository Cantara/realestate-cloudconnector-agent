package no.cantara.realestate.cloudconnector.mappedid;

import no.cantara.realestate.cloudconnector.sensors.simulated.SimulatedCo2Sensor;
import no.cantara.realestate.semantics.rec.SensorRecObject;
import no.cantara.realestate.sensors.MappedSensorId;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorType;
import no.cantara.realestate.sensors.UniqueKey;
import no.cantara.realestate.sensors.metasys.MetasysSensorId;
import no.cantara.realestate.sensors.tfm.Tfm;
import no.cantara.realestate.sensors.tfm.TfmUniqueKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static no.cantara.realestate.cloudconnector.RealestateCloudconnectorApplication.buildRecStub;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MappedIdRepositoryImplTest {

    MappedIdRepository repository;
    final Tfm tfm = new Tfm("TFM-RY02101");

    @BeforeEach
    void setUp() {
        repository = new MappedIdRepositoryImpl();
    }

    @Test
    void add() {
        assertEquals(0, repository.size());
        MetasysSensorId sensorId = new MetasysSensorId("dbId", "objectRef");
        SensorRecObject recObject = new SensorRecObject("recid");
        MappedSensorId mappedSensorId = new MappedSensorId(sensorId, recObject);
        repository.add(mappedSensorId);
        assertEquals(1, repository.size());
    }

    @Test
    void find() {
        assertEquals(0, repository.size());
        MetasysSensorId sensorId = new MetasysSensorId("dbId", "objectRef");
        SensorRecObject recObject = new SensorRecObject("recid");
        recObject.setTfm(tfm);
        MappedSensorId mappedSensorId = new MappedSensorId(sensorId, recObject);
        repository.add(mappedSensorId);
        MetasysSensorId tempSensor = new MetasysSensorId("dbId2", "objectRef2");
        SensorRecObject tempRec = new SensorRecObject("tempRec");
        MappedSensorId tempSensorId = new MappedSensorId(tempSensor, tempRec);
        repository.add(tempSensorId);
        assertEquals(2, repository.size());
        UniqueKey tfmKey = new TfmUniqueKey(tfm);
        List<MappedSensorId> matchingSersorIds = repository.find(tfmKey);
        assertNotNull(matchingSersorIds);
        assertEquals(1, matchingSersorIds.size());
        assertEquals("recid", matchingSersorIds.get(0).getRec().getRecId());
    }

    @Test
    void updateRec() {
        assertEquals(0, repository.size());
        MetasysSensorId sensorId = new MetasysSensorId("dbId", "objectRef");
        SensorRecObject recObject = new SensorRecObject("recid");
        MappedSensorId mappedSensorId = new MappedSensorId(sensorId, recObject);
        repository.add(mappedSensorId);

        recObject.setTfm(tfm);
        repository.updateRec(recObject);
        assertEquals(1, repository.size());
    }

    @Test
    void stubMappedIdsTest() {
        SensorId simulatedCo2Sensor = new SimulatedCo2Sensor("1");
        SensorRecObject recRoom1 = buildRecStub("room1", SensorType.co2);
        MappedSensorId mappedSimulatedCo2Sensor = new MappedSensorId(simulatedCo2Sensor, recRoom1);
        repository.add(mappedSimulatedCo2Sensor);
//        repository.updateRec(recObject);
        assertEquals(1, repository.size());
        UniqueKey uniqueKey = simulatedCo2Sensor.getMappingKey();

        List<MappedSensorId> allMappings = ((MappedIdRepositoryImpl)repository).getAll();
        assertEquals(1, allMappings.size());
        MappedSensorId shouldMatch = allMappings.get(0);
        System.out.println("***" + shouldMatch.getSensorId().getMappingKey().getKey());
        System.out.println("***" + uniqueKey.getKey());
        assertEquals(shouldMatch.getSensorId().getMappingKey().getKey(), uniqueKey.getKey());
//        assertTrue(shouldMatch.getSensorId().getMappingKey().equals(uniqueKey));
        List<MappedSensorId> mappedSensorIds = repository.find(uniqueKey);
        assertNotNull(mappedSensorIds);
        assertEquals(1, mappedSensorIds.size());

    }
}