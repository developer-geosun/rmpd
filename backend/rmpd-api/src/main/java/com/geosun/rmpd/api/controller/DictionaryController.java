package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.api.dto.ApiStubResponse;
import com.geosun.rmpd.api.dto.DictionaryEntryResponse;
import com.geosun.rmpd.api.dto.DictionaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dictionaries")
@Tag(name = "Dictionaries", description = "Словники PUESC")
public class DictionaryController {

    @GetMapping("/{type}")
    @Operation(summary = "Отримати словник за типом (country, id_type, …)")
    public ResponseEntity<DictionaryResponse> getByType(@PathVariable String type) {
        if ("country".equals(type)) {
            return ResponseEntity.ok(new DictionaryResponse(type, List.of(
                    new DictionaryEntryResponse("PL", "Polska", "Poland"),
                    new DictionaryEntryResponse("UA", "Ukraina", "Ukraine"),
                    new DictionaryEntryResponse("DE", "Niemcy", "Germany"))));
        }
        return ResponseEntity.ok(new DictionaryResponse(type, List.of()));
    }
}
