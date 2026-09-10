package com.felipemelozx.kairos.controller;

import com.felipemelozx.kairos.dto.request.LoginRequest;
import com.felipemelozx.kairos.dto.request.RegisterRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.entity.User;
import com.felipemelozx.kairos.entity.enums.AuthProvider;
import com.felipemelozx.kairos.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

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
        assertThat(cookieValue(response, "ACCESS_TOKEN")).isNotBlank();
        assertThat(cookieValue(response, "REFRESH_TOKEN")).isNotBlank();
        assertThat(cookieValue(response, "CSRF_TOKEN")).isNotBlank();
    }

    @Test
    void shouldRejectRegisterWhenEmailExists() {
        createUser("exists@example.com", "password123", "Existing");

        RegisterRequest request = new RegisterRequest("exists@example.com", "password123", "New");
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/auth/register", request, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldSetCsrfCookieOnLogin() {
        ResponseEntity<ApiResponse> response = registerAndLogin("login@example.com", "password123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cookieValue(response, "ACCESS_TOKEN")).isNotBlank();
        assertThat(cookieValue(response, "REFRESH_TOKEN")).isNotBlank();

        String csrfCookie = setCookieHeader(response, "CSRF_TOKEN");
        assertThat(csrfCookie).isNotBlank();
        assertThat(csrfCookie).contains("Secure").contains("SameSite=Strict").contains("Max-Age=900");
        assertThat(csrfCookie).doesNotContain("HttpOnly").doesNotContain("Domain=");
    }

    @Test
    void shouldSetCsrfCookieOnRefresh() {
        ResponseEntity<ApiResponse> login = registerAndLogin("refresh@example.com", "password123");
        String refreshToken = cookieValue(login, "REFRESH_TOKEN");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "REFRESH_TOKEN=" + refreshToken);

        ResponseEntity<ApiResponse> response =
                restTemplate.postForEntity("/auth/refresh", new HttpEntity<Void>(headers), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cookieValue(response, "ACCESS_TOKEN")).isNotBlank();
        assertThat(cookieValue(response, "CSRF_TOKEN")).isNotBlank();
    }

    @Test
    void shouldClearCsrfCookieOnLogout() {
        ResponseEntity<ApiResponse> login = registerAndLogin("logout@example.com", "password123");
        String accessToken = cookieValue(login, "ACCESS_TOKEN");
        String csrfToken = cookieValue(login, "CSRF_TOKEN");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "ACCESS_TOKEN=" + accessToken + "; CSRF_TOKEN=" + csrfToken);
        headers.add("X-CSRF-Token", csrfToken);
        headers.add(HttpHeaders.ORIGIN, "http://localhost:3000");

        ResponseEntity<ApiResponse> response =
                restTemplate.postForEntity("/auth/logout", new HttpEntity<Void>(headers), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(setCookieHeader(response, "ACCESS_TOKEN")).contains("Max-Age=0");
        assertThat(setCookieHeader(response, "REFRESH_TOKEN")).contains("Max-Age=0");
        assertThat(setCookieHeader(response, "CSRF_TOKEN")).contains("Max-Age=0");
    }

    @Test
    void shouldReturnCurrentUserWhenAuthenticated() {
        ResponseEntity<ApiResponse> login = registerAndLogin("me@example.com", "password123");
        String accessToken = cookieValue(login, "ACCESS_TOKEN");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "ACCESS_TOKEN=" + accessToken);

        ResponseEntity<ApiResponse> response =
                restTemplate.exchange("/auth/me", HttpMethod.GET, new HttpEntity<Void>(headers), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
    }

    @Test
    void shouldReturn403WhenOriginInvalid() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ORIGIN, "https://evil.example.com");
        HttpEntity<LoginRequest> request =
                new HttpEntity<>(new LoginRequest("victim@example.com", "password123"), headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/auth/login", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturn403WhenCsrfHeaderMissing() {
        ResponseEntity<String> response =
                restTemplate.postForEntity("/auth/logout", HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<ApiResponse> registerAndLogin(String email, String password) {
        RegisterRequest register = new RegisterRequest(email, password, "Test User");
        restTemplate.postForEntity("/auth/register", register, ApiResponse.class);
        return restTemplate.postForEntity("/auth/login", new LoginRequest(email, password), ApiResponse.class);
    }

    private String cookieValue(ResponseEntity<?> response, String name) {
        return setCookieHeaders(response).stream()
                .filter(cookie -> cookie.startsWith(name + "="))
                .map(cookie -> cookie.substring(name.length() + 1).split(";")[0])
                .findFirst()
                .orElse("");
    }

    private String setCookieHeader(ResponseEntity<?> response, String name) {
        return setCookieHeaders(response).stream()
                .filter(cookie -> cookie.startsWith(name + "="))
                .findFirst()
                .orElse("");
    }

    private List<String> setCookieHeaders(ResponseEntity<?> response) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        return cookies != null ? cookies : List.of();
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
