package no.cantara.realestate.cloudconnector.simulators.sensors;

import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorSystem;

import java.util.Map;

public class SimulatedCo2Sensor extends SensorId {


    public SimulatedCo2Sensor(String id) {
        super(id, SensorSystem.simulator, Map.of("SimulatedCo2Sensor", id));
    }

    @Override
    public SimulatedUniqueKey getMappingKey() {
        return new SimulatedUniqueKey( "SimulatedCo2Sensor-"+getId());
    }


}
