package com.geosun.rmpd.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.DeclarationProgressDto;
import com.geosun.rmpd.application.dto.RoutePointDto;
import com.geosun.rmpd.domain.enums.TransportType;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.Declaration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeclarationCompletionService {

    private final ObjectMapper objectMapper;

    public DeclarationCompletionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DeclarationProgressDto evaluate(Declaration declaration) {
        List<String> missing = new ArrayList<>();
        int total = 0;
        int filled = 0;

        Carrier carrier = declaration.getCarrier();
        total += 4;
        if (isFilled(carrier.getIdType())) {
            filled++;
        } else {
            missing.add("Профіль перевізника: тип ID");
        }
        if (isFilled(carrier.getIdNumber())) {
            filled++;
        } else {
            missing.add("Профіль перевізника: номер ID");
        }
        if (isFilled(carrier.getName())) {
            filled++;
        } else {
            missing.add("Профіль перевізника: назва");
        }
        if (isFilled(carrier.getEmail())) {
            filled++;
        } else {
            missing.add("Профіль перевізника: email");
        }

        total++;
        if (declaration.getVehicle() != null) {
            filled++;
        } else {
            missing.add("Транспортний засіб");
        }

        TransportType type = declaration.getTransportType();
        boolean permitRequired = type == TransportType.LADEN || type == TransportType.CABOTAGE;
        if (permitRequired) {
            total++;
            if (declaration.getPermit() != null) {
                filled++;
            } else {
                missing.add("Дозвіл");
            }
        }

        total += 5;
        if (type != null) {
            filled++;
        } else {
            missing.add("Тип перевезення");
        }
        if (declaration.getRouteStartDate() != null) {
            filled++;
        } else {
            missing.add("Дата початку");
        }
        if (declaration.getRouteEndDate() != null) {
            filled++;
        } else {
            missing.add("Дата завершення");
        }
        if (isFilled(declaration.getLoadingCountry())) {
            filled++;
        } else {
            missing.add("Країна завантаження");
        }
        if (isFilled(declaration.getUnloadingCountry())) {
            filled++;
        } else {
            missing.add("Країна розвантаження");
        }

        total++;
        if (!parseRoutePoints(declaration.getRoutePointsJson()).isEmpty()) {
            filled++;
        } else {
            missing.add("Точки маршруту в PL");
        }

        if (requiresParties(type)) {
            total += 2;
            if (declaration.getSenderParty() != null) {
                filled++;
            } else {
                missing.add("Відправник");
            }
            if (declaration.getReceiverParty() != null) {
                filled++;
            } else {
                missing.add("Отримувач");
            }
        }

        total++;
        if (declaration.isTermsAccepted()) {
            filled++;
        } else {
            missing.add("Підтвердження заяв");
        }

        int percent = total == 0 ? 0 : (int) Math.round(100.0 * filled / total);
        return new DeclarationProgressDto(percent, missing);
    }

    public boolean requiresParties(TransportType type) {
        return type == null || type == TransportType.LADEN || type == TransportType.CABOTAGE;
    }

    private List<RoutePointDto> parseRoutePoints(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<RoutePointDto>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }
}
