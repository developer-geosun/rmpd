package com.geosun.rmpd.infrastructure.gps;

import java.time.Instant;

public record GpsPosition(
        String deviceId,
        double latitude,
        double longitude,
        Instant recordedAt,
        String source) {}
