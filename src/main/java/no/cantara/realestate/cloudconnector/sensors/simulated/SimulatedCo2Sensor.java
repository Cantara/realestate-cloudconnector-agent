package no.cantara.realestate.cloudconnector.sensors.simulated;

import no.cantara.realestate.sensors.SensorId;

public class SimulatedCo2Sensor extends SensorId {


    public SimulatedCo2Sensor(String id) {
        super(id);
    }

    @Override
    public SimulatedUniqueKey getMappingKey() {
        return new SimulatedUniqueKey( "SimulatedCo2Sensor-"+getId());
    }


}
