package no.cantara.realestate.cloudconnector.sensorid;

import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorSystem;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import no.cantara.realestate.sensors.metasys.MetasysSensorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemorySensorIdRepositoryTest {

    private InMemorySensorIdRepository repository;
    private SensorSystem system1;
    private SensorSystem system2;
    private SensorId sensor1;
    private SensorId sensor2;
    private SensorId sensor3;

    @BeforeEach
    void setUp() {
        repository = new InMemorySensorIdRepository();
        system1 = SensorSystem.metasys;
        system2 = SensorSystem.desigo;
        sensor1 = new MetasysSensorId("objectId1", "objectReference1");
        sensor2 = new MetasysSensorId("objectId2", "objectReference2");
        sensor3 = new DesigoSensorId("Sensor-desigo1","desigoId1", "desigoPropertyId1");
        repository.addAll(List.of(sensor1, sensor2, sensor3));
    }

    @Test
    void testAdd() {
        SensorId sensor4 = new MetasysSensorId("objectId4", "objectReference4");
        repository.add(sensor4);
        assertTrue(repository.all().contains(sensor4));
    }

    @Test
    void testAddAll() {
        SensorId sensor4 = new MetasysSensorId("objectId4", "objectReference4");
        SensorId sensor5 = new MetasysSensorId("objectId5", "objectReference5");
        repository.addAll(List.of(sensor4, sensor5));
        assertTrue(repository.all().containsAll(List.of(sensor4, sensor5)));
    }

    @Test
    void testFind() {
        List<SensorId> result = repository.find(system1);
        assertEquals(2, result.size());
        assertTrue(result.contains(sensor1));
        assertTrue(result.contains(sensor2));
    }

    @Test
    void testFindWithNullSystem() {
        List<SensorId> result = repository.find((SensorSystem) null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testAll() {
        List<SensorId> result = repository.all();
        assertEquals(3, result.size());
    }

    @Test
    void testAllBySystem() {
        Map<SensorSystem, List<SensorId>> result = repository.allBySystem();
        assertEquals(2, result.size());
        assertTrue(result.get(system1).containsAll(List.of(sensor1, sensor2)));
        assertTrue(result.get(system2).contains(sensor3));
    }

    @Test
    void testSize() {
        assertEquals(3, repository.size());
    }

    @Test
    void testEnsureNoDuplicates() {
        repository.add(sensor1);
        assertEquals(3, repository.size());
        repository.add(new MetasysSensorId("objectId1", "objectReference1"));
        assertEquals(3, repository.size());
    }

    @Test
    void testWithIdentifier() {
        SensorId sensorWithMetasysObjectId = new MetasysSensorId("Sensor-metasys3", "objectId3");
        repository.add(sensorWithMetasysObjectId);

        repository.add( new MetasysSensorId("Sensor-metasys4", "objectId4"));
        repository.add( new MetasysSensorId("Sensor-metasys5", "objectId5"));
        repository.add(new DesigoSensorId("Sensor-desigo2","desigoId2", "desigoPropertyId2"));

        List<SensorId> result = repository.find(MetasysSensorId.METASYS_OBJECT_ID, "objectId3");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Sensor-metasys3", result.get(0).getTwinId());
        assertEquals(0,repository.find(MetasysSensorId.METASYS_OBJECT_ID, "objectId6").size());

        List<SensorId> desigo = repository.find(DesigoSensorId.DESIGO_ID, "desigoId2", DesigoSensorId.DESIGO_PROPERTY_ID, "desigoPropertyId2");
        assertNotNull(result);
        assertEquals(1, desigo.size());
        assertEquals("Sensor-desigo2", desigo.get(0).getTwinId());
        assertEquals(0,repository.find(DesigoSensorId.DESIGO_ID, "desigoId3").size());
        assertEquals(0, repository.find(DesigoSensorId.DESIGO_ID, "desigoId3", DesigoSensorId.DESIGO_PROPERTY_ID, "desigoPropertyId3").size());
    }
}