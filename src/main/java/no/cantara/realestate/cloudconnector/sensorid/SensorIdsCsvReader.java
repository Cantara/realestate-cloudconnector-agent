package no.cantara.realestate.cloudconnector.sensorid;

import no.cantara.realestate.csv.CsvCollection;
import no.cantara.realestate.csv.CsvReader;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorSystem;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import no.cantara.realestate.sensors.ecostruxure.EcoStrxureSensorId;
import no.cantara.realestate.sensors.metasys.MetasysSensorId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.cantara.realestate.utils.StringUtils.hasValue;
import static no.cantara.realestate.utils.StringUtils.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;

public class SensorIdsCsvReader {
    private static final Logger log = getLogger(SensorIdsCsvReader.class);
    public static final String KEY_VALUE_SEPARATOR = ":";
    private static final String IDENTIFIERS_SEAPARATOR = ";";

    public static void main(String[] args) throws IOException {
        List<SensorId> sensorIds = parseSensorIds("src/test/resources/config/sensorids.csv");
        for (SensorId sensorId : sensorIds) {
            System.out.println(sensorId);
        }
    }

    /**
     * Parse sensor ids from a csv file
     * Expected format:
     * SensorId;SensorSystem;Identificator
     * Identificator is either
     *  1 a single value or
     *  2 a key-value pair separated by ":"
     *  3 multiple key-value pairs separated by ";"
     *
     * @param filePath path to the csv file
     * @return list of sensor ids
     */
    @NotNull
    public static List<SensorId> parseSensorIds(String filePath) {
        List<SensorId> sensorIds = new ArrayList<>();

        try {
            CsvCollection csvCollection = CsvReader.parse(filePath);
            csvCollection.getRecords().forEach(record -> {
                String sensorIdValue = record.get("SensorId");
                String systemName = record.get("SensorSystem");
                if (systemName == null) {
                    log.warn("SensorSystem is null for sensorId: {}", sensorIdValue);
                    return;
                }
                Map<String, String> identifiers = new HashMap<>();
                SensorSystem sensorSystem = SensorSystem.valueOf(systemName.toLowerCase());
                switch (sensorSystem) {
                    case desigo:
                        identifiers = findIdentifiers(DesigoSensorId.DESIGO_ID, record);
                        break;
                    case metasys:
                        identifiers = findIdentifiers(MetasysSensorId.METASYS_OBJECT_ID, record);
                        break;
                    case ecostructure:
                        identifiers = findIdentifiers(EcoStrxureSensorId.OBJECT_ID, record);
                        break;
                    case simulator:
                        identifiers = findIdentifiers("simulatorId", record);
                        break;
                    default:
                        log.warn("Unknown system: {}", sensorSystem);
                        return;
                }

                SensorId sensorId = new SensorId(sensorIdValue, sensorSystem, identifiers);
                sensorIds.add(sensorId);
                log.info("Added sensorId: {}", sensorId);
            });
        } catch (Exception e) {
            log.warn("Failed to read file: {}. Reason: {}", filePath, e.getMessage(), e);
        }
        return sensorIds;
    }

    protected static Map<String, String> findIdentifiers(String singleIdKey, Map<String, String> record) {
        Map<String, String> identifiers = new HashMap<>();
        String identifiersString = record.get(singleIdKey);
        if (isEmpty(identifiersString)) {
            identifiersString = record.get("Identificator");
        }
        if (hasValue(identifiersString)) {
            // Split identifiersString basert på separatortegnet
            String[] identifierPairs = identifiersString.split(IDENTIFIERS_SEAPARATOR);
            for (String identifierPair : identifierPairs) {
                // Split hvert par på ":" for å få nøkkel og verdi
                String[] keyValuePair = identifierPair.split(KEY_VALUE_SEPARATOR);
                if (keyValuePair.length == 2) {
                    identifiers.put(keyValuePair[0], keyValuePair[1]);
                } else {
                    identifiers.put(singleIdKey, identifierPair);
                }
            }
        }
        return identifiers;
    }
}
