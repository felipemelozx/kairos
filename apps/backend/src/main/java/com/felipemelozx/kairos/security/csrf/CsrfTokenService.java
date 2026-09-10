package com.felipemelozx.kairos.security.csrf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CsrfTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int RANDOM_BYTES = 32;
    private static final long DEFAULT_EXPIRY_MS = 900_000L;

    private final byte[] secret;
    private final long expiryMs;
    private final SecureRandom secureRandom;

    public CsrfTokenService(
            @Value("${app.security.csrf.secret}") String secret,
            @Value("${app.security.csrf.expiry-ms:900000}") long expiryMs) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expiryMs = expiryMs;
        this.secureRandom = new SecureRandom();
    }

    public String generateToken() {
        byte[] randomBytes = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(randomBytes);
        String random = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        long expEpoch = System.currentTimeMillis() + expiryMs;
        String payload = random + "." + expEpoch;
        String signature = computeHmac(payload);
        return payload + "." + signature;
    }

    public boolean validateToken(String token) {
        if (token == null) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        String payload = parts[0] + "." + parts[1];
        String providedSignature = parts[2];
        String expectedSignature = computeHmac(payload);
        if (!MessageDigest.isEqual(
                providedSignature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        try {
            long expEpoch = Long.parseLong(parts[1]);
            return System.currentTimeMillis() < expEpoch;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }
}
