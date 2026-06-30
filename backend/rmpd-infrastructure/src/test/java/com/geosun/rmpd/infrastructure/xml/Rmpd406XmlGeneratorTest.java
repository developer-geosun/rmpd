package com.geosun.rmpd.infrastructure.xml;

import com.geosun.rmpd.domain.model.Declaration;
import com.geosun.rmpd.domain.model.Vehicle;
import com.geosun.rmpd.infrastructure.gps.GpsPosition;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Rmpd406XmlGeneratorTest {

    @Test
    void generatesXmlWithReferenceAndPosition() {
        Declaration declaration = new Declaration();
        declaration.setReferenceNumber("SENT-123");
        Vehicle vehicle = new Vehicle();
        vehicle.setGpsDeviceId("ZSL-ABC123-X");
        declaration.setVehicle(vehicle);
        GpsPosition position = new GpsPosition("ZSL-ABC123-X", 52.1, 21.0, Instant.parse("2026-06-30T10:00:00Z"), "mock");

        String xml = Rmpd406XmlGenerator.generate(declaration, position);

        assertTrue(xml.contains("<ReferenceNumber>SENT-123</ReferenceNumber>"));
        assertTrue(xml.contains("<GpsDeviceId>ZSL-ABC123-X</GpsDeviceId>"));
        assertTrue(xml.contains("<Latitude>52.1</Latitude>"));
    }
}
