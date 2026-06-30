package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.application.dto.PartyDto;
import com.geosun.rmpd.application.dto.PartySuggestionDto;
import com.geosun.rmpd.application.dto.PartyUpsertDto;
import com.geosun.rmpd.application.service.PartyService;
import com.geosun.rmpd.application.service.PartySuggestionService;
import com.geosun.rmpd.domain.enums.PartyRole;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parties")
@Tag(name = "Parties", description = "Довідник контрагентів")
public class PartyController {

    private final PartyService partyService;
    private final PartySuggestionService partySuggestionService;

    public PartyController(PartyService partyService, PartySuggestionService partySuggestionService) {
        this.partyService = partyService;
        this.partySuggestionService = partySuggestionService;
    }

    @GetMapping("/suggestions")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','VIEWER')")
    public ResponseEntity<List<PartySuggestionDto>> suggestions(
            @RequestParam String q, @RequestParam(required = false) PartyRole role) {
        return ResponseEntity.ok(partySuggestionService.search(q, role));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','VIEWER')")
    public ResponseEntity<List<PartyDto>> list() {
        return ResponseEntity.ok(partyService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','VIEWER')")
    public ResponseEntity<PartyDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(partyService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<PartyDto> create(@Valid @RequestBody PartyUpsertDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partyService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<PartyDto> update(@PathVariable Long id, @Valid @RequestBody PartyUpsertDto dto) {
        return ResponseEntity.ok(partyService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        partyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
