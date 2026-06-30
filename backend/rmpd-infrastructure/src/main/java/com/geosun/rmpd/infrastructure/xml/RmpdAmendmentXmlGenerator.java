package com.geosun.rmpd.infrastructure.xml;

import com.geosun.rmpd.domain.model.Declaration;
import com.geosun.rmpd.domain.model.Vehicle;
import java.time.format.DateTimeFormatter;

public final class RmpdAmendmentXmlGenerator {

    private RmpdAmendmentXmlGenerator() {}

    public static String generate(Declaration declaration, String amendmentReason) {
        Vehicle vehicle = declaration.getVehicle();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<RMPD xmlns=\"http://puesc.gov.pl/sent/rmpd\">\n");
        xml.append("  <ReferenceNumber>").append(escape(declaration.getReferenceNumber())).append("</ReferenceNumber>\n");
        xml.append("  <AmendmentType>UPDATE</AmendmentType>\n");
        if (amendmentReason != null && !amendmentReason.isBlank()) {
            xml.append("  <AmendmentReason>").append(escape(amendmentReason)).append("</AmendmentReason>\n");
        }
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
        if (declaration.getRouteStartDate() != null) {
            xml.append("  <RouteStartDate>")
                    .append(declaration.getRouteStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .append("</RouteStartDate>\n");
        }
        if (declaration.getRouteEndDate() != null) {
            xml.append("  <RouteEndDate>")
                    .append(declaration.getRouteEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .append("</RouteEndDate>\n");
        }
        if (declaration.getComment() != null) {
            xml.append("  <Comment>").append(escape(declaration.getComment())).append("</Comment>\n");
        }
        xml.append("</RMPD>");
        return xml.toString();
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
