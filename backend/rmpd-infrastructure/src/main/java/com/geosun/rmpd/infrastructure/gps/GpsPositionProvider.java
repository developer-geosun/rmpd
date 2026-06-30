package com.geosun.rmpd.infrastructure.gps;

import java.util.Optional;

/**
 * Порт отримання останньої GPS-позиції локатора (SENT-GEO / e-TOLL / ZSL).
 */
public interface GpsPositionProvider {

    Optional<GpsPosition> fetchLastPosition(String gpsDeviceId);
}
