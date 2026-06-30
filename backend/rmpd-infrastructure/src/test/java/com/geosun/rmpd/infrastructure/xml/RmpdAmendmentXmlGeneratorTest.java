package com.geosun.rmpd.infrastructure.xml;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.geosun.rmpd.domain.enums.DeclarationStatus;
import com.geosun.rmpd.domain.enums.TransportType;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.Declaration;
import com.geosun.rmpd.domain.model.Vehicle;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RmpdAmendmentXmlGeneratorTest {

    @Test
    void generate_containsReferenceAndAmendmentType() {
        Declaration declaration = new Declaration();
        declaration.setStatus(DeclarationStatus.REGISTERED);
        declaration.setReferenceNumber("SENT-123456");
        declaration.setTransportType(TransportType.LADEN);
        declaration.setRouteStartDate(LocalDate.of(2026, 7, 1));
        declaration.setRouteEndDate(LocalDate.of(2026, 7, 5));

        Carrier carrier = new Carrier();
        carrier.setName("Carrier");
        declaration.setCarrier(carrier);

        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationCountry("UA");
        vehicle.setTractorNumber("AA1111BB");
        vehicle.setGpsDeviceId("ZSL-001");
        declaration.setVehicle(vehicle);

        String xml = RmpdAmendmentXmlGenerator.generate(declaration, "Route date change");
        assertTrue(xml.contains("<RMPD "));
        assertTrue(xml.contains("SENT-123456"));
        assertTrue(xml.contains("UPDATE"));
        assertTrue(xml.contains("Route date change"));
    }
}
