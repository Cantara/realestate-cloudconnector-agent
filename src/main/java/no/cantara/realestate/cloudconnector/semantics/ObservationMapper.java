package no.cantara.realestate.cloudconnector.semantics;

import no.cantara.realestate.observations.ObservationMessage;
import no.cantara.realestate.observations.ObservationMessageBuilder;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.rec.RecTags;
import no.cantara.realestate.rec.SensorRecObject;
import no.cantara.realestate.sensors.MappedSensorId;
import no.cantara.realestate.sensors.MeasurementUnit;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import static no.cantara.realestate.utils.StringUtils.hasValue;

public class ObservationMapper {

    @Deprecated
    public static ObservationMessage buildRecObservation(MappedSensorId mappedSensorId, ObservedValue trendSample) {
        ObservationMessageBuilder builder = new ObservationMessageBuilder();
        SensorRecObject rec = mappedSensorId.getRec();
        SensorId sensorId = mappedSensorId.getSensorId();
        if (hasValue(sensorId.getId())) {
            builder.withSensorId(sensorId.getId());
        } else {
            builder.withSensorId(rec.getRecId());
        }
        if (rec.getTfm() != null) {
            builder.withTfm(rec.getTfm().getTfm());
        }
        builder.withRealEstate(rec.getRealEstate());
        builder.withBuilding(rec.getBuilding());
        builder.withFloor(rec.getFloor());
        builder.withSection(rec.getSection());
        builder.withServesRoom(rec.getServesRoom());
        builder.withPlacementRoom(rec.getPlacementRoom());
        builder.withSensorType(rec.getSensorType());
        builder.withClimateZone(rec.getClimateZone());
        builder.withElectricityZone(rec.getElectricityZone());
        if (rec.getSensorType() != null) {
            SensorType sensorType = SensorType.from(rec.getSensorType());
            MeasurementUnit measurementUnit = MeasurementUnit.mapFromSensorType(sensorType);
            builder.withMeasurementUnit(measurementUnit.name());
        }

        Number value = null;
        Instant observedAt = null;
        if (trendSample != null) {
            value = trendSample.getValue();
            if (value instanceof BigDecimal) {
                value = ((BigDecimal) value).setScale(2, RoundingMode.CEILING);
            }
            observedAt = trendSample.getObservedAt();
        }
        builder.withObservationTime(observedAt);
        Instant receivedAt = Instant.now();
        builder.withValue(value);
        builder.withReceivedAt(receivedAt);
        ObservationMessage observationMessage = builder.build();
        return observationMessage;
    }

    public static ObservationMessage buildTwinIdObservation(ObservedValue observedValue) {
        if (observedValue == null) {
            return null;
        }
        ObservationMessageBuilder builder = new ObservationMessageBuilder();
        SensorId sensorId = observedValue.getSensorId();
        if (hasValue(sensorId.getId())) {
            builder.withSensorId(sensorId.getId());
        }

        Number value = null;
        Instant observedAt = null;
        value = observedValue.getValue();
        if (value instanceof BigDecimal) {
            value = ((BigDecimal) value).setScale(2, RoundingMode.CEILING);
        }
        observedAt = observedValue.getObservedAt();

        builder.withObservationTime(observedAt);
        Instant receivedAt = Instant.now();
        builder.withValue(value);
        builder.withReceivedAt(receivedAt);
        ObservationMessage observationMessage = builder.build();
        return observationMessage;
    }

    public static ObservationMessage buildRecTagsObservation( ObservedValue observedValue, RecTags rec) {
        if (observedValue == null) {
            return null;
        }
        ObservationMessageBuilder builder = new ObservationMessageBuilder();
        SensorId sensorId = observedValue.getSensorId();
        if (hasValue(sensorId.getId())) {
            builder.withSensorId(sensorId.getId());
        } else {
            builder.withSensorId(rec.getTwinId());
        }
        builder.withTfm(rec.getTfm());
        builder.withRealEstate(rec.getRealEstate());
        builder.withBuilding(rec.getBuilding());
        builder.withFloor(rec.getFloor());
        builder.withSection(rec.getSection());
        builder.withServesRoom(rec.getServesRoom());
        builder.withPlacementRoom(rec.getPlacementRoom());
        builder.withSensorType(rec.getSensorType());
        builder.withClimateZone(rec.getClimateZone());
        builder.withElectricityZone(rec.getElectricityZone());
        if (rec.getSensorType() != null) {
            SensorType sensorType = SensorType.from(rec.getSensorType());
            MeasurementUnit measurementUnit = MeasurementUnit.mapFromSensorType(sensorType);
            builder.withMeasurementUnit(measurementUnit.name());
        }

        Number value = null;
        Instant observedAt = null;
        value = observedValue.getValue();
        if (value instanceof BigDecimal) {
            value = ((BigDecimal) value).setScale(2, RoundingMode.CEILING);
        }
        observedAt = observedValue.getObservedAt();
        builder.withObservationTime(observedAt);
        Instant receivedAt = Instant.now();
        builder.withValue(value);
        builder.withReceivedAt(receivedAt);
        ObservationMessage observationMessage = builder.build();
        return observationMessage;
    }
}
