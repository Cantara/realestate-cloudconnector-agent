package no.cantara.realestate.cloudconnector.sensorid;

import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorSystem;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.slf4j.LoggerFactory.getLogger;

class SensorIdsCsvReaderTest {
    private static final Logger log = getLogger(SensorIdsCsvReaderTest.class);

    @Test
    void parseSensorIds() {
        String filePath = getClass().getResource("/config/validateSensorids.csv").getPath();
        filePath = URLDecoder.decode(filePath); //filePath.replace("%20", " ");
        assertTrue(Files.exists(Paths.get(filePath)),"File is not acessible " + filePath);
        List<SensorId> sensorIds = SensorIdsCsvReader.parseSensorIds(filePath);
        assertNotNull(sensorIds);
        assertEquals(4, sensorIds.size());
        SensorId expectedSensorId = new SensorId("Sensor-def-102", SensorSystem.metasys, Map.of("metasysObjectId","567-890"));
        assertTrue(sensorIds.contains(expectedSensorId));
    }

    @Test
    void validateDesigoIdentifiers() {
        String desigoIdentifiers = "desigoId:System1:GmsDevice_2_101444_12165555;desigoPropertyId:RAQual_Present_Value";
        Map record = Map.of(SensorIdsCsvReader.HEADER_IDENTIFICATOR, desigoIdentifiers);
        Map<String, String> identifiers = SensorIdsCsvReader.findIdentifiers("desigoId", record);
        assertEquals(2, identifiers.size());
        assertEquals("System1:GmsDevice_2_101444_12165555", identifiers.get("desigoId"));
        assertEquals("RAQual_Present_Value", identifiers.get("desigoPropertyId"));
    }

    @Test
    void singleIdentifier() {
        String singleIdentifier = "metasysObjectId:567-890";
        Map record = Map.of(SensorIdsCsvReader.HEADER_IDENTIFICATOR, singleIdentifier);
        Map<String, String> identifiers = SensorIdsCsvReader.findIdentifiers("metasysObjectId", record);
        assertEquals(1, identifiers.size());
        assertEquals("567-890", identifiers.get("metasysObjectId"));
    }

    @Test
    void emptyIdentifier() {
        String singleIdentifier = "567-890";
        Map record = Map.of(SensorIdsCsvReader.HEADER_IDENTIFICATOR, singleIdentifier);
        Map<String, String> identifiers = SensorIdsCsvReader.findIdentifiers("metasysObjectId", record);
        assertEquals(1, identifiers.size());
        assertEquals("567-890", identifiers.get("metasysObjectId"));
    }
}