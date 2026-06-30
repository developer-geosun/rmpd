package com.geosun.rmpd.application.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.AddressDto;
import org.springframework.stereotype.Component;

@Component
public class JsonMapper {

    private final ObjectMapper objectMapper;

    public JsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(AddressDto address) {
        if (address == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(address);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Невірний формат адреси", e);
        }
    }

    public AddressDto toAddress(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AddressDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Невірний JSON адреси", e);
        }
    }
}
