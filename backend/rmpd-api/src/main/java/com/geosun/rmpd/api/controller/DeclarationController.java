package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.api.dto.ApiStubResponse;
import com.geosun.rmpd.api.dto.DeclarationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/declarations")
@Tag(name = "Declarations", description = "Декларації RMPD100")
public class DeclarationController {

    @GetMapping
    @Operation(summary = "Список декларацій")
    public ResponseEntity<List<DeclarationResponse>> list() {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping
    @Operation(summary = "Створити черновик")
    public ResponseEntity<ApiStubResponse> create() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiStubResponse.notImplemented("POST /declarations"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Деталі декларації")
    public ResponseEntity<ApiStubResponse> get(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiStubResponse.notImplemented("GET /declarations/" + id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Оновити черновик")
    public ResponseEntity<ApiStubResponse> update(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiStubResponse.notImplemented("PUT /declarations/" + id));
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "XSD + бізнес-валідація")
    public ResponseEntity<ApiStubResponse> validate(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiStubResponse.notImplemented("POST /declarations/" + id + "/validate"));
    }

    @GetMapping("/{id}/xml")
    @Operation(summary = "Завантажити XML")
    public ResponseEntity<ApiStubResponse> downloadXml(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiStubResponse.notImplemented("GET /declarations/" + id + "/xml"));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Підписати та відправити в PUESC")
    public ResponseEntity<ApiStubResponse> submit(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiStubResponse.notImplemented("POST /declarations/" + id + "/submit"));
    }
}
