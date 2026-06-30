package com.geosun.rmpd.infrastructure.gps;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Заглушка інтеграції e-TOLL; реальний HTTP-клієнт — у backlog провайдерів.
 */
@Component
@ConditionalOnProperty(name = "rmpd.gps.provider", havingValue = "etoll")
public class EtollGpsPositionProvider implements GpsPositionProvider {

    private static final Logger log = LoggerFactory.getLogger(EtollGpsPositionProvider.class);

    private final String apiUrl;

    public EtollGpsPositionProvider(@Value("${rmpd.gps.etoll-api-url:}") String apiUrl) {
        this.apiUrl = apiUrl;
    }

    @Override
    public Optional<GpsPosition> fetchLastPosition(String gpsDeviceId) {
        if (apiUrl == null || apiUrl.isBlank()) {
            log.warn("e-TOLL GPS: rmpd.gps.etoll-api-url не налаштовано");
            return Optional.empty();
        }
        log.info("e-TOLL GPS stub для device={}", gpsDeviceId);
        return Optional.empty();
    }
}
