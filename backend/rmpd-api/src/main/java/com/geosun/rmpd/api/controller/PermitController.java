package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.application.dto.PermitDto;
import com.geosun.rmpd.application.dto.PermitUpsertDto;
import com.geosun.rmpd.application.service.PermitService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permits")
@Tag(name = "Permits", description = "Довідник дозволів")
public class PermitController {

    private final PermitService permitService;

    public PermitController(PermitService permitService) {
        this.permitService = permitService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','VIEWER')")
    public ResponseEntity<List<PermitDto>> list() {
        return ResponseEntity.ok(permitService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','VIEWER')")
    public ResponseEntity<PermitDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(permitService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<PermitDto> create(@Valid @RequestBody PermitUpsertDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(permitService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<PermitDto> update(@PathVariable Long id, @Valid @RequestBody PermitUpsertDto dto) {
        return ResponseEntity.ok(permitService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
