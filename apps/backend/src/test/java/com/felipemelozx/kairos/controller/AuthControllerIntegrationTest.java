package com.felipemelozx.kairos.controller;

import com.felipemelozx.kairos.dto.request.RegisterRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.entity.User;
import com.felipemelozx.kairos.entity.enums.AuthProvider;
import com.felipemelozx.kairos.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.8-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldRegisterUserWhenValidRequest() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "John Doe");

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/auth/register", request, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(userRepository.existsByEmail("new@example.com")).isTrue();
    }

    @Test
    void shouldRejectRegisterWhenEmailExists() {
        createUser("exists@example.com", "password123", "Existing");

        RegisterRequest request = new RegisterRequest("exists@example.com", "password123", "New");
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/auth/register", request, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private void createUser(String email, String password, String name) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(password);
        user.setName(name);
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        userRepository.save(user);
    }
}
