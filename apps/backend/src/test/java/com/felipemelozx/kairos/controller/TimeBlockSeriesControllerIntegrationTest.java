package com.felipemelozx.kairos.controller;

import com.felipemelozx.kairos.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.dto.request.CreateSeriesRequest;
import com.felipemelozx.kairos.dto.request.LoginRequest;
import com.felipemelozx.kairos.dto.request.RegisterRequest;
import com.felipemelozx.kairos.dto.request.SeriesPatternRequest;
import com.felipemelozx.kairos.dto.request.UpdateSeriesRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TimeBlockSeriesControllerIntegrationTest {

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

    @Test
    void shouldCreateSeriesWhenAuthenticated() {
        LoginSession session = login(uniqueEmail());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/time-block-series",
                new HttpEntity<>(new CreateSeriesRequest("Ingles", null,
                        today.plusDays(1), today.plusDays(3),
                        new SeriesPatternRequest("DAILY", null, null, "09:00", 60)),
                        mutationHeaders(session)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> data = data(response);
        assertThat(data.get("title")).isEqualTo("Ingles");
        assertThat(data.get("id")).isNotNull();

        ResponseEntity<ApiResponse> blocks = restTemplate.exchange("/time-blocks", HttpMethod.GET,
                new HttpEntity<>(readHeaders(session)), ApiResponse.class);
        List<Map<String, Object>> listed = listData(blocks);
        assertThat(listed).hasSize(3);
        assertThat(listed).allMatch(b -> data.get("id").equals(b.get("seriesId")));
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = restTemplate.exchange("/time-block-series", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn400WhenPatternInvalid() {
        LoginSession session = login(uniqueEmail());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/time-block-series",
                new HttpEntity<>(new CreateSeriesRequest("Ingles", null,
                        today.plusDays(1), today.plusDays(3),
                        new SeriesPatternRequest("DAILY", null, null, "25:00", 60)),
                        mutationHeaders(session)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorCode(response)).isEqualTo("INVALID_SERIES_PATTERN");
    }

    @Test
    void shouldReturn400WhenEndBeforeStart() {
        LoginSession session = login(uniqueEmail());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/time-block-series",
                new HttpEntity<>(new CreateSeriesRequest("Ingles", null,
                        today.plusDays(5), today.plusDays(1),
                        new SeriesPatternRequest("DAILY", null, null, "09:00", 60)),
                        mutationHeaders(session)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorCode(response)).isEqualTo("INVALID_SERIES_RANGE");
    }

    @Test
    void shouldReturn404WhenProjectNotOwned() {
        LoginSession userA = login(uniqueEmail());
        String projectId = createProject(userA, "Private Project");
        LoginSession userB = login(uniqueEmail());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/time-block-series",
                new HttpEntity<>(new CreateSeriesRequest("Ingles", UUID.fromString(projectId),
                        today.plusDays(1), today.plusDays(3),
                        new SeriesPatternRequest("DAILY", null, null, "09:00", 60)),
                        mutationHeaders(userB)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldListAndGetSeries() {
        LoginSession session = login(uniqueEmail());
        String id = createSeries(session, "Ingles");

        ResponseEntity<ApiResponse> list = restTemplate.exchange("/time-block-series", HttpMethod.GET,
                new HttpEntity<>(readHeaders(session)), ApiResponse.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listData(list)).hasSize(1);

        ResponseEntity<ApiResponse> get = restTemplate.exchange("/time-block-series/" + id, HttpMethod.GET,
                new HttpEntity<>(readHeaders(session)), ApiResponse.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(get).get("title")).isEqualTo("Ingles");
    }

    @Test
    void shouldSplitSeriesOnUpdatePreservingPast() {
        LoginSession session = login(uniqueEmail());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String id = createSeriesStarting(session, "Ingles", today.minusDays(10), null, "09:00");
        LocalDate effectiveFrom = today.plusDays(5);

        ResponseEntity<ApiResponse> response = restTemplate.exchange("/time-block-series/" + id, HttpMethod.PATCH,
                new HttpEntity<>(new UpdateSeriesRequest(effectiveFrom, null, null, null,
                        new SeriesPatternRequest("DAILY", null, null, "10:00", 30)),
                        mutationHeaders(session)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = data(response);
        assertThat(data.get("startDate").toString()).isEqualTo(effectiveFrom.toString());

        ResponseEntity<ApiResponse> old = restTemplate.exchange("/time-block-series/" + id, HttpMethod.GET,
                new HttpEntity<>(readHeaders(session)), ApiResponse.class);
        assertThat(data(old).get("endDate").toString()).isEqualTo(effectiveFrom.minusDays(1).toString());
    }

    @Test
    void shouldDeleteSeriesPreservingPastOccurrences() {
        LoginSession session = login(uniqueEmail());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String id = createSeriesStarting(session, "Ingles", today.minusDays(10), null, "09:00");

        ResponseEntity<ApiResponse> response = restTemplate.exchange("/time-block-series/" + id, HttpMethod.DELETE,
                new HttpEntity<>(mutationHeaders(session)), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiResponse> get = restTemplate.exchange("/time-block-series/" + id, HttpMethod.GET,
                new HttpEntity<>(readHeaders(session)), ApiResponse.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ApiResponse> blocks = restTemplate.exchange("/time-blocks", HttpMethod.GET,
                new HttpEntity<>(readHeaders(session)), ApiResponse.class);
        assertThat(listData(blocks)).isNotEmpty();
    }

    @Test
    void shouldForbidUserBFromAccessingUserASeries() {
        LoginSession userA = login(uniqueEmail());
        String seriesId = createSeries(userA, "Private Series");
        LoginSession userB = login(uniqueEmail());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        ResponseEntity<ApiResponse> get = restTemplate.exchange("/time-block-series/" + seriesId, HttpMethod.GET,
                new HttpEntity<>(readHeaders(userB)), ApiResponse.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ApiResponse> patch = restTemplate.exchange("/time-block-series/" + seriesId, HttpMethod.PATCH,
                new HttpEntity<>(new UpdateSeriesRequest(today.plusDays(5), null, null, null,
                        new SeriesPatternRequest("DAILY", null, null, "10:00", 30)),
                        mutationHeaders(userB)),
                ApiResponse.class);
        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ApiResponse> delete = restTemplate.exchange("/time-block-series/" + seriesId, HttpMethod.DELETE,
                new HttpEntity<>(mutationHeaders(userB)), ApiResponse.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ApiResponse> stillThere = restTemplate.exchange("/time-block-series/" + seriesId, HttpMethod.GET,
                new HttpEntity<>(readHeaders(userA)), ApiResponse.class);
        assertThat(stillThere.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String createSeries(LoginSession session, String title) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return createSeriesStarting(session, title, today.plusDays(1), today.plusDays(3), "09:00");
    }

    private String createSeriesStarting(LoginSession session, String title, LocalDate start, LocalDate end,
            String time) {
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/time-block-series",
                new HttpEntity<>(new CreateSeriesRequest(title, null, start, end,
                        new SeriesPatternRequest("DAILY", null, null, time, 60)),
                        mutationHeaders(session)),
                ApiResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
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

    private String errorCode(ResponseEntity<ApiResponse> response) {
        return response.getBody().error() == null ? null : response.getBody().error().code();
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
        return "series-" + UUID.randomUUID() + "@example.com";
    }

    private record LoginSession(String accessToken, String csrfToken) {
    }
}
