package no.cantara.realestate.cloudconnector.rec;

import no.cantara.realestate.rec.RecTags;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorSystem;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import no.cantara.realestate.sensors.desigo.DesigoSensorSystemId;
import no.cantara.realestate.sensors.metasys.MetasysSensorId;
import no.cantara.realestate.sensors.metasys.MetasysSensorSystemId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RecRepositoryTest {


    private RecRepositoryInMemory recRepo;
    @BeforeEach
    void setup() {
        recRepo = new RecRepositoryInMemory();
        addTestData();
    }

    @Test
    void findByDesigoSensorSystemId() {
        assertEquals(1, recRepo.findBySensorSystemId(new DesigoSensorSystemId("desigoId1", "desigoPropertyId1")).size());
        assertEquals(0, recRepo.findBySensorSystemId(new DesigoSensorSystemId("desigoId1", "desigoPropertyId2")).size());
        List<RecTags> recTags = recRepo.findBySensorSystemId(new DesigoSensorSystemId("desigoId1", "desigoPropertyId1"));
        assertEquals(1, recTags.size());
        RecTags recTag = recTags.get(0);
        assertEquals("Sensor-desigo1", recTag.getSensorId());
        assertEquals("Sensor-desigo1", recTag.getTwinId());
        assertEquals(SensorSystem.desigo.name(), recTag.getSensorSystem());
    }

    @Test
    void findByMetasysSensorSystemId() {
        assertEquals(1, recRepo.findBySensorSystemId(new MetasysSensorSystemId("metasysObjectId1")).size());
        assertEquals(1, recRepo.findBySensorSystemId(new MetasysSensorSystemId("metasysObjectId2")).size());
        assertEquals(0, recRepo.findBySensorSystemId(new MetasysSensorSystemId("metasysObjectId3")).size());
    }

    @Test
    void findBySensorSystem() {
        assertEquals(2, recRepo.findBySensorSystem(SensorSystem.desigo).size());
        assertEquals(2, recRepo.findBySensorSystem(SensorSystem.metasys).size());
    }

    @Test
    void findBySensorId() {
        SensorId sensorId = new MetasysSensorId("Sensor-metasys1","metasysObjectId1");
        RecTags recTag = recRepo.getRecTagsBySensorId(sensorId);
        assertNotNull(recTag);
        assertEquals("Sensor-metasys1", recTag.getTwinId());
    }

    @Test
    void findByTwinId() {
        List<RecTags> recTagsList = recRepo.getRecTagsByTwinId("Sensor-metasys1");
        assertNotNull(recTagsList);
        assertEquals(1, recTagsList.size());
        assertEquals("Sensor-metasys1", recTagsList.get(0).getTwinId());
    }

    private void addTestData() {
        // Create test data for Metasys sensors
        MetasysSensorId metasysSensor1 = new MetasysSensorId("Sensor-metasys1","metasysObjectId1");

        RecTags metasysTags1 = new RecTags("Sensor-metasys1");
        metasysTags1.setRealEstate("RE001");
        metasysTags1.setBuilding("Building-A");
        metasysTags1.setFloor("1");
        metasysTags1.setSection("East");
        metasysTags1.setPlacementRoom("101");
        metasysTags1.setServesRoom("101");
        metasysTags1.setClimateZone("CZ-1");
        metasysTags1.setSensorType("Temperature");
        metasysTags1.setSensorSystem("metasys");
        metasysTags1.setSensorId("Sensor-metasys1");

        recRepo.addRecTags(metasysSensor1, metasysTags1);

        MetasysSensorId metasysSensor2 = new MetasysSensorId("Sensor-metasys2","metasysObjectId2");

        RecTags metasysTags2 = new RecTags("Sensor-metasys2");
        metasysTags2.setRealEstate("RE001");
        metasysTags2.setBuilding("Building-A");
        metasysTags2.setFloor("1");
        metasysTags2.setSection("East");
        metasysTags2.setPlacementRoom("101");
        metasysTags2.setServesRoom("101");
        metasysTags2.setClimateZone("CZ-1");
        metasysTags2.setSensorType("Temperature");
        metasysTags2.setSensorSystem("metasys");
        metasysTags2.setSensorId("Sensor-metasys1");

        recRepo.addRecTags(metasysSensor2, metasysTags2);

        // Create test data for Desigo sensors
        DesigoSensorId desigoSensor1 = new DesigoSensorId("Sensor-desigo1","desigoId1", "desigoPropertyId1");

        RecTags desigoTags1 = new RecTags("Sensor-desigo1");
        desigoTags1.setRealEstate("RE001");
        desigoTags1.setBuilding("Building-B");
        desigoTags1.setFloor("2");
        desigoTags1.setSection("West");
        desigoTags1.setPlacementRoom("201");
        desigoTags1.setServesRoom("201");
        desigoTags1.setClimateZone("CZ-2");
        desigoTags1.setSensorType("Temperature");
        desigoTags1.setSensorSystem("desigo");
        desigoTags1.setSensorId("Sensor-desigo1");

        recRepo.addRecTags(desigoSensor1, desigoTags1);
        DesigoSensorId desigoSensor2 = new DesigoSensorId("Sensor-desigo2","desigoId2", "desigoPropertyId2");

        RecTags desigoTags2 = new RecTags("Sensor-desigo2");
        desigoTags2.setRealEstate("RE001");
        desigoTags2.setBuilding("Building-B");
        desigoTags2.setFloor("2");
        desigoTags2.setSection("West");
        desigoTags2.setPlacementRoom("201");
        desigoTags2.setServesRoom("201");
        desigoTags2.setClimateZone("CZ-2");
        desigoTags2.setSensorType("Temperature");
        desigoTags2.setSensorSystem("desigo");
        desigoTags2.setSensorId("Sensor-desigo1");

        recRepo.addRecTags(desigoSensor2, desigoTags2);

        // Create test data for TFM sensors
        SensorId tfmSensor1 = new SensorId("tfm-sensor-1");
        tfmSensor1.setSensorSystem(SensorSystem.tfm);
        tfmSensor1.addIdentifier("deviceId", "TFM-01");
        tfmSensor1.addIdentifier("objectId", "CO2-SENSOR-1");

        RecTags tfmTags1 = new RecTags("twin-tfm-1");
        tfmTags1.setRealEstate("RE002");
        tfmTags1.setBuilding("Building-C");
        tfmTags1.setFloor("1");
        tfmTags1.setSection("North");
        tfmTags1.setPlacementRoom("101");
        tfmTags1.setServesRoom("101,102,103");
        tfmTags1.setClimateZone("CZ-3");
        tfmTags1.setSensorType("CO2");
        tfmTags1.setSensorSystem("tfm");
        tfmTags1.setSensorId("tfm-sensor-1");

        recRepo.addRecTags(tfmSensor1, tfmTags1);

        // Add a second TFM sensor in the same building but different floor
        SensorId tfmSensor2 = new SensorId("tfm-sensor-2");
        tfmSensor2.setSensorSystem(SensorSystem.tfm);
        tfmSensor2.addIdentifier("deviceId", "TFM-01");
        tfmSensor2.addIdentifier("objectId", "TEMP-SENSOR-2");

        RecTags tfmTags2 = new RecTags("twin-tfm-2");
        tfmTags2.setRealEstate("RE002");
        tfmTags2.setBuilding("Building-C");
        tfmTags2.setFloor("2");
        tfmTags2.setSection("North");
        tfmTags2.setPlacementRoom("201");
        tfmTags2.setServesRoom("201");
        tfmTags2.setElectricityZone("EZ-1");
        tfmTags2.setSensorType("Temperature");
        tfmTags2.setSensorSystem("tfm");
        tfmTags2.setSensorId("tfm-sensor-2");

        recRepo.addRecTags(tfmSensor2, tfmTags2);
    }
}
