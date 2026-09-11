# Kairos Backend Architecture

## 1. Executive Summary

**Kairos** is a time-centered productivity system built with a **Layered (N-tier) Architecture**. The backend provides a RESTful API for managing Projects, TimeBlocks, ChecklistItems, and WorkSessions for a **single user** (owner-scoped).

**Key Architectural Decisions:**

| Decision | Rationale |
|----------|-----------|
| **Layered Architecture (controller/service/repository/entity)** | Pragmatic, widely understood structure; JPA entities used directly by services; less boilerplate |
| **Single-user / Owner-scoped** | Personal productivity tool; no multi-tenancy, no organizations, no roles |
| **Spring Boot 4.0.1 + Java 21** | Modern, enterprise-grade, excellent ecosystem |
| **PostgreSQL + Flyway** | ACID compliance, complex queries, reliable migrations |
| **httpOnly Cookie Auth** | Secure token storage; CSRF protection; no client-side token handling |
| **OAuth2 (Google) + Email/Password** | Flexible login; auto-create user on first Google login |
| **Header-based API Versioning** | Clean URLs; version via `X-API-Version` header |
| **OpenAPI / Swagger** | Machine-readable API contracts; auto-generated docs |

---

## 2. Architectural Style

### 2.1 Layered Architecture

Kairos uses a classic layered architecture. Each layer has a single responsibility and depends only on the layer directly below it.

```
┌───────────────────────────────────────────────────────────────┐
│  CONTROLLER LAYER                                             │
│  (HTTP endpoints, request/response handling, DTOs, auth)      │
└───────────────────────────┬───────────────────────────────────┘
                            │  calls
┌───────────────────────────▼───────────────────────────────────┐
│  SERVICE LAYER                                                │
│  (Business logic, business rules, transactions, validation)   │
└───────────────────────────┬───────────────────────────────────┘
                            │  uses
┌───────────────────────────▼───────────────────────────────────┐
│  REPOSITORY LAYER                                             │
│  (Spring Data JPA interfaces, data access)                    │
└───────────────────────────┬───────────────────────────────────┘
                            │  maps
┌───────────────────────────▼───────────────────────────────────┐
│  ENTITY LAYER                                                 │
│  (JPA entities, DB mapping, enums)                            │
└───────────────────────────────────────────────────────────────┘
```

### 2.2 Dependency Rule

Dependencies flow **downward only**. Upper layers may depend on lower layers; lower layers never depend on upper layers.

```
Controller → Service → Repository → Entity
```

**Key Principles:**

- **Controller** never contains business logic — it only translates HTTP into service calls and DTOs.
- **Service** holds all business rules and is `@Transactional`. It is the only caller of repositories.
- **Repository** is a Spring Data JPA interface — no hand-written SQL unless needed.
- **Entity** is a JPA mapping of a database table — no business logic, only state and simple helpers.
- **DTOs** live at the edges (requests/responses) and are never persisted.

### 2.3 Request Flow

```
HTTP Request (httpOnly Cookie with JWT)
     │
     ▼
JwtCookieFilter ──extracts userId──▶ Controller ──validates DTO──▶ Service ──▶ Repository ──▶ Database
                                                                  ▲                    │
                                                                  │                    └── returns JPA Entity
                                                                  └── response DTO ◀───┘
```

---

## 3. Package Structure

### 3.1 Package Layout

```
com.felipemelozx.kairos
├── controller/                        # REST API controllers
│   ├── AuthController.java
│   ├── ProjectController.java
│   ├── TimeBlockController.java
│   ├── TimeBlockSeriesController.java
│   ├── ChecklistItemController.java
│   ├── WorkSessionController.java
│   └── MetricsController.java
├── service/                           # Business logic + transactions
│   ├── AuthService.java
│   ├── UserService.java
│   ├── ProjectService.java
│   ├── TimeBlockService.java
│   ├── TimeBlockSeriesService.java
│   ├── ChecklistItemService.java
│   ├── WorkSessionService.java
│   └── MetricsService.java
├── repository/                        # Spring Data JPA interfaces
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   ├── TimeBlockRepository.java
│   ├── TimeBlockSeriesRepository.java
│   ├── ChecklistItemRepository.java
│   └── WorkSessionRepository.java
├── entity/                            # JPA entities + enums
│   ├── User.java
│   ├── Project.java
│   ├── TimeBlock.java
│   ├── TimeBlockSeries.java
│   ├── ChecklistItem.java
│   ├── WorkSession.java
│   └── enums/
│       ├── ProjectStatus.java
│       └── RecurrencePattern.java
├── dto/                               # Data Transfer Objects
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── CreateProjectRequest.java
│   │   ├── CreateTimeBlockRequest.java
│   │   └── ...
│   └── response/
│       ├── AuthResponse.java
│       ├── UserResponse.java
│       ├── ProjectResponse.java
│       └── ...
├── security/                          # JWT, OAuth2, cookies
│   ├── jwt/
│   │   ├── JwtService.java
│   │   └── JwtCookieAuthenticationFilter.java
│   ├── oauth2/
│   │   └── OAuth2AuthenticationSuccessHandler.java
│   ├── CookieUtils.java
│   └── UserPrincipal.java
├── config/                            # Bean / framework configuration
│   ├── SecurityConfig.java
│   ├── OpenApiConfig.java
│   ├── JacksonConfig.java
│   └── ...
├── exception/                         # Global exception handling
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   ├── GlobalExceptionHandler.java
│   └── ErrorResponse.java
└── KairosApplication.java             # Main entry point
```

### 3.2 Layer Responsibilities

| Layer | Responsibility | Example Members |
|-------|----------------|-----------------|
| **controller/** | HTTP handling, request validation (Bean Validation), response mapping | `ProjectController`, DTOs |
| **service/** | Business rules, orchestration, transactions, owner-scoping | `ProjectService`, `TimeBlockService` |
| **repository/** | Data access via Spring Data | `ProjectRepository`, `WorkSessionRepository` |
| **entity/** | JPA mapping of tables/columns | `Project`, `TimeBlock`, `WorkSession` |
| **config/** | Security, OpenAPI, serialization setup | `SecurityConfig`, `OpenApiConfig` |
| **exception/** | Consistent error responses | `GlobalExceptionHandler` |

### 3.3 Where Business Rules Live

All rules from the PRD and Data Model are enforced in the **service layer**:

| Business Rule | Enforced In |
|---------------|-------------|
| `endDateTime > startDateTime` for Time Blocks | `TimeBlockService.create(...)` |
| Series materialize occurrences up to 12-month horizon | `TimeBlockSeriesService` |
| Forward edit splits series (history immutable) | `TimeBlockSeriesService.update(...)` |
| Per-occurrence override sets `isOverride = true` | `TimeBlockService.update(...)` |
| Timer auto-stops at block end; creates WorkSession | `WorkSessionService` |
| Work sessions are immutable once created | `WorkSessionService` |
| Owner scoping: all queries filter by `userId` | Each service method |

---

## 4. Code Examples

### 4.1 Entity (JPA)

```java
package com.felipemelozx.kairos.entity;

import com.felipemelozx.kairos.entity.enums.ProjectStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_projects_user_active", columnList = "user_id, status")
})
public class Project {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 7)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Project() {
    }

    // Getters and setters...
}
```

### 4.2 Repository (Spring Data JPA)

```java
package com.felipemelozx.kairos.repository;

import com.felipemelozx.kairos.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<Project> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE Project p SET p.deletedAt = :deletedAt " +
           "WHERE p.id = :id AND p.userId = :userId AND p.deletedAt IS NULL")
    void softDeleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId, @Param("deletedAt") Instant deletedAt);
}
```

### 4.3 Service (Business Logic + Transaction)

```java
package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.dto.response.ProjectResponse;
import com.felipemelozx.kairos.entity.Project;
import com.felipemelozx.kairos.entity.enums.ProjectStatus;
import com.felipemelozx.kairos.exception.BusinessException;
import com.felipemelozx.kairos.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private static final int MAX_NAME_LENGTH = 100;
    private static final java.util.Set<String> RESERVED_NAMES =
            java.util.Set.of("all", "inbox", "today");

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectResponse create(UUID currentUserId, CreateProjectRequest request) {
        validateName(request.name());
        validateColor(request.color());

        Project project = new Project();
        project.setUserId(currentUserId);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setColor(request.color());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedAt(Instant.now());

        Project saved = projectRepository.save(project);
        log.info("Project created: id={}, userId={}", saved.getId(), currentUserId);
        return ProjectResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listByUser(UUID currentUserId) {
        return projectRepository.findByUserIdAndDeletedAtIsNull(currentUserId)
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional
    public void softDelete(UUID projectId, UUID currentUserId) {
        projectRepository.softDeleteByIdAndUserId(projectId, currentUserId, Instant.now());
        log.info("Project soft deleted: id={}", projectId);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException("INVALID_PROJECT_NAME",
                    "Project name must be between 1 and " + MAX_NAME_LENGTH + " characters");
        }
        if (RESERVED_NAMES.contains(name.toLowerCase())) {
            throw new BusinessException("RESERVED_PROJECT_NAME",
                    "Project name '" + name + "' is reserved");
        }
    }

    private void validateColor(String color) {
        if (color == null || !color.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new BusinessException("INVALID_COLOR", "Color must be a valid hex color (#RRGGBB)");
        }
    }
}
```

### 4.4 Controller (HTTP Layer Only)

```java
package com.felipemelozx.kairos.controller;

import com.felipemelozx.kairos.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.dto.response.ProjectResponse;
import com.felipemelozx.kairos.service.ProjectService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@SecurityRequirement(name = "cookieAuth")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        UUID userId = UUID.fromString(principal.getUsername());
        ProjectResponse response = projectService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> list(
            @AuthenticationPrincipal UserDetails principal) {

        UUID userId = UUID.fromString(principal.getUsername());
        List<ProjectResponse> projects = projectService.listByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }
}
```

---

## 5. Validation Strategy (Two-Level)

### Level 1: DTO Validation (Bean Validation)

Structural rules live in the request DTO using `jakarta.validation` annotations. Applied automatically by Spring when the controller parameter is annotated with `@Valid`.

```java
package com.felipemelozx.kairos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "description must not exceed 500 characters")
        String description,

        @NotBlank(message = "color is required")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "color must be a valid hex color")
        String color
) {
}
```

### Level 2: Service Validation (Business Rules)

Business and cross-field rules are enforced in the service before any mutation (see `4.3`).

> **Note:** `spring-boot-starter-validation` must be added to `pom.xml` for Bean Validation to work.

---

## 6. Persistence

### 6.1 Conventions

- One JPA entity per table, named after the domain concept (e.g., `TimeBlock` ↔ `time_blocks`).
- Enums stored as strings via `@Enumerated(EnumType.STRING)`.
- Soft delete: entities expose a nullable `deletedAt`; repositories filter with `...AndDeletedAtIsNull`.
- `work_sessions` uses **physical deletion** (no soft delete) — they are immutable records.
- Indexes mirror `docs/diagrams/data-model.md` (including PostgreSQL partial indexes).

### 6.2 Soft-Delete Queries

Spring Data derives filters from method names (`findByUserIdAndDeletedAtIsNull`). For bulk soft delete use `@Modifying @Query`. When partial indexes are required, define them in Flyway migrations and keep the `@Index`/`@Table` annotations aligned.

### 6.3 Transaction Boundaries

`@Transactional` lives on **service public methods**, never on controllers or repositories.

---

## 7. API Design

### 7.1 URL Convention (No Version in URL)

URLs are clean and version-free. API versioning is handled via the `X-API-Version` request header.

| Resource | URL Pattern | Methods |
|----------|-------------|---------|
| Auth (register) | `/api/auth/register` | POST |
| Auth (login) | `/api/auth/login` | POST |
| Auth (logout) | `/api/auth/logout` | POST |
| Auth (refresh) | `/api/auth/refresh` | POST |
| Auth (me) | `/api/auth/me` | GET |
| Projects | `/api/projects` | GET, POST |
| Project | `/api/projects/{projectId}` | GET, PATCH, DELETE |
| Time Blocks | `/api/time-blocks` | GET, POST |
| Time Block | `/api/time-blocks/{blockId}` | GET, PATCH, DELETE |
| Time Block Series | `/api/time-block-series` | GET, POST |
| Time Block Series | `/api/time-block-series/{seriesId}` | GET, PATCH, DELETE |
| Checklist Items | `/api/time-blocks/{blockId}/checklist-items` | GET, POST |
| Checklist Item | `/api/checklist-items/{itemId}` | PATCH, DELETE |
| Work Sessions | `/api/work-sessions` | GET, POST |
| Metrics | `/api/metrics` | GET |

### 7.2 API Versioning (Header-Based)

Version is communicated via the `X-API-Version` header. The server reads this header and routes accordingly. If absent, defaults to the latest version.

```
GET /api/projects
X-API-Version: 1
```

```
GET /api/projects
X-API-Version: 2
```

Implementation: a `VersionFilter` or controller-level `@RequestMapping(headers = "X-API-Version=1")`.

### 7.3 Response Envelope

```java
package com.felipemelozx.kairos.controller;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorResponse error,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(false, null, error, Instant.now());
    }
}
```

### 7.4 Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `BUSINESS_ERROR` | 400 | Business rule violation |
| `NOT_FOUND` | 404 | Resource not found |
| `ACCESS_DENIED` | 403 | User not authorized |
| `UNAUTHORIZED` | 401 | Not authenticated or token expired |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

### 7.5 OpenAPI / Swagger

The API is documented via **springdoc-openapi** (OpenAPI 3.0). The spec is auto-generated from annotations and available at runtime.

**Configuration:**

```java
package com.felipemelozx.kairos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI kairosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kairos API")
                        .description("Time-centered productivity system API")
                        .version("1.0.0"))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                        .addList("cookieAuth"))
                .schemaRequirement("cookieAuth", new SecurityScheme()
                        .type(Type.APIKEY)
                        .in(In.COOKIE)
                        .name("ACCESS_TOKEN"));
    }
}
```

**Endpoints:**

| Path | Description |
|------|-------------|
| `/v3/api-docs` | OpenAPI JSON spec |
| `/swagger-ui.html` | Interactive Swagger UI |

**Dependency (pom.xml):**

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.4</version>
</dependency>
```

---

## 8. Security Architecture

### 8.1 Authentication — httpOnly Cookie + JWT

Tokens are stored in **httpOnly, Secure, SameSite=Strict cookies** — never exposed to JavaScript.

| Cookie | Purpose | TTL | Flags |
|--------|---------|-----|-------|
| `ACCESS_TOKEN` | JWT access token | 15 minutes | httpOnly, Secure, SameSite=Strict, Path=/ |
| `REFRESH_TOKEN` | JWT refresh token | 7 days | httpOnly, Secure, SameSite=Strict, Path=/api/auth/refresh |

**Flow:**

1. User authenticates (Google OAuth or email/password)
2. Backend validates credentials, finds or creates `User`
3. Backend generates JWT access token (15 min) + refresh token (7 days)
4. Tokens set as httpOnly cookies in the response
5. Subsequent requests automatically include cookies
6. `JwtCookieAuthenticationFilter` extracts and validates the access token from the cookie
7. On access token expiry, frontend calls `POST /api/auth/refresh` (refresh cookie auto-sent)
8. On refresh token expiry, user must re-authenticate

### 8.2 Login Methods

#### Google OAuth2 (Auto-Create User)

1. Frontend redirects to `/oauth2/authorization/google`
2. User authenticates with Google
3. Google redirects back to `/login/oauth2/code/google`
4. `OAuth2AuthenticationSuccessHandler` extracts Google profile (email, name, avatar)
5. `AuthService` finds existing user by email **or creates a new one automatically**
6. JWT tokens set as httpOnly cookies
7. Redirect to frontend (e.g., `/calendar`)

#### Email + Password

1. User registers via `POST /api/auth/register` with `{ email, password, name }`
2. Password hashed with BCrypt before storage
3. User logs in via `POST /api/auth/login` with `{ email, password }`
4. `AuthService` validates credentials, generates JWT tokens
5. Tokens set as httpOnly cookies

### 8.3 Authorization (Owner Scoping)

**Model: service-layer discipline, enforced by structure and tests.**

1. **Repository convention (compile-time)**: repositories expose scoped methods only — e.g., `findByIdAndUserId(id, userId)`. Unsafe unscoped methods (`findById`) are **not exposed**. Forgetting the user filter becomes a compile error, not a runtime bug.

2. **Authorization tests (CI)**: every GET/PATCH/DELETE endpoint has a test asserting "user B cannot access user A's resource". Forgetting to write the test fails CI.

3. All requests resolve the user from the JWT cookie; services pass the authenticated `userId` into every repository call.

### 8.4 CSRF Protection

Since cookies are httpOnly and SameSite=Strict, CSRF risk is mitigated. For additional safety:

- `POST /api/auth/logout` invalidates the refresh token server-side (token blacklist or short TTL)
- `SameSite=Strict` prevents cross-site cookie sending

### 8.5 SecurityConfig Example

```java
package com.felipemelozx.kairos.config;

import com.felipemelozx.kairos.security.jwt.JwtCookieAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtCookieAuthenticationFilter jwtCookieFilter;

    public SecurityConfig(JwtCookieAuthenticationFilter jwtCookieFilter) {
        this.jwtCookieFilter = jwtCookieFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oauth2SuccessHandler())
            )
            .addFilterBefore(jwtCookieFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

---

## 9. Cross-Cutting Concerns

### 9.1 Exception Handling

```java
package com.felipemelozx.kairos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.error(new ErrorResponse(ex.getCode(), ex.getMessage(), null)));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(new ErrorResponse("NOT_FOUND", ex.getMessage(), null)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            details.put(field, error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(
                ApiResponse.error(new ErrorResponse("VALIDATION_ERROR",
                        "Request validation failed", details)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(new ErrorResponse("INTERNAL_ERROR",
                        "An unexpected error occurred", null)));
    }
}
```

### 9.2 Logging Strategy

Use SLF4J consistently. Every service method logs an **entry summary** and an **exit summary** at `INFO`, and business decisions at `DEBUG`. Never log tokens, passwords, or secrets.

---

## 10. Migration Strategy (Flyway)

```
src/main/resources/db/migration/
├── V1__Create_users.sql
├── V2__Create_projects.sql
├── V3__Create_time_block_series.sql
├── V4__Create_time_blocks.sql
├── V5__Create_checklist_items.sql
├── V6__Create_work_sessions.sql
└── V7__Create_performance_indexes.sql
```

Rules:

- **Never modify** an applied migration; create a new one.
- Use transactions for data safety.
- Test migrations against a copy of production data.
- Provide rollback scripts for critical migrations.

---

## 11. Testing Strategy

### 11.1 Test Pyramid

```
                    ┌──────┐
                    │ E2E  │  (5% - Critical flows only)
                    ├──────┤
                    │  IT  │  (20% - Integration tests)
                    ├──────┤
                    │ Unit │  (75% - Services, validation)
                    └──────┘
```

### 11.2 Unit Test (Service + Mock Repository)

```java
package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.exception.BusinessException;
import com.felipemelozx.kairos.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void shouldRejectReservedProjectName() {
        UUID userId = UUID.randomUUID();
        CreateProjectRequest request = new CreateProjectRequest("inbox", null, "#3B82F6");

        assertThatThrownBy(() -> projectService.create(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reserved");

        verify(projectRepository, never()).save(any());
    }
}
```

### 11.3 Integration Test (Repository + Testcontainers)

```java
package com.felipemelozx.kairos.repository;

import com.felipemelozx.kairos.entity.Project;
import com.felipemelozx.kairos.entity.enums.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ProjectRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.8-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void shouldSaveAndFindActiveProjectByUser() {
        UUID userId = UUID.randomUUID();
        Project project = new Project();
        project.setUserId(userId);
        project.setName("Study English");
        project.setColor("#3B82F6");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedAt(Instant.now());

        projectRepository.save(project);

        List<Project> result = projectRepository.findByUserIdAndDeletedAtIsNull(userId);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Study English");
    }
}
```

### 11.4 Authorization Tests

Every endpoint must have a test verifying owner scoping:

```java
@Test
void shouldForbidAccessToOtherUsersProject() {
    UUID userA = UUID.randomUUID();
    UUID userB = UUID.randomUUID();
    Project project = createProjectForUser(userA);

    assertThatThrownBy(() -> projectService.getById(project.getId(), userB))
            .isInstanceOf(ResourceNotFoundException.class);
}
```

### 11.5 Coverage

- Target: 80%+ (JaCoCo).
- Changes must never reduce overall coverage.

---

## 12. Deployment Architecture

### 12.1 Docker Compose (Development)

Defined in the repository root `docker-compose.yml`:

| Service | Image | Port |
|---------|-------|------|
| postgres | `postgres:16.8-alpine` | 5432 |
| backend | build from `apps/backend` | 8080 |

### 12.2 Production Infrastructure

Single VPS with Docker Compose:

```
┌─────────────────────────────────────────┐
│              Nginx                       │
│         (SSL Termination)               │
└────────────────────┬────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
┌───────▼────────┐      ┌────────▼────────┐
│   Kairos       │      │   PostgreSQL    │
│   Backend      │      │                 │
└────────────────┘      └─────────────────┘
```

### 12.3 Environment Configuration

| Environment | Profile | Database | Deployment |
|-------------|---------|----------|------------|
| Development | `dev` | Docker Compose | Local |
| Production | `prod` | VPS PostgreSQL | VPS Docker Compose |

---

## 13. Observability

### 13.1 Health Checks

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  health:
    db:
      enabled: true
    readiness-state:
      enabled: true
    liveness-state:
      enabled: true
```

> **Note:** requires `spring-boot-starter-actuator`.

### 13.2 Metrics

- **Logging**: Structured JSON logs (SLF4J + MDC with `requestId`, `userId`).
- **Metrics**: Micrometer counters/timers around service methods.

---

## 14. Development Workflow

### 14.1 Git Workflow

```
main (production)
  ↑ (merge after PR approval)
develop (integration)
  ↑ (merge after review)
feature/xxx (branch from develop)
```

### 14.2 Conventional Commits

```
feat: add project creation endpoint
fix: resolve time block validation error
docs: update backend architecture documentation
refactor: move business rule to TimeBlockService
test: add integration tests for work sessions
chore: upgrade Spring Boot to 4.0.1
```

### 14.3 Code Review Checklist

- [ ] Follows layered architecture (no business logic in controllers/repositories)
- [ ] Business rules live in the service layer
- [ ] All repository methods are owner-scoped (include `userId`)
- [ ] Tests included (unit + integration + authorization)
- [ ] No security vulnerabilities
- [ ] Logging added for service methods
- [ ] OpenAPI annotations added for new endpoints

---

## 15. Architecture Decision Records (ADRs)

### ADR-001: Layered Architecture (Supersedes Clean/Hexagonal)

**Status:** Accepted — **2026**

**Context:** Need maintainable, testable backend for long-term product evolution. The previous Clean/Hexagonal approach added boilerplate without proportional benefit for a single-module Spring Boot service.

**Decision:** Implement a classic **Layered Architecture**: `controller → service → repository → entity`.

**Consequences:**
- ✅ Less boilerplate
- ✅ Simpler to onboard
- ✅ Spring idioms used naturally
- ❌ Entities carry JPA coupling

### ADR-002: Single-User / Owner-Scoped (Supersedes Multi-Tenancy)

**Status:** Accepted — **2026**

**Context:** PRD defines Kairos as a personal productivity tool. No multi-tenancy, no organizations, no team collaboration in MVP.

**Decision:** All data scoped to a single `userId`. No Organization entity. Every repository method includes `userId` for owner scoping.

**Consequences:**
- ✅ Simpler queries (no org joins)
- ✅ Faster development
- ✅ Matches PRD exactly
- ❌ Would require significant rework if multi-tenancy is needed later

### ADR-003: httpOnly Cookie Auth (Supersedes Token-in-Body)

**Status:** Accepted — **2026**

**Context:** JWT tokens need secure storage. localStorage is vulnerable to XSS. httpOnly cookies are the most secure option for web apps.

**Decision:** Access and refresh tokens stored in httpOnly, Secure, SameSite=Strict cookies. Frontend never accesses tokens directly.

**Consequences:**
- ✅ Immune to XSS token theft
- ✅ Automatic cookie sending (no interceptor needed)
- ❌ Requires CSRF consideration (mitigated by SameSite=Strict)
- ❌ Slightly more complex logout (server-side token invalidation)

### ADR-004: Header-Based API Versioning

**Status:** Accepted — **2026**

**Context:** API versioning via URL path (`/v1/...`) pollutes URLs and creates maintenance burden.

**Decision:** Use `X-API-Version` header for versioning. URLs remain clean (`/api/projects`). Default to latest version if header absent.

**Consequences:**
- ✅ Clean URLs
- ✅ Easy to test (header vs path)
- ❌ Less discoverable than URL versioning
- ❌ Requires documentation of versioning strategy

### ADR-005: OpenAPI / Swagger Documentation

**Status:** Accepted — **2026**

**Context:** Need machine-readable API contracts for frontend integration and documentation.

**Decision:** Use `springdoc-openapi` to auto-generate OpenAPI 3.0 spec from annotations. Swagger UI available at `/swagger-ui.html`.

**Consequences:**
- ✅ Auto-generated docs (always in sync with code)
- ✅ Frontend can generate client types from spec
- ✅ Interactive testing via Swagger UI
- ❌ Requires annotations on all endpoints

---

## 16. Future Considerations

### 16.1 Scalability

- **Read Replicas:** Offload read queries to PostgreSQL replicas
- **CQRS:** Separate read/write models for complex queries

### 16.2 Performance

- **WebSocket:** Real-time updates for timer state
- **Async Processing:** Background jobs for recurrence roll-forward

### 16.3 Security

- **Rate Limiting:** Prevent brute-force login attempts
- **Audit Logging:** Track all data mutations
- **Multi-Factor Auth:** Optional 2FA for enhanced security

---

## Appendix A: Quick Reference

### Package Quick Navigation

| What You Need | Where to Find It |
|---------------|------------------|
| Add a REST endpoint | `controller/` |
| Add/modify business logic | `service/` |
| Query the database | `repository/` |
| Map a table | `entity/` |
| Create a request/response object | `dto/request/` or `dto/response/` |
| Add JWT/Cookie logic | `security/` |
| Configure security/OpenAPI | `config/` |
| Add a custom error | `exception/` |

### Common Commands

```bash
# Build
./mvnw clean package

# Run tests
./mvnw test

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Database migration
./mvnw flyway:migrate

# Check test coverage
./mvnw jacoco:report

# View OpenAPI spec
open http://localhost:8080/v3/api-docs

# View Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

**Document Version:** 3.0
**Last Updated:** 2026-09-09
**Status:** Accepted — Layered Architecture, Single-User, httpOnly Cookie Auth, Header Versioning, OpenAPI
**Changes:**
- Aligned with PRD: single-user, no organizations, no multi-tenancy
- Auth: httpOnly cookies (access + refresh), Google OAuth + email/password
- Auto-create user on first Google login
- Header-based API versioning (`X-API-Version`) instead of URL path
- Added OpenAPI/Swagger documentation (springdoc-openapi)
- Removed Redis caching (not needed for MVP single-user)
- Removed Organization/OrganizationMember entities and related code
- Updated all code examples to reflect owner-scoped (userId) pattern
- ADR-002 superseded: Multi-Tenancy → Single-User/Owner-Scoped
- ADR-003 superseded: JWT-in-Body → httpOnly Cookie Auth
- Added ADR-004: Header-Based API Versioning
- Added ADR-005: OpenAPI/Swagger Documentation
