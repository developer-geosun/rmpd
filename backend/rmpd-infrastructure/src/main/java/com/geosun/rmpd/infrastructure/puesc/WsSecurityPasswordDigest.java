package com.geosun.rmpd.infrastructure.puesc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public final class WsSecurityPasswordDigest {

    private static final SecureRandom RANDOM = new SecureRandom();

    private WsSecurityPasswordDigest() {}

    public static DigestResult create(String password) {
        byte[] nonceBytes = new byte[16];
        RANDOM.nextBytes(nonceBytes);
        String nonce = Base64.getEncoder().encodeToString(nonceBytes);
        String created = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        String passwordHash = base64Sha1(password.getBytes(StandardCharsets.UTF_8));
        byte[] digestInput = concat(nonceBytes, created.getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(passwordHash));
        String digest = base64Sha1(digestInput);
        return new DigestResult(nonce, created, digest);
    }

    private static byte[] concat(byte[] nonce, byte[] created, byte[] passwordHash) {
        byte[] result = new byte[nonce.length + created.length + passwordHash.length];
        System.arraycopy(nonce, 0, result, 0, nonce.length);
        System.arraycopy(created, 0, result, nonce.length, created.length);
        System.arraycopy(passwordHash, 0, result, nonce.length + created.length, passwordHash.length);
        return result;
    }

    private static String base64Sha1(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return Base64.getEncoder().encodeToString(digest.digest(input));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public record DigestResult(String nonce, String created, String passwordDigest) {}
}
