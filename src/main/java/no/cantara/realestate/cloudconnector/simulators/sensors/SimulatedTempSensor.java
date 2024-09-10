package no.cantara.realestate.cloudconnector.simulators.sensors;

import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorSystem;

import java.util.Map;

public class SimulatedTempSensor extends SensorId {


    public SimulatedTempSensor(String id) {
        super(id, SensorSystem.simulator, Map.of("SimulatedTempSensor", id));
    }

    @Override
    public SimulatedUniqueKey getMappingKey() {
        return new SimulatedUniqueKey( "SimulatedTempSensor-"+getId());
    }
}
