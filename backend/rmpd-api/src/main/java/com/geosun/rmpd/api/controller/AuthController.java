package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.application.dto.AuthTokensResponse;
import com.geosun.rmpd.application.dto.LoginCommand;
import com.geosun.rmpd.application.dto.RefreshTokenCommand;
import com.geosun.rmpd.application.dto.UserInfoResponse;
import com.geosun.rmpd.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Аутентифікація JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Вхід за email та паролем")
    public ResponseEntity<AuthTokensResponse> login(@Valid @RequestBody LoginCommand command) {
        return ResponseEntity.ok(authService.login(command));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Оновлення access token")
    public ResponseEntity<AuthTokensResponse> refresh(@Valid @RequestBody RefreshTokenCommand command) {
        return ResponseEntity.ok(authService.refresh(command));
    }

    @GetMapping("/me")
    @Operation(summary = "Поточний користувач")
    public ResponseEntity<UserInfoResponse> me() {
        return ResponseEntity.ok(authService.currentUser());
    }
}
