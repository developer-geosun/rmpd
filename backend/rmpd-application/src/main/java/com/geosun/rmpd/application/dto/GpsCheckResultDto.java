package com.geosun.rmpd.application.dto;

import java.time.Instant;

public record GpsCheckResultDto(
        boolean valid,
        String gpsDeviceId,
        Double latitude,
        Double longitude,
        Instant recordedAt,
        String source,
        boolean positionStale,
        String message) {}
