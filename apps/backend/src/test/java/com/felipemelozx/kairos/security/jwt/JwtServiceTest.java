package com.felipemelozx.kairos.security.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "test-secret-key-that-is-at-least-256-bits-long-for-hs256");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L);
    }

    @Test
    void shouldGenerateAccessTokenWithUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId);

        Claims claims = jwtService.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
    }

    @Test
    void shouldValidateToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId);

        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void shouldReturnFalseForInvalidToken() {
        assertThat(jwtService.validateToken("invalid-token")).isFalse();
    }

    @Test
    void shouldExtractUserIdFromToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId);

        String extractedUserId = jwtService.getUserIdFromToken(token);
        assertThat(extractedUserId).isEqualTo(userId.toString());
    }

    @Test
    void shouldGenerateRefreshToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateRefreshToken(userId);

        assertThat(token).isNotNull();
        assertThat(jwtService.validateToken(token)).isTrue();
    }
}
