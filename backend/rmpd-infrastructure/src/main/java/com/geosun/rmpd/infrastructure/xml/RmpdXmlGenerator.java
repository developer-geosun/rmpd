package com.geosun.rmpd.infrastructure.xml;

import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.Declaration;
import com.geosun.rmpd.domain.model.Party;
import com.geosun.rmpd.domain.model.Permit;
import com.geosun.rmpd.domain.model.Vehicle;
import java.time.format.DateTimeFormatter;

public final class RmpdXmlGenerator {

    private RmpdXmlGenerator() {}

    public static String generate(Declaration declaration) {
        Carrier carrier = declaration.getCarrier();
        Vehicle vehicle = declaration.getVehicle();
        Permit permit = declaration.getPermit();
        Party sender = declaration.getSenderParty();
        Party receiver = declaration.getReceiverParty();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<RMPD100 xmlns=\"http://puesc.gov.pl/sent/rmpd\">\n");
        xml.append("  <Carrier>\n");
        xml.append("    <Name>").append(escape(carrier.getName())).append("</Name>\n");
        xml.append("    <IdType>").append(escape(carrier.getIdType())).append("</IdType>\n");
        xml.append("    <IdNumber>").append(escape(carrier.getIdNumber())).append("</IdNumber>\n");
        xml.append("    <Email>").append(escape(carrier.getEmail())).append("</Email>\n");
        xml.append("  </Carrier>\n");

        if (vehicle != null) {
            xml.append("  <Vehicle>\n");
            xml.append("    <RegistrationCountry>").append(escape(vehicle.getRegistrationCountry())).append("</RegistrationCountry>\n");
            xml.append("    <TractorNumber>").append(escape(vehicle.getTractorNumber())).append("</TractorNumber>\n");
            if (vehicle.getTrailerNumber() != null) {
                xml.append("    <TrailerNumber>").append(escape(vehicle.getTrailerNumber())).append("</TrailerNumber>\n");
            }
            xml.append("    <GpsDeviceId>").append(escape(vehicle.getGpsDeviceId())).append("</GpsDeviceId>\n");
            xml.append("  </Vehicle>\n");
        }

        if (permit != null) {
            xml.append("  <Permit>\n");
            xml.append("    <PermitNumber>").append(escape(permit.getPermitNumber())).append("</PermitNumber>\n");
            xml.append("    <PermitType>").append(escape(permit.getPermitType())).append("</PermitType>\n");
            xml.append("  </Permit>\n");
        }

        xml.append("  <Transport>\n");
        if (declaration.getTransportType() != null) {
            xml.append("    <Type>").append(declaration.getTransportType().name()).append("</Type>\n");
        }
        if (declaration.getCmrNumber() != null) {
            xml.append("    <CmrNumber>").append(escape(declaration.getCmrNumber())).append("</CmrNumber>\n");
        }
        if (declaration.getRouteStartDate() != null) {
            xml.append("    <RouteStartDate>")
                    .append(declaration.getRouteStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .append("</RouteStartDate>\n");
        }
        if (declaration.getRouteEndDate() != null) {
            xml.append("    <RouteEndDate>")
                    .append(declaration.getRouteEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .append("</RouteEndDate>\n");
        }
        if (declaration.getLoadingCountry() != null) {
            xml.append("    <LoadingCountry>").append(escape(declaration.getLoadingCountry())).append("</LoadingCountry>\n");
        }
        if (declaration.getUnloadingCountry() != null) {
            xml.append("    <UnloadingCountry>").append(escape(declaration.getUnloadingCountry())).append("</UnloadingCountry>\n");
        }
        xml.append("  </Transport>\n");

        if (sender != null) {
            appendParty(xml, "Sender", sender);
        }
        if (receiver != null) {
            appendParty(xml, "Receiver", receiver);
        }

        if (declaration.getComment() != null) {
            xml.append("  <Comment>").append(escape(declaration.getComment())).append("</Comment>\n");
        }

        xml.append("</RMPD100>");
        return xml.toString();
    }

    private static void appendParty(StringBuilder xml, String tag, Party party) {
        xml.append("  <").append(tag).append(">\n");
        xml.append("    <Name>").append(escape(party.getName())).append("</Name>\n");
        xml.append("    <IdType>").append(escape(party.getIdType())).append("</IdType>\n");
        xml.append("    <IdNumber>").append(escape(party.getIdNumber())).append("</IdNumber>\n");
        xml.append("  </").append(tag).append(">\n");
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
