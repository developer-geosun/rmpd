package com.geosun.rmpd.infrastructure.gps;

import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rmpd.gps.provider", havingValue = "mock", matchIfMissing = true)
public class MockGpsPositionProvider implements GpsPositionProvider {

    @Override
    public Optional<GpsPosition> fetchLastPosition(String gpsDeviceId) {
        if (gpsDeviceId == null || gpsDeviceId.isBlank()) {
            return Optional.empty();
        }
        int hash = Math.abs(gpsDeviceId.hashCode());
        double lat = 52.0 + (hash % 1000) / 10000.0;
        double lon = 21.0 + (hash % 2000) / 10000.0;
        return Optional.of(new GpsPosition(gpsDeviceId, lat, lon, Instant.now().minusSeconds(300), "mock"));
    }
}
