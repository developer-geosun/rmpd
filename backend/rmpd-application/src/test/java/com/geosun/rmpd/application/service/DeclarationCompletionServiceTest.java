package com.geosun.rmpd.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.DeclarationProgressDto;
import com.geosun.rmpd.domain.enums.TransportType;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.Declaration;
import com.geosun.rmpd.domain.model.Party;
import com.geosun.rmpd.domain.model.Permit;
import com.geosun.rmpd.domain.model.Vehicle;
import org.junit.jupiter.api.Test;

class DeclarationCompletionServiceTest {

    private final DeclarationCompletionService service = new DeclarationCompletionService(new ObjectMapper());

    @Test
    void emptyTransport_skipsParties() {
        Declaration d = baseDeclaration();
        d.setTransportType(TransportType.EMPTY);
        d.setVehicle(new Vehicle());
        d.setRouteStartDate(java.time.LocalDate.now());
        d.setRouteEndDate(java.time.LocalDate.now().plusDays(1));
        d.setLoadingCountry("UA");
        d.setUnloadingCountry("PL");
        d.setRoutePointsJson("[{\"type\":\"ENTRY\",\"name\":\"Swiecko\",\"country\":\"PL\"}]");
        d.setTermsAccepted(true);

        DeclarationProgressDto progress = service.evaluate(d);
        assertTrue(progress.completionPercent() >= 80);
        assertTrue(progress.missingFields().stream().noneMatch(f -> f.contains("Відправник")));
    }

    @Test
    void laden_requiresPermitAndParties() {
        Declaration d = baseDeclaration();
        d.setTransportType(TransportType.LADEN);
        d.setVehicle(new Vehicle());
        d.setRouteStartDate(java.time.LocalDate.now());
        d.setRouteEndDate(java.time.LocalDate.now().plusDays(1));
        d.setLoadingCountry("UA");
        d.setUnloadingCountry("PL");
        d.setRoutePointsJson("[{\"type\":\"ENTRY\",\"name\":\"Swiecko\",\"country\":\"PL\"}]");

        DeclarationProgressDto progress = service.evaluate(d);
        assertTrue(progress.missingFields().contains("Дозвіл"));
        assertTrue(progress.missingFields().contains("Відправник"));
    }

    @Test
    void fullLaden_isComplete() {
        Declaration d = baseDeclaration();
        d.setTransportType(TransportType.LADEN);
        d.setVehicle(new Vehicle());
        d.setPermit(new Permit());
        d.setSenderParty(new Party());
        d.setReceiverParty(new Party());
        d.setRouteStartDate(java.time.LocalDate.now());
        d.setRouteEndDate(java.time.LocalDate.now().plusDays(1));
        d.setLoadingCountry("UA");
        d.setUnloadingCountry("PL");
        d.setRoutePointsJson("[{\"type\":\"ENTRY\",\"name\":\"Swiecko\",\"country\":\"PL\"}]");
        d.setTermsAccepted(true);

        assertEquals(100, service.evaluate(d).completionPercent());
    }

    private Declaration baseDeclaration() {
        Carrier carrier = new Carrier();
        carrier.setIdType("INNY");
        carrier.setIdNumber("123");
        carrier.setName("Carrier");
        carrier.setEmail("a@b.c");
        Declaration d = new Declaration();
        d.setCarrier(carrier);
        return d;
    }
}
