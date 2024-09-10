package no.cantara.realestate.cloudconnector.simulators.sensors;

import no.cantara.realestate.sensors.UniqueKey;

import java.util.Objects;

public class SimulatedUniqueKey implements UniqueKey<String> {
    private final String metasysObjectId;
    public SimulatedUniqueKey(String metasysObjectId) {
        this.metasysObjectId = metasysObjectId;
    }

    public String getKey() {
        return "Metasys__" + metasysObjectId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimulatedUniqueKey that = (SimulatedUniqueKey) o;
        return Objects.equals(metasysObjectId, that.metasysObjectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metasysObjectId);
    }
}
