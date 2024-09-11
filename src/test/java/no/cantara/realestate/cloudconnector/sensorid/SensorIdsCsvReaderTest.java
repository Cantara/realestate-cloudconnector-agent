package no.cantara.realestate.cloudconnector.sensorid;

import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorSystem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SensorIdsCsvReaderTest {

    @Test
    void parseSensorIds() {
        String filePath = getClass().getResource("/config/sensorids.csv").getPath();
        List<SensorId> sensorIds = SensorIdsCsvReader.parseSensorIds(filePath);
        assertNotNull(sensorIds);
        assertEquals(4, sensorIds.size());
        SensorId expectedSensorId = new SensorId("Sensor-def-102", SensorSystem.metasys, Map.of("metasysObjectId","567-890"));
        assertTrue(sensorIds.contains(expectedSensorId));
    }
}