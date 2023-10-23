package no.cantara.realestate.cloudconnector.sensors.simulated;

import no.cantara.realestate.SensorId;
import no.cantara.realestate.UniqueKey;

public class SimulatedCo2Sensor extends SensorId {


    public SimulatedCo2Sensor(String id) {
        super(id);
    }

    @Override
    public UniqueKey<String> getMappingKey() {
        return () -> "SimulatedCo2Sensor";
    }
}
