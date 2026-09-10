package com.felipemelozx.kairos.security.csrf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsrfTokenServiceTest {

    private CsrfTokenService csrfTokenService;

    @BeforeEach
    void setUp() {
        csrfTokenService = new CsrfTokenService(
                "test-csrf-secret-that-is-at-least-256-bits-long-for-hmac-sha256",
                900_000L
        );
    }

    @Test
    void shouldGenerateValidSignedToken() {
        String token = csrfTokenService.generateToken();

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void shouldValidateValidToken() {
        String token = csrfTokenService.generateToken();

        assertThat(csrfTokenService.validateToken(token)).isTrue();
    }

    @Test
    void shouldRejectNullToken() {
        assertThat(csrfTokenService.validateToken(null)).isFalse();
    }

    @Test
    void shouldRejectTokenWithWrongFormat() {
        assertThat(csrfTokenService.validateToken("invalid")).isFalse();
        assertThat(csrfTokenService.validateToken("a.b")).isFalse();
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = csrfTokenService.generateToken();
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(csrfTokenService.validateToken(tampered)).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        CsrfTokenService expiredService = new CsrfTokenService(
                "test-csrf-secret-that-is-at-least-256-bits-long-for-hmac-sha256",
                -1000L
        );
        String token = expiredService.generateToken();

        assertThat(expiredService.validateToken(token)).isFalse();
    }

    @Test
    void shouldRejectTokenWithWrongSecret() {
        CsrfTokenService service1 = new CsrfTokenService(
                "secret-one-that-is-at-least-256-bits-long-for-hmac-sha256",
                900_000L
        );
        CsrfTokenService service2 = new CsrfTokenService(
                "secret-two-that-is-at-least-256-bits-long-for-hmac-sha256",
                900_000L
        );

        String token = service1.generateToken();

        assertThat(service2.validateToken(token)).isFalse();
    }

    @Test
    void shouldGenerateUniqueTokens() {
        String token1 = csrfTokenService.generateToken();
        String token2 = csrfTokenService.generateToken();

        assertThat(token1).isNotEqualTo(token2);
    }
}
