package com.geosun.rmpd.infrastructure.xml;

import com.geosun.rmpd.domain.model.Declaration;
import com.geosun.rmpd.domain.model.Vehicle;
import com.geosun.rmpd.infrastructure.gps.GpsPosition;
import java.time.format.DateTimeFormatter;

public final class Rmpd406XmlGenerator {

    private Rmpd406XmlGenerator() {}

    public static String generate(Declaration declaration, GpsPosition position) {
        Vehicle vehicle = declaration.getVehicle();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<RMPD406 xmlns=\"http://puesc.gov.pl/sent/rmpd406\">\n");
        xml.append("  <ReferenceNumber>").append(escape(declaration.getReferenceNumber())).append("</ReferenceNumber>\n");
        if (vehicle != null) {
            xml.append("  <GpsDeviceId>").append(escape(vehicle.getGpsDeviceId())).append("</GpsDeviceId>\n");
        }
        if (position != null) {
            xml.append("  <LastPosition>\n");
            xml.append("    <Latitude>").append(position.latitude()).append("</Latitude>\n");
            xml.append("    <Longitude>").append(position.longitude()).append("</Longitude>\n");
            xml.append("    <RecordedAt>")
                    .append(position.recordedAt().atZone(java.time.ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .append("</RecordedAt>\n");
            xml.append("    <Source>").append(escape(position.source())).append("</Source>\n");
            xml.append("  </LastPosition>\n");
        }
        xml.append("</RMPD406>");
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
