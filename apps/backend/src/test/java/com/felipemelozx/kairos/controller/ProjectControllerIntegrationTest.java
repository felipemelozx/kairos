package com.felipemelozx.kairos.controller;

import com.felipemelozx.kairos.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.dto.request.LoginRequest;
import com.felipemelozx.kairos.dto.request.RegisterRequest;
import com.felipemelozx.kairos.dto.request.UpdateProjectRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.entity.Project;
import com.felipemelozx.kairos.entity.User;
import com.felipemelozx.kairos.entity.enums.ProjectStatus;
import com.felipemelozx.kairos.repository.ProjectRepository;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ProjectControllerIntegrationTest {

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
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateProjectWhenAuthenticated() {
        String email = uniqueEmail();
        LoginSession session = login(email);

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/projects",
                new HttpEntity<>(new CreateProjectRequest("Study English", "Grammar", "#1A2B3C"), mutationHeaders(session)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();

        Map<String, Object> data = data(response);
        assertThat(data.get("name")).isEqualTo("Study English");
        assertThat(data.get("color")).isEqualTo("#1A2B3C");
        assertThat(data.get("status")).isEqualTo("ACTIVE");
        assertThat(data.get("createdAt")).isNotNull();

        User owner = userRepository.findByEmailAndActiveIsTrue(email).orElseThrow();
        assertThat(data.get("userId")).isEqualTo(owner.getId().toString());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = restTemplate.exchange("/projects", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn400WhenNameBlank() {
        LoginSession session = login(uniqueEmail());

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/projects",
                new HttpEntity<>(new CreateProjectRequest("   ", null, "#1A2B3C"), mutationHeaders(session)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturn400WhenColorMissing() {
        LoginSession session = login(uniqueEmail());

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/projects",
                new HttpEntity<>(new CreateProjectRequest("No Color", null, null), mutationHeaders(session)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturn400WhenColorInvalidHex() {
        LoginSession session = login(uniqueEmail());

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/projects",
                new HttpEntity<>(new CreateProjectRequest("Bad Color", null, "red"), mutationHeaders(session)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturn400WhenDescriptionTooLong() {
        LoginSession session = login(uniqueEmail());

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/projects",
                new HttpEntity<>(new CreateProjectRequest("Long", "x".repeat(501), "#1A2B3C"), mutationHeaders(session)),
                ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldListCurrentUserProjectsIncludingArchived() {
        LoginSession session = login(uniqueEmail());
        String activeId = createProject(session, "Active Project");
        String archivedId = createProject(session, "Archived Project");
        patchProject(session, archivedId, new UpdateProjectRequest(null, null, null, ProjectStatus.ARCHIVED));

        ResponseEntity<ApiResponse> response = restTemplate.exchange("/projects", HttpMethod.GET,
                new HttpEntity<>(readHeaders(session)), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> projects = listData(response);
        assertThat(projects).extracting(p -> p.get("id")).contains(activeId, archivedId);
        assertThat(projects).filteredOn(p -> archivedId.equals(p.get("id")))
                .extracting(p -> p.get("status")).containsExactly("ARCHIVED");
    }

    @Test
    void shouldGetSingleProject() {
        LoginSession session = login(uniqueEmail());
        String id = createProject(session, "Single Project");

        ResponseEntity<ApiResponse> response = restTemplate.exchange("/projects/" + id, HttpMethod.GET,
                new HttpEntity<>(readHeaders(session)), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(response).get("name")).isEqualTo("Single Project");
    }

    @Test
    void shouldUpdateProject() {
        LoginSession session = login(uniqueEmail());
        String id = createProject(session, "Original");

        ResponseEntity<ApiResponse> response = patchProject(session, id,
                new UpdateProjectRequest("Renamed", "New description", "#FFFFFF", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = data(response);
        assertThat(data.get("name")).isEqualTo("Renamed");
        assertThat(data.get("description")).isEqualTo("New description");
        assertThat(data.get("color")).isEqualTo("#FFFFFF");
        assertThat(data.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    void shouldArchiveProjectViaPatch() {
        LoginSession session = login(uniqueEmail());
        String id = createProject(session, "To Archive");

        ResponseEntity<ApiResponse> response = patchProject(session, id,
                new UpdateProjectRequest(null, null, null, ProjectStatus.ARCHIVED));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(response).get("status")).isEqualTo("ARCHIVED");
        assertThat(data(response).get("name")).isEqualTo("To Archive");
    }

    @Test
    void shouldDeleteProject() {
        LoginSession session = login(uniqueEmail());
        String id = createProject(session, "To Delete");

        ResponseEntity<ApiResponse> response = restTemplate.exchange("/projects/" + id, HttpMethod.DELETE,
                new HttpEntity<>(mutationHeaders(session)), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiResponse> list = restTemplate.exchange("/projects", HttpMethod.GET,
                new HttpEntity<>(readHeaders(session)), ApiResponse.class);
        assertThat(listData(list)).extracting(p -> p.get("id")).doesNotContain(id);

        Project deleted = projectRepository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldForbidUserBFromAccessingUserAProject() {
        LoginSession userA = login(uniqueEmail());
        String projectId = createProject(userA, "Private Project");
        LoginSession userB = login(uniqueEmail());

        ResponseEntity<ApiResponse> get = restTemplate.exchange("/projects/" + projectId, HttpMethod.GET,
                new HttpEntity<>(readHeaders(userB)), ApiResponse.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ApiResponse> patch = patchProject(userB, projectId,
                new UpdateProjectRequest("Hacked", null, null, null));
        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ApiResponse> delete = restTemplate.exchange("/projects/" + projectId, HttpMethod.DELETE,
                new HttpEntity<>(mutationHeaders(userB)), ApiResponse.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ApiResponse> stillThere = restTemplate.exchange("/projects/" + projectId, HttpMethod.GET,
                new HttpEntity<>(readHeaders(userA)), ApiResponse.class);
        assertThat(stillThere.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(stillThere).get("name")).isEqualTo("Private Project");
    }

    private String createProject(LoginSession session, String name) {
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/projects",
                new HttpEntity<>(new CreateProjectRequest(name, "desc", "#1A2B3C"), mutationHeaders(session)),
                ApiResponse.class);
        return (String) data(response).get("id");
    }

    private ResponseEntity<ApiResponse> patchProject(LoginSession session, String id, UpdateProjectRequest request) {
        return restTemplate.exchange("/projects/" + id, HttpMethod.PATCH,
                new HttpEntity<>(request, mutationHeaders(session)), ApiResponse.class);
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
        return "proj-" + UUID.randomUUID() + "@example.com";
    }

    private record LoginSession(String accessToken, String csrfToken) {
    }
}
