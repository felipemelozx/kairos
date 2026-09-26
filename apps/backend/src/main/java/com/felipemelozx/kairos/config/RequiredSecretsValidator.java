package com.felipemelozx.kairos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * FIX-001: fail fast in prod when crypto secrets are missing or placeholders.
 * Local/dev profiles keep backwards-compatible defaults from application.yml.
 */
@Component
public class RequiredSecretsValidator implements ApplicationRunner {

    private static final List<String> PLACEHOLDERS = List.of(
            "default-secret-change-in-production-min-256-bits-long-for-testing",
            "default-csrf-secret-change-in-production-min-256-bits",
            "client-id",
            "client-secret",
            "change-me",
            "changeme",
            "placeholder");

    private final Environment env;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${app.security.csrf.secret:}")
    private String csrfSecret;

    public RequiredSecretsValidator(Environment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProd()) {
            return;
        }
        List<String> problems = new ArrayList<>();
        checkSecret("JWT_SECRET/jwt.secret", jwtSecret, 32, problems);
        checkSecret("CSRF_SECRET/app.security.csrf.secret", csrfSecret, 32, problems);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Missing or unsafe production secrets: " + String.join("; ", problems));
        }
    }

    private boolean isProd() {
        return Arrays.asList(env.getActiveProfiles()).contains("prod");
    }

    private void checkSecret(String name, String value, int minLen, List<String> problems) {
        if (value == null || value.isBlank()) {
            problems.add(name + " is missing");
            return;
        }
        String v = value.trim();
        if (v.length() < minLen) {
            problems.add(name + " must be >= " + minLen + " chars");
            return;
        }
        String lower = v.toLowerCase();
        for (String p : PLACEHOLDERS) {
            if (lower.contains(p)) {
                problems.add(name + " uses a placeholder value");
                return;
            }
        }
    }
}
