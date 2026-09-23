package com.felipemelozx.kairos.controller;

import com.felipemelozx.kairos.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.dto.request.CreateTimeBlockRequest;
import com.felipemelozx.kairos.dto.request.LoginRequest;
import com.felipemelozx.kairos.dto.request.RegisterRequest;
import com.felipemelozx.kairos.dto.request.UpdateTimeBlockRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.entity.TimeBlock;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TimeBlockControllerIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";

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
    private com.felipemelozx.kairos.repository.TimeBlockRepository timeBlockRepository;

    @Test
    void shouldCreateBlockWhenAuthenticated() {
        LoginSession session = login(uniqueEmail());

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/time-blocks",
                new HttpEntity<>(new CreateTimeBlockRequest("Deep Work",
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T10:00:00Z"), null, null),
                        mutationHeaders(session)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();

        Map<String, Object> data = data(response);
        assertThat(data.get("title")).isEqualTo("Deep Work");
        assertThat(data.get("isOverride")).isEqualTo(false);
        assertThat(data.get("createdAt")).isNotNull();
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = restTemplate.exchange("/time-blocks", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn400WhenEndBeforeStart() {
        LoginSession session = login(uniqueEmail());

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/time-blocks",
                new HttpEntity<>(new CreateTimeBlockRequest("Bad range",
                        Instant.parse("2026-10-01T10:00:00Z"), Instant.parse("2026-10-01T09:00:00Z"), null, null),
                        mutationHeaders(session)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturn400WhenTitleBlank() {
        LoginSession session = login(uniqueEmail());

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/time-blocks",
                new HttpEntity<>(new CreateTimeBlockRequest("   ",
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T10:00:00Z"), null, null),
                        mutationHeaders(session)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturn404WhenProjectNotOwned() {
        LoginSession userA = login(uniqueEmail());
        String projectId = createProject(userA, "Private Project");
        LoginSession userB = login(uniqueEmail());

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/time-blocks",
                new HttpEntity<>(new CreateTimeBlockRequest("Study",
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T10:00:00Z"),
                        UUID.fromString(projectId), null),
                        mutationHeaders(userB)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldListBlocks() {
        LoginSession session = login(uniqueEmail());
        createBlock(session, "Morning");
        createBlock(session, "Afternoon");

        ResponseEntity<ApiResponse> response = restTemplate.exchange("/time-blocks", HttpMethod.GET,
                new HttpEntity<>(readHeaders(session)), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listData(response)).hasSize(2);
    }

    @Test
    void shouldListBlocksFilteredByRange() {
        LoginSession session = login(uniqueEmail());
        createBlock(session, "In Range");

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/time-blocks?from=2026-10-01T00:00:00Z&to=2026-10-10T00:00:00Z", HttpMethod.GET,
                new HttpEntity<>(readHeaders(session)), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listData(response)).hasSize(1);
    }

    @Test
    void shouldGetSingleBlock() {
        LoginSession session = login(uniqueEmail());
        String id = createBlock(session, "Single Block");

        ResponseEntity<ApiResponse> response = restTemplate.exchange("/time-blocks/" + id, HttpMethod.GET,
                new HttpEntity<>(readHeaders(session)), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(response).get("title")).isEqualTo("Single Block");
    }

    @Test
    void shouldUpdateBlock() {
        LoginSession session = login(uniqueEmail());
        String id = createBlock(session, "Original");

        ResponseEntity<ApiResponse> response = restTemplate.exchange("/time-blocks/" + id, HttpMethod.PATCH,
                new HttpEntity<>(new UpdateTimeBlockRequest("Renamed", null, null, null), mutationHeaders(session)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(response).get("title")).isEqualTo("Renamed");
    }

    @Test
    void shouldDeleteBlock() {
        LoginSession session = login(uniqueEmail());
        String id = createBlock(session, "To Delete");

        ResponseEntity<ApiResponse> response = restTemplate.exchange("/time-blocks/" + id, HttpMethod.DELETE,
                new HttpEntity<>(mutationHeaders(session)), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiResponse> list = restTemplate.exchange("/time-blocks", HttpMethod.GET,
                new HttpEntity<>(readHeaders(session)), ApiResponse.class);
        assertThat(listData(list)).extracting(p -> p.get("id")).doesNotContain(id);

        TimeBlock deleted = timeBlockRepository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldForbidUserBFromAccessingUserABlock() {
        LoginSession userA = login(uniqueEmail());
        String blockId = createBlock(userA, "Private Block");
        LoginSession userB = login(uniqueEmail());

        ResponseEntity<ApiResponse> get = restTemplate.exchange("/time-blocks/" + blockId, HttpMethod.GET,
                new HttpEntity<>(readHeaders(userB)), ApiResponse.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ApiResponse> patch = restTemplate.exchange("/time-blocks/" + blockId, HttpMethod.PATCH,
                new HttpEntity<>(new UpdateTimeBlockRequest("Hacked", null, null, null), mutationHeaders(userB)),
                ApiResponse.class);
        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ApiResponse> delete = restTemplate.exchange("/time-blocks/" + blockId, HttpMethod.DELETE,
                new HttpEntity<>(mutationHeaders(userB)), ApiResponse.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ApiResponse> stillThere = restTemplate.exchange("/time-blocks/" + blockId, HttpMethod.GET,
                new HttpEntity<>(readHeaders(userA)), ApiResponse.class);
        assertThat(stillThere.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(stillThere).get("title")).isEqualTo("Private Block");
    }

    private String createBlock(LoginSession session, String title) {
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/time-blocks",
                new HttpEntity<>(new CreateTimeBlockRequest(title,
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T10:00:00Z"), null, null),
                        mutationHeaders(session)),
                ApiResponse.class);
        return (String) data(response).get("id");
    }

    private String createProject(LoginSession session, String name) {
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/projects",
                new HttpEntity<>(new CreateProjectRequest(name, "desc", "#1A2B3C"), mutationHeaders(session)),
                ApiResponse.class);
        return (String) data(response).get("id");
    }

    private LoginSession login(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "password123", "Test User"), ApiResponse.class);
        ResponseEntity<ApiResponse> login = restTemplate.postForEntity("/auth/login",
                new LoginRequest(email, "password123"), ApiResponse.class);
        return new LoginSession(cookieValue(login, "ACCESS_TOKEN"), cookieValue(login, "CSRF_TOKEN"));
    }

    private HttpHeaders readHeaders(LoginSession session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "ACCESS_TOKEN=" + session.accessToken());
        return headers;
    }

    private HttpHeaders mutationHeaders(LoginSession session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE,
                "ACCESS_TOKEN=" + session.accessToken() + "; CSRF_TOKEN=" + session.csrfToken());
        headers.add("X-CSRF-Token", session.csrfToken());
        headers.add(HttpHeaders.ORIGIN, ALLOWED_ORIGIN);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<ApiResponse> response) {
        return (Map<String, Object>) response.getBody().data();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listData(ResponseEntity<ApiResponse> response) {
        return (List<Map<String, Object>>) response.getBody().data();
    }

    private String cookieValue(ResponseEntity<?> response, String name) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies == null) {
            return "";
        }
        return cookies.stream()
                .filter(cookie -> cookie.startsWith(name + "="))
                .map(cookie -> cookie.substring(name.length() + 1).split(";")[0])
                .findFirst()
                .orElse("");
    }

    private String uniqueEmail() {
        return "tb-" + UUID.randomUUID() + "@example.com";
    }

    private record LoginSession(String accessToken, String csrfToken) {
    }
}
