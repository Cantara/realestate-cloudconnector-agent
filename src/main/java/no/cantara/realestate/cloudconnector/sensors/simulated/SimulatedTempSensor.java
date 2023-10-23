package no.cantara.realestate.cloudconnector.sensors.simulated;

import no.cantara.realestate.SensorId;
import no.cantara.realestate.UniqueKey;

public class SimulatedTempSensor extends SensorId {


    public SimulatedTempSensor(String id) {
        super(id);
    }

    @Override
    public UniqueKey<String> getMappingKey() {
        return () -> "SimulatedTempSensor";
    }
}
