# Kairos Backend Architecture

## 1. Executive Summary

**Kairos** is a time-centered productivity system built with a **Layered (N-tier) Architecture**. The backend provides a RESTful API for managing Projects, Tasks, TimeBlocks, and WorkSessions within a multi-tenant organization model.

**Key Architectural Decisions:**

| Decision | Rationale |
|----------|-----------|
| **Layered Architecture (controller/service/repository/entity)** | Pragmatic, widely understood structure; JPA entities used directly by services; less boilerplate |
| **Multi-Tenant via Organizations** | B2B readiness, data isolation, future team collaboration |
| **Spring Boot 4.0.1 + Java 21** | Modern, enterprise-grade, excellent ecosystem |
| **PostgreSQL + Flyway** | ACID compliance, complex queries, reliable migrations |
| **Redis Caching** | Performance optimization for read-heavy operations |
| **OAuth2 + JWT** | Stateless auth, Google integration, scalable |

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
HTTP Request
     │
     ▼
Controller ──validates DTO (Bean Validation)──▶ Service ──▶ Repository ──▶ Database
     ▲                                                 │
     │                                                 └── returns JPA Entity
     └── response DTO mapped from entity ◀─────────────────┘
```

---

## 3. Package Structure

### 3.1 Package Layout

```
com.felipemelozx.kairos
├── controller/                        # REST API controllers
│   ├── ProjectController.java
│   ├── TaskController.java
│   ├── TimeBlockController.java
│   ├── WorkSessionController.java
│   ├── OrganizationController.java
│   └── AuthController.java
├── service/                           # Business logic + transactions
│   ├── ProjectService.java
│   ├── TaskService.java
│   ├── TimeBlockService.java
│   ├── WorkSessionService.java
│   ├── OrganizationService.java
│   └── AuthService.java
├── repository/                        # Spring Data JPA interfaces
│   ├── UserRepository.java
│   ├── OrganizationRepository.java
│   ├── OrganizationMemberRepository.java
│   ├── ProjectRepository.java
│   ├── TaskRepository.java
│   ├── TimeBlockRepository.java
│   └── WorkSessionRepository.java
├── entity/                            # JPA entities + enums
│   ├── User.java
│   ├── Organization.java
│   ├── OrganizationMember.java
│   ├── Project.java
│   ├── Task.java
│   ├── TimeBlock.java
│   ├── WorkSession.java
│   └── enums/
│       ├── ProjectStatus.java
│       ├── TaskStatus.java
│       ├── OrganizationRole.java
│       └── OrganizationStatus.java
├── dto/                               # Data Transfer Objects
│   ├── request/
│   │   ├── CreateProjectRequest.java
│   │   ├── UpdateTaskStatusRequest.java
│   │   ├── CreateWorkSessionRequest.java
│   │   └── ...
│   └── response/
│       ├── ProjectResponse.java
│       ├── TaskResponse.java
│       ├── AuthResponse.java
│       └── ...
├── security/                          # JWT, OAuth2, current user
│   ├── jwt/
│   │   ├── JwtService.java
│   │   └── JwtAuthenticationFilter.java
│   ├── oauth2/
│   │   └── OAuth2AuthenticationSuccessHandler.java
│   └── UserPrincipal.java
├── config/                            # Bean / framework configuration
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
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
| **service/** | Business rules, orchestration, transactions, org-scope checks | `TaskService`, `ProjectService` |
| **repository/** | Data access via Spring Data | `ProjectRepository`, `WorkSessionRepository` |
| **entity/** | JPA mapping of tables/columns | `Project`, `Task`, `WorkSession` |
| **config/** | Security, caching, serialization setup | `SecurityConfig` |
| **exception/** | Consistent error responses | `GlobalExceptionHandler` |

### 3.3 Where Business Rules Live

All rules from the PRD and Data Model are enforced in the **service layer**:

| Business Rule | Enforced In |
|---------------|-------------|
| Task only becomes `DONE` if it has at least one `WorkSession` | `TaskService.markAsDone(...)` |
| `endDateTime > startDateTime` for Time Blocks | `TimeBlockService.create(...)` |
| `durationMinutes >= 1` for Work Sessions | `WorkSessionService.create(...)` |
| Reserved project names / name length | `ProjectService` |
| Soft-delete cascades (project → tasks) | `ProjectService.delete(...)` |
| Organization membership before any access | Each service (org-scope check) |

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
    @Index(name = "idx_projects_org_status", columnList = "organization_id, status"),
    @Index(name = "idx_projects_org_deleted", columnList = "organization_id, deleted_at")
})
public class Project {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

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
        // JPA
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
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    @Modifying
    @Query("UPDATE Project p SET p.deletedAt = :deletedAt " +
           "WHERE p.id = :id AND p.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);
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
import com.felipemelozx.kairos.repository.OrganizationMemberRepository;
import com.felipemelozx.kairos.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for Projects.
 */
@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private static final int MAX_NAME_LENGTH = 100;
    private static final java.util.Set<String> RESERVED_NAMES =
            java.util.Set.of("all", "inbox", "today");

    private final ProjectRepository projectRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    public ProjectService(ProjectRepository projectRepository,
                          OrganizationMemberRepository organizationMemberRepository) {
        this.projectRepository = projectRepository;
        this.organizationMemberRepository = organizationMemberRepository;
    }

    @Transactional
    public ProjectResponse create(UUID organizationId, UUID currentUserId, CreateProjectRequest request) {
        assertMember(organizationId, currentUserId);
        validateName(request.name());
        validateColor(request.color());

        Project project = new Project();
        project.setOrganizationId(organizationId);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setColor(request.color());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedAt(Instant.now());

        Project saved = projectRepository.save(project);
        log.info("Project created: id={}, organizationId={}", saved.getId(), organizationId);
        return ProjectResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listByOrganization(UUID organizationId, UUID currentUserId) {
        assertMember(organizationId, currentUserId);
        return projectRepository.findByOrganizationIdAndDeletedAtIsNull(organizationId)
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional
    public void softDelete(UUID organizationId, UUID projectId, UUID currentUserId) {
        assertMember(organizationId, currentUserId);
        projectRepository.softDeleteById(projectId, Instant.now());
        // Cascade: soft delete tasks belonging to this project (application level)
        taskService.softDeleteByProjectId(projectId);
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

    private void assertMember(UUID organizationId, UUID currentUserId) {
        if (!organizationMemberRepository.existsByOrganizationIdAndUserId(organizationId, currentUserId)) {
            throw new BusinessException("ACCESS_DENIED", "User is not a member of this organization");
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
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        ProjectResponse response = projectService.create(
                organizationId, jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> list(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal Jwt jwt) {

        List<ProjectResponse> projects = projectService.listByOrganization(
                organizationId, jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(projects));
    }
}
```

### 4.5 Business Rule Example (Task Completion)

```java
package com.felipemelozx.kairos.service;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final WorkSessionRepository workSessionRepository;

    @Transactional
    public TaskResponse markAsDone(UUID organizationId, UUID taskId, UUID currentUserId) {
        assertMember(organizationId, currentUserId);

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // Business rule: DONE requires at least one execution log
        boolean hasExecution = workSessionRepository.existsByTaskIdAndDeletedAtIsNull(taskId);
        if (!hasExecution) {
            throw new BusinessException("TASK_COMPLETION_ERROR",
                    "Task cannot be marked as DONE without an execution log");
        }

        task.setStatus(TaskStatus.DONE);
        Task saved = taskRepository.save(task);
        return TaskResponse.from(saved);
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

Business and cross-field rules are enforced in the service before any mutation (see `4.3` and `4.5`).

> **Note:** `spring-boot-starter-validation` must be added to `pom.xml` for Bean Validation to work.

---

## 6. Persistence

### 6.1 Conventions

- One JPA entity per table, named after the domain concept (e.g., `Task` ↔ `tasks`).
- Enums stored as strings via `@Enumerated(EnumType.STRING)`.
- Soft delete: entities expose a nullable `deletedAt`; repositories filter with `...AndDeletedAtIsNull`.
- `work_sessions` uses **physical deletion** (no soft delete) for performance.
- Indexes mirror `docs/diagrams/data-model.md` (including PostgreSQL partial indexes).

### 6.2 Soft-Delete Queries

Spring Data derives filters from method names (`findByOrganizationIdAndDeletedAtIsNull`). For bulk soft delete use `@Modifying @Query` as shown in `4.2`. When partial indexes are required, define them in Flyway migrations and keep the `@Index`/`@Table` annotations aligned.

### 6.3 Transaction Boundaries

`@Transactional` lives on **service public methods**, never on controllers or repositories.

---

## 7. API Design

### 7.1 RESTful Conventions

| Resource | URL Pattern | Methods |
|----------|-------------|---------|
| Projects | `/api/v1/organizations/{orgId}/projects` | GET, POST |
| Project | `/api/v1/projects/{projectId}` | GET, PATCH, DELETE |
| Tasks | `/api/v1/projects/{projectId}/tasks` | GET, POST |
| Task | `/api/v1/tasks/{taskId}` | GET, PATCH, DELETE |
| Time Blocks | `/api/v1/organizations/{orgId}/time-blocks` | GET, POST |
| Time Block | `/api/v1/time-blocks/{blockId}` | GET, PATCH, DELETE |
| Work Sessions | `/api/v1/organizations/{orgId}/work-sessions` | GET, POST |
| Metrics | `/api/v1/organizations/{orgId}/metrics` | GET |

### 7.2 Response Envelope

```java
package com.felipemelozx.kairos.controller;

import java.time.Instant;

/**
 * Standard API response envelope.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorResponse error,
        Instant timestamp,
        String apiVersion
) {
    private static final String API_VERSION = "1.0";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now(), API_VERSION);
    }

    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(false, null, error, Instant.now(), API_VERSION);
    }
}
```

### 7.3 Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `BUSINESS_ERROR` | 400 | Business rule violation |
| `NOT_FOUND` | 404 | Resource not found |
| `ACCESS_DENIED` | 403 | User not authorized |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## 8. Cross-Cutting Concerns

### 8.1 Exception Handling

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

### 8.2 Logging Strategy

Use SLF4J consistently. Every service method logs an **entry summary** and an **exit summary** at `INFO`, and business decisions at `DEBUG`. Never log tokens, passwords, or secrets.

---

## 9. Caching Strategy (Service Level)

Caching annotations are applied at the **service layer** so cache keys align with business methods.

```java
@Service
public class ProjectService {

    @Cacheable(value = "projects", key = "#id")
    @Transactional(readOnly = true)
    public ProjectResponse getById(UUID organizationId, UUID id, UUID currentUserId) {
        assertMember(organizationId, currentUserId);
        Project project = projectRepository.findById(id)
                .filter(p -> p.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return ProjectResponse.from(project);
    }

    @CacheEvict(value = "projects", key = "#projectId")
    @Transactional
    public void softDelete(UUID organizationId, UUID projectId, UUID currentUserId) {
        // ...
    }
}
```

| Data Type | TTL | Eviction Strategy |
|-----------|-----|-------------------|
| Project by ID | 1 hour | Time-based |
| User profile | 30 minutes | Time-based |
| Organization members | 15 minutes | Write-through |
| Task lists | 5 minutes | Aggressive (frequent updates) |

---

## 10. Security Architecture

### 10.1 Authentication Flow

1. Frontend redirects to `/oauth2/authorization/google`
2. User authenticates with Google
3. Google redirects back to `/login/oauth2/code/google`
4. `AuthService` finds or creates the `User` record
5. Backend generates JWT tokens: **Access 15 min**, **Refresh 7 days**
6. Tokens returned in JSON response

### 10.2 Organization Scope Validation

Every mutating request must include `organizationId`. Services validate membership before touching data:

1. JWT contains a valid user
2. User is a member of the organization (`OrganizationMemberRepository`)
3. User has the required role (OWNER/ADMIN/MEMBER) when applicable

---

## 11. Migration Strategy (Flyway)

```
src/main/resources/db/migration/
├── V1__Create_users.sql
├── V2__Create_organizations.sql
├── V3__Create_organization_members.sql
├── V4__Create_projects.sql
├── V5__Create_tasks.sql
├── V6__Create_time_blocks.sql
├── V7__Create_work_sessions.sql
└── V8__Create_performance_indexes.sql
```

Rules:

- **Never modify** an applied migration; create a new one.
- Use transactions for data safety.
- Test migrations against a copy of production data.
- Provide rollback scripts for critical migrations.

---

## 12. Testing Strategy

### 12.1 Test Pyramid

```
                    ┌──────┐
                    │ E2E  │  (5% - Critical flows only)
                    ├──────┤
                    │  IT  │  (20% - Integration tests)
                    ├──────┤
                    │ Unit │  (75% - Services, validation)
                    └──────┘
```

### 12.2 Unit Test (Service + Mock Repository)

```java
package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.exception.BusinessException;
import com.felipemelozx.kairos.repository.OrganizationMemberRepository;
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
    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void shouldRejectReservedProjectName() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(organizationMemberRepository.existsByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(true);

        CreateProjectRequest request = new CreateProjectRequest("inbox", null, "#3B82F6");

        assertThatThrownBy(() -> projectService.create(orgId, userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reserved");

        verify(projectRepository, never()).save(any());
    }
}
```

### 12.3 Integration Test (Repository + Testcontainers)

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
    void shouldSaveAndFindActiveProjectByOrganization() {
        UUID orgId = UUID.randomUUID();
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setOrganizationId(orgId);
        project.setName("Study English");
        project.setColor("#3B82F6");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedAt(Instant.now());

        projectRepository.save(project);

        List<Project> result = projectRepository.findByOrganizationIdAndDeletedAtIsNull(orgId);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Study English");
    }
}
```

### 12.4 Coverage

- Target: 80%+ (JaCoCo).
- Changes must never reduce overall coverage.

---

## 13. Deployment Architecture

### 13.1 Docker Compose (Development)

Defined in the repository root `docker-compose.yml`:

| Service | Image | Port |
|---------|-------|------|
| postgres | `postgres:16.8-alpine` | 5432 |
| redis | `redis:7-alpine` | 6379 |
| backend | build from `apps/backend` (Dockerfile in `infrastructure/docker/`) | 8080 |

### 13.2 Production Infrastructure

```
┌─────────────────────────────────────────┐
│          Nginx / Cloud LB               │
│         (SSL Termination)               │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│        Kairos Backend (x2 instances)    │
│      (Blue-Green Deployment)            │
└────────────────────┬────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
┌───────▼────────┐      ┌────────▼────────┐
│   PostgreSQL   │      │     Redis       │
│   (Primary)    │      │    (Cache)      │
└────────────────┘      └─────────────────┘
        │
┌───────▼────────┐
│   PostgreSQL   │
│   (Replica)    │
└────────────────┘
```

### 13.3 Environment Configuration

| Environment | Profile | Database | Cache | Deployment |
|-------------|---------|----------|-------|------------|
| Development | `dev` | Docker Compose | Docker Compose | Local |
| Staging | `staging` | Cloud PostgreSQL | Cloud Redis | GitHub Actions |
| Production | `prod` | VPS PostgreSQL | VPS Redis | GitHub Actions |

---

## 14. Observability

### 14.1 Health Checks

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  health:
    db:
      enabled: true
    redis:
      enabled: true
    readiness-state:
      enabled: true
    liveness-state:
      enabled: true
```

> **Note:** requires `spring-boot-starter-actuator`.

### 14.2 Metrics

- **Logging**: Structured JSON logs (SLF4J + MDC with `requestId`, `userId`, `organizationId`).
- **Metrics**: Micrometer counters/timers around service methods (e.g., `project.created`, `task.completion.duration`).
- **Tracing**: OpenTelemetry (future).

---

## 15. Development Workflow

### 15.1 Git Workflow

```
main (production)
  ↑ (merge after PR approval)
develop (integration)
  ↑ (merge after review)
feature/xxx (branch from develop)
```

### 15.2 Conventional Commits

```
feat: add project creation endpoint
fix: resolve task completion validation error
docs: update backend architecture documentation
refactor: move business rule to ProjectService
test: add integration tests for work sessions
chore: upgrade Spring Boot to 4.0.1
```

### 15.3 Code Review Checklist

- [ ] Follows layered architecture (no business logic in controllers/repositories)
- [ ] Business rules live in the service layer
- [ ] Tests included (unit + integration)
- [ ] No security vulnerabilities
- [ ] Logging added for service methods
- [ ] Performance impact assessed
- [ ] Documentation updated

---

## 16. Architecture Decision Records (ADRs)

### ADR-001: Layered Architecture (Supersedes Clean/Hexagonal)

**Status:** Accepted — **2026**

**Context:** Need maintainable, testable backend for long-term product evolution. The previous Clean/Hexagonal approach (framework-agnostic `core/` + `infrastructure/`, gateways, use cases) was considered but added boilerplate (gateways, mappers, duplicated entities) without proportional benefit for a single-module Spring Boot service.

**Decision:** Implement a classic **Layered Architecture**: `controller → service → repository → entity`. Business rules live in services; JPA entities are used directly; DTOs only at the HTTP edges.

**Consequences:**
- ✅ Less boilerplate (no gateway interfaces, no duplicated domain/JPA entities)
- ✅ Simpler to onboard new developers
- ✅ Spring idioms used naturally (`@Service`, `@Transactional`, Spring Data)
- ❌ Entities carry JPA coupling (harder to reuse outside Spring)
- ❌ Business rules can leak into controllers if not disciplined — enforced by review checklist
- 🔁 This ADR **supersedes** the former Clean Architecture ADR

### ADR-002: Multi-Tenancy via Organizations

**Status:** Accepted

**Context:** Product vision includes B2B/future team collaboration.

**Decision:** All data scoped to Organizations; users can belong to multiple orgs.

**Consequences:**
- ✅ Ready for B2B features
- ✅ Data isolation between organizations
- ❌ More complex queries (always filter by organizationId)
- ❌ Larger foreign key overhead

### ADR-003: JWT Stateless Authentication

**Status:** Accepted

**Context:** Need scalable auth without server-side sessions.

**Decision:** OAuth2 + JWT tokens with short-lived access + long-lived refresh tokens.

**Consequences:**
- ✅ Stateless (horizontal scaling)
- ✅ Mobile-friendly
- ❌ Token revocation is complex
- ❌ Requires secure token storage on client

---

## 17. Future Considerations

### 17.1 Scalability

- **Read Replicas:** Offload read queries to PostgreSQL replicas
- **CQRS:** Separate read/write models for complex queries
- **Event Sourcing:** Audit log + event replay capability
- **Microservices:** Extract bounded contexts (e.g., Auth Service)

### 17.2 Performance

- **Database Sharding:** Distribute large datasets by organizationId
- **GraphQL:** Flexible queries for complex frontend needs
- **WebSocket:** Real-time updates for task status changes
- **Async Processing:** Background jobs for email notifications, reports

### 17.3 Security

- **Rate Limiting:** Prevent abuse at organization level
- **Audit Logging:** Track all data mutations
- **PII Encryption:** Encrypt sensitive data at rest
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
| Add JWT/OAuth2 logic | `security/` |
| Configure security/caching | `config/` |
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
```

---

**Document Version:** 2.0
**Last Updated:** 2026-09-08
**Status:** Accepted — Layered Architecture (controller/service/repository/entity)
**Changes:**
- Replaced Clean/Hexagonal (core/infrastructure, gateways, use cases) with Layered Architecture
- JPA entities used directly by services; DTOs only at HTTP edges
- Business rules moved to the service layer
- Removed duplicated domain/JPA entity and gateway/mapper boilerplate
- ADR-001 superseded: Clean/Hexagonal → Layered
