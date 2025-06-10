package no.cantara.realestate.cloudconnector.semantics;


import no.cantara.realestate.observations.ObservationMessage;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.rec.RecTags;
import no.cantara.realestate.rec.SensorRecObject;
import no.cantara.realestate.sensors.MappedSensorId;
import no.cantara.realestate.sensors.SensorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObservationMapperTest {

    private ObservedValue trendSample;
    private MappedSensorId mappedSensorId;
    private SensorRecObject rec;
    private SensorId mockSensorId;
    private RecTags recTags;

    @BeforeEach
    void setUp() {
        trendSample = mock(ObservedValue.class);
        mappedSensorId = mock(MappedSensorId.class);
        rec = mock(SensorRecObject.class);
        recTags = mock(RecTags.class);
        mockSensorId = mock(SensorId.class);
        when(mappedSensorId.getSensorId()).thenReturn(mockSensorId);
        when(mappedSensorId.getRec()).thenReturn(rec);
    }

    @Test
    void buildRecObservation() {
        String sensorIdValue = "sensorId1";
        when(mockSensorId.getId()).thenReturn(sensorIdValue);
        when(mappedSensorId.getSensorId()).thenReturn(mockSensorId);
        when(mockSensorId.getId()).thenReturn(sensorIdValue);
        assertEquals(sensorIdValue, mappedSensorId.getSensorId().getId());
        ObservationMessage observationMessage = ObservationMapper.buildRecObservation(mappedSensorId, trendSample);
        assertNotNull(observationMessage);
        assertEquals(sensorIdValue,observationMessage.getSensorId());
    }

    @Test
    void buildRecTagsObservation() {
        String sensorIdValue = "sensorId1";
        when(mockSensorId.getId()).thenReturn(sensorIdValue);
        when(trendSample.getSensorId()).thenReturn(mockSensorId);
        when(mockSensorId.getId()).thenReturn(sensorIdValue);
        when(recTags.getTfm()).thenReturn("TFM");
        assertEquals(sensorIdValue, mappedSensorId.getSensorId().getId());
        ObservationMessage observationMessage = ObservationMapper.buildRecTagsObservation(trendSample, recTags);
        assertNotNull(observationMessage);
        assertEquals(sensorIdValue,observationMessage.getSensorId());
        assertEquals("TFM", observationMessage.getTfm());
    }
}