package com.geosun.rmpd.application.service;

import com.geosun.rmpd.application.dto.AuthTokensResponse;
import com.geosun.rmpd.application.dto.LoginCommand;
import com.geosun.rmpd.application.dto.RefreshTokenCommand;
import com.geosun.rmpd.application.dto.UserInfoResponse;
import com.geosun.rmpd.application.exception.UnauthorizedException;
import com.geosun.rmpd.infrastructure.security.AuthenticatedUser;
import com.geosun.rmpd.infrastructure.security.JwtProperties;
import com.geosun.rmpd.infrastructure.security.JwtService;
import com.geosun.rmpd.infrastructure.security.SecurityUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    public AuthTokensResponse login(LoginCommand command) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(command.email(), command.password()));
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return toTokens(user);
    }

    public AuthTokensResponse refresh(RefreshTokenCommand command) {
        String token = command.refreshToken();
        if (!jwtService.isRefreshToken(token)) {
            throw new UnauthorizedException("Невірний refresh token");
        }
        try {
            AuthenticatedUser user = jwtService.parseUser(token);
            return toTokens(user);
        } catch (Exception ex) {
            throw new UnauthorizedException("Refresh token прострочений або невалідний");
        }
    }

    public UserInfoResponse currentUser() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        return new UserInfoResponse(user.userId(), user.getUsername(), user.role(), user.carrierId());
    }

    private AuthTokensResponse toTokens(AuthenticatedUser user) {
        return new AuthTokensResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                jwtProperties.getAccessTokenTtlMinutes() * 60,
                new UserInfoResponse(user.userId(), user.getUsername(), user.role(), user.carrierId()));
    }
}
