package com.geosun.rmpd.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_CARRIER_ID = "cid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(resolveSecretBytes(properties.getSecret()));
    }

    public String generateAccessToken(AuthenticatedUser user) {
        return buildToken(user, TYPE_ACCESS, properties.getAccessTokenTtlMinutes() * 60);
    }

    public String generateRefreshToken(AuthenticatedUser user) {
        return buildToken(user, TYPE_REFRESH, properties.getRefreshTokenTtlDays() * 24 * 60 * 60);
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class));
    }

    public AuthenticatedUser parseUser(String token) {
        Claims claims = parseClaims(token);
        return new AuthenticatedUser(
                claims.get(CLAIM_USER_ID, Long.class),
                claims.get(CLAIM_CARRIER_ID, Long.class),
                claims.getSubject(),
                com.geosun.rmpd.domain.enums.UserRole.valueOf(claims.get(CLAIM_ROLE, String.class)),
                "");
    }

    private String buildToken(AuthenticatedUser user, String type, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claims(Map.of(
                        CLAIM_USER_ID, user.userId(),
                        CLAIM_CARRIER_ID, user.carrierId(),
                        CLAIM_ROLE, user.role().name(),
                        CLAIM_TOKEN_TYPE, type))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static byte[] resolveSecretBytes(String secret) {
        if (secret.length() >= 44 && secret.matches("^[A-Za-z0-9+/=]+$")) {
            return Decoders.BASE64.decode(secret);
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("rmpd.jwt.secret must be at least 32 bytes");
        }
        return bytes;
    }
}
