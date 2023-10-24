package no.cantara.realestate.cloudconnector.sensors.simulated;

import no.cantara.realestate.sensors.SensorId;

public class SimulatedTempSensor extends SensorId {


    public SimulatedTempSensor(String id) {
        super(id);
    }

    @Override
    public SimulatedUniqueKey getMappingKey() {
        return new SimulatedUniqueKey( "SimulatedTempSensor-"+getId());
    }
}
