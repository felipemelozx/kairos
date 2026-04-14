# Kairos Backend Architecture

## 1. Executive Summary

**Kairos** is a time-centered productivity system built with **Clean/Hexagonal Architecture** principles. The backend provides a RESTful API for managing Projects, Tasks, TimeBlocks, and WorkSessions within a multi-tenant organization model.

**Key Architectural Decisions:**

| Decision | Rationale |
|----------|-----------|
| **Clean/Hexagonal Architecture** | Framework-agnostic domain layer, testability, maintainability |
| **Multi-Tenant via Organizations** | B2B readiness, data isolation, future team collaboration |
| **Spring Boot 4.0.1 + Java 21** | Modern, enterprise-grade, excellent ecosystem |
| **PostgreSQL + Flyway** | ACID compliance, complex queries, reliable migrations |
| **Redis Caching** | Performance optimization for read-heavy operations |
| **OAuth2 + JWT** | Stateless auth, Google integration, scalable |

---

## 2. Architectural Style

### 2.1 Clean/Hexagonal Architecture (Simplified)

Kairos uses a simplified two-tier Clean Architecture:

```
┌─────────────────────────────────────────────────────────────────┐
│                        CORE LAYER                                │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Domain Entities (Project, Task, TimeBlock, etc.)        │    │
│  │ Use Cases (Business Logic Orchestration)                │    │
│  │ Gateway Interfaces (Ports)                               │    │
│  │ Value Objects (TaskStatus, ProjectColor, etc.)          │    │
│  │ Domain Exceptions & Validators                          │    │
│  │ ❌ FRAMEWORK-AGNOSTIC - No Spring/JPA/Lombok            │    │
│  └─────────────────────────────────────────────────────────┘    │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                          │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Controllers (REST Endpoints)                            │    │
│  │ DTOs (Request/Response)                                 │    │
│  │ Gateway Implementations (JPA, External APIs)            │    │
│  │ JPA Entities (Database Mapping)                         │    │
│  │ Security (JWT, OAuth2)                                  │    │
│  │ Framework Configuration                                 │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Dependency Rule

**Core never depends on Infrastructure.** All dependencies point inward.

```
Infrastructure → Core (Core is independent)
```

**Key Principle:**
- **Core:** Pure Java business logic, testable without Spring
- **Infrastructure:** Framework-specific implementations (Spring, JPA, Security)

### 2.3 Ports & Adapters

Every interaction between core and the outside world goes through **interfaces (ports)**:

| Port (Interface) | Adapter (Implementation) | Direction |
|------------------|-------------------------|-----------|
| `ProjectRepository` | `JpaProjectRepository` | Driving |
| `TaskRepository` | `JpaTaskRepository` | Driving |
| `TimeBlockRepository` | `JpaTimeBlockRepository` | Driving |
| `WorkSessionRepository` | `JpaWorkSessionRepository` | Driving |
| `UserRepository` | `JpaUserRepository` | Driving |
| `OrganizationRepository` | `JpaOrganizationRepository` | Driving |
| `CachePort` | `RedisCacheAdapter` | Driven |
| `EmailPort` | `SmtpEmailAdapter` (future) | Driven |

---

## 3. Package Structure

### 3.1 Two-Tier Organization

Kairos uses a simplified two-tier package structure:

```
com.felipemelozx.kairos
├── core/                                      # FRAMEWORK-AGNOSTIC (Pure Java)
│   ├── domain/                                # Domain entities
│   │   ├── Project.java
│   │   ├── Task.java
│   │   ├── TimeBlock.java
│   │   ├── WorkSession.java
│   │   ├── User.java
│   │   └── Organization.java
│   ├── exception/                             # Custom exceptions
│   │   ├── DomainException.java
│   │   ├── TaskCompletionException.java
│   │   ├── InvalidTimeRangeException.java
│   │   └── OrganizationAccessDeniedException.java
│   ├── gateway/                               # Gateway interfaces (Ports)
│   │   ├── ProjectGateway.java
│   │   ├── TaskGateway.java
│   │   ├── TimeBlockGateway.java
│   │   ├── WorkSessionGateway.java
│   │   ├── UserGateway.java
│   │   ├── OrganizationGateway.java
│   │   └── CacheGateway.java
│   ├── usecase/                               # Use cases (Business logic)
│   │   ├── project/
│   │   │   ├── CreateProjectUseCase.java
│   │   │   ├── UpdateProjectUseCase.java
│   │   │   ├── DeleteProjectUseCase.java
│   │   │   ├── ListProjectsUseCase.java
│   │   │   └── GetProjectByIdUseCase.java
│   │   ├── task/
│   │   │   ├── CreateTaskUseCase.java
│   │   │   ├── UpdateTaskStatusUseCase.java
│   │   │   ├── ListTasksUseCase.java
│   │   │   └── MarkTaskAsDoneUseCase.java
│   │   ├── timeblock/
│   │   │   ├── CreateTimeBlockUseCase.java
│   │   │   ├── UpdateTimeBlockUseCase.java
│   │   │   ├── ListTimeBlocksUseCase.java
│   │   │   └── DeleteTimeBlockUseCase.java
│   │   ├── worksession/
│   │   │   ├── CreateWorkSessionUseCase.java
│   │   │   ├── ListWorkSessionsUseCase.java
│   │   │   └── GetMetricsUseCase.java
│   │   └── organization/
│   │       ├── CreateOrganizationUseCase.java
│   │       ├── AddMemberUseCase.java
│   │       └── ListOrganizationsUseCase.java
│   ├── valueobject/                           # Value Objects
│   │   ├── ProjectColor.java
│   │   ├── TaskStatus.java
│   │   ├── TimeRange.java
│   │   └── OrganizationRole.java
│   └── validator/                             # Business rule validators
│       ├── ProjectValidator.java
│       ├── TaskValidator.java
│       └── TimeBlockValidator.java
├── infrastructure/                            # FRAMEWORK-DEPENDENT (Spring/JPA)
│   ├── config/                                # Configuration classes
│   │   ├── SecurityConfig.java
│   │   ├── OAuth2Config.java
│   │   ├── FlywayConfig.java
│   │   ├── RedisConfig.java
│   │   └── JpaConfig.java
│   ├── controller/                            # REST API controllers
│   │   ├── ProjectController.java
│   │   ├── TaskController.java
│   │   ├── TimeBlockController.java
│   │   ├── WorkSessionController.java
│   │   ├── OrganizationController.java
│   │   └── AuthController.java
│   ├── dto/                                   # Data Transfer Objects
│   │   ├── request/
│   │   │   ├── CreateProjectRequest.java
│   │   │   ├── UpdateTaskStatusRequest.java
│   │   │   └── ...
│   │   └── response/
│   │       ├── ProjectResponse.java
│   │       ├── TaskResponse.java
│   │       └── ...
│   ├── entity/                                # JPA entities (Database mapping)
│   │   ├── ProjectJpaEntity.java
│   │   ├── TaskJpaEntity.java
│   │   ├── TimeBlockJpaEntity.java
│   │   ├── WorkSessionJpaEntity.java
│   │   ├── UserJpaEntity.java
│   │   └── OrganizationJpaEntity.java
│   ├── gateway/                               # Gateway implementations
│   │   ├── JpaProjectGateway.java
│   │   ├── JpaTaskGateway.java
│   │   ├── JpaTimeBlockGateway.java
│   │   ├── JpaWorkSessionGateway.java
│   │   ├── JpaUserGateway.java
│   │   ├── JpaOrganizationGateway.java
│   │   └── RedisCacheGateway.java
│   ├── mapper/                                # Domain ↔ DTO/JPA mappers
│   │   ├── ProjectMapper.java
│   │   ├── TaskMapper.java
│   │   └── ...
│   ├── persistence/                           # Spring Data JPA repositories
│   │   ├── SpringDataProjectRepository.java
│   │   ├── SpringDataTaskRepository.java
│   │   └── ...
│   ├── security/                              # Security implementation
│   │   ├── jwt/
│   │   │   ├── JwtService.java
│   │   │   └── JwtAuthenticationFilter.java
│   │   ├── oauth2/
│   │   │   └── OAuth2AuthenticationSuccessHandler.java
│   │   └── UserPrincipal.java
│   └── exception/                             # Global exception handling
│       ├── GlobalExceptionHandler.java
│       └── ErrorResponse.java
└── KairosApplication.java                     # Main entry point
```

### 3.2 Layer Responsibilities

| Layer | Responsibility | Framework Dependencies |
|-------|---------------|------------------------|
| **core/** | Business logic, domain rules, use case orchestration | ❌ None (pure Java) |
| **infrastructure/** | HTTP, persistence, security, external integrations | ✅ Spring, JPA, Security, etc. |

### 3.3 Key Principles

1. **Core layer** is completely framework-agnostic
2. **Infrastructure layer** depends on Core, not vice versa
3. **Gateway interfaces** in Core are implemented by Infrastructure
4. **Use cases** in Core orchestrate business logic via gateways
5. **Controllers** in Infrastructure delegate to Core use cases

---

## 4. Core Layer Design (FRAMEWORK-AGNOSTIC)

### 4.1 Domain Entities

All domain entities are **Plain Old Java Objects (POJOs)** with **ZERO** framework dependencies.

#### Project.java

```java
package com.felipemelozx.kairos.core.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Core Project entity - represents a long-term context or objective.
 *
 * FRAMEWORK-AGNOSTIC: No Spring, JPA, or Lombok annotations.
 */
public final class Project {

    private final UUID id;
    private final UUID organizationId;
    private final String name;
    private final String description;
    private final ProjectColor color;
    private final ProjectStatus status;
    private final Instant createdAt;
    private final Instant deletedAt;

    private Project(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id is required");
        this.organizationId = Objects.requireNonNull(builder.organizationId, "organizationId is required");
        this.name = Objects.requireNonNull(builder.name, "name is required");
        this.description = builder.description;
        this.color = Objects.requireNonNull(builder.color, "color is required");
        this.status = Objects.requireNonNull(builder.status, "status is required");
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt is required");
        this.deletedAt = builder.deletedAt;
    }

    // Getters only - immutability enforced
    public UUID id() { return id; }
    public UUID organizationId() { return organizationId; }
    public String name() { return name; }
    public String description() { return description; }
    public ProjectColor color() { return color; }
    public ProjectStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant deletedAt() { return deletedAt; }

    // Business logic methods
    public boolean isActive() {
        return status == ProjectStatus.ACTIVE && deletedAt == null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public Project archive() {
        return new Builder()
                .from(this)
                .status(ProjectStatus.ARCHIVED)
                .build();
    }

    public Project softDelete() {
        return new Builder()
                .from(this)
                .deletedAt(Instant.now())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id = UUID.randomUUID();
        private UUID organizationId;
        private String name;
        private String description;
        private ProjectColor color = ProjectColor.DEFAULT;
        private ProjectStatus status = ProjectStatus.ACTIVE;
        private Instant createdAt = Instant.now();
        private Instant deletedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder color(ProjectColor color) { this.color = color; return this; }
        public Builder status(ProjectStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder deletedAt(Instant deletedAt) { this.deletedAt = deletedAt; return this; }

        public Builder from(Project project) {
            this.id = project.id;
            this.organizationId = project.organizationId;
            this.name = project.name;
            this.description = project.description;
            this.color = project.color;
            this.status = project.status;
            this.createdAt = project.createdAt;
            this.deletedAt = project.deletedAt;
            return this;
        }

        public Project build() {
            return new Project(this);
        }
    }
}
```

#### Task.java

```java
package com.felipemelozx.kairos.core.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Core Task entity - represents a unit of work.
 *
 * Business Rule: Task can only be marked DONE if execution time is logged.
 */
public final class Task {

    private final UUID id;
    private final UUID organizationId;
    private final String title;
    private final String description;
    private final TaskStatus status;
    private final UUID projectId;
    private final Optional<UUID> timeBlockId;
    private final Instant createdAt;
    private final Instant deletedAt;

    private Task(Builder builder) {
        this.id = Objects.requireNonNull(builder.id);
        this.organizationId = Objects.requireNonNull(builder.organizationId);
        this.title = Objects.requireNonNull(builder.title);
        this.description = builder.description;
        this.status = Objects.requireNonNull(builder.status);
        this.projectId = Objects.requireNonNull(builder.projectId);
        this.timeBlockId = Optional.ofNullable(builder.timeBlockId);
        this.createdAt = Objects.requireNonNull(builder.createdAt);
        this.deletedAt = builder.deletedAt;
    }

    // Business logic
    public boolean isDone() {
        return status == TaskStatus.DONE;
    }

    /**
     * Transition task to DONE status.
     *
     * @throws DomainException if task has no logged execution time
     */
    public Task markAsDone(boolean hasExecutionLog) {
        if (!hasExecutionLog) {
            throw new DomainException("Task cannot be marked as DONE without execution log");
        }
        return new Builder()
                .from(this)
                .status(TaskStatus.DONE)
                .build();
    }

    public Task start() {
        if (status != TaskStatus.TODO) {
            throw new DomainException("Task must be in TODO status to start");
        }
        return new Builder()
                .from(this)
                .status(TaskStatus.DOING)
                .build();
    }

    public Task resetToTodo() {
        return new Builder()
                .from(this)
                .status(TaskStatus.TODO)
                .build();
    }

    // Getters, Builder pattern...
}
```

### 4.2 Value Objects

```java
package com.felipemelozx.kairos.core.valueobject;

import java.util.Objects;

/**
 * ProjectColor value object - represents a hex color.
 */
public final class ProjectColor {

    private static final int HEX_COLOR_LENGTH = 7;
    private static final String HEX_PREFIX = "#";

    private final String value;

    private ProjectColor(String value) {
        if (!isValidHexColor(value)) {
            throw new IllegalArgumentException("Invalid hex color format: " + value);
        }
        this.value = value;
    }

    public static ProjectColor of(String hex) {
        return new ProjectColor(hex);
    }

    public static ProjectColor random() {
        // Generate random hex color
        String randomColor = String.format("#%06x", (int) (Math.random() * 0xFFFFFF));
        return new ProjectColor(randomColor);
    }

    public static final ProjectColor DEFAULT = ProjectColor.of("#3B82F6");

    private boolean isValidHexColor(String value) {
        return value != null
                && value.length() == HEX_COLOR_LENGTH
                && value.startsWith(HEX_PREFIX)
                && value.substring(1).matches("[0-9A-Fa-f]+");
    }

    public String value() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectColor that = (ProjectColor) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
```

### 4.3 Domain Exceptions

```java
package com.felipemelozx.kairos.core.exception;

/**
 * Base exception for all domain rule violations.
 */
public class DomainException extends RuntimeException {

    private final String errorCode;

    public DomainException(String message) {
        super(message);
        this.errorCode = "DOMAIN_ERROR";
    }

    public DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
```

```java
package com.felipemelozx.kairos.core.exception;

/**
 * Thrown when attempting to complete a task without execution logs.
 */
public class TaskCompletionException extends DomainException {

    public TaskCompletionException(String message) {
        super("TASK_COMPLETION_ERROR", message);
    }
}
```

---

## 5. Application Layer Design

### 5.1 Use Case Pattern

All use cases implement a **consistent interface**:

```java
package com.felipemelozx.kairos.core.usecase.project;

import com.felipemelozx.kairos.infrastructure.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.infrastructure.dto.response.ProjectResponse;
import com.felipemelozx.kairos.core.gateway.ProjectGateway;
import com.felipemelozx.kairos.core.domain.Project;
import com.felipemelozx.kairos.core.exception.DomainException;
import com.felipemelozx.kairos.core.validator.ProjectValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use Case: Create a new project.
 *
 * Responsibilities:
 * - Validate input (DTO level + business rules)
 * - Execute business logic
 * - Persist entity via gateway
 * - Log execution
 * - Return response
 */
public final class CreateProjectUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateProjectUseCase.class);

    private final ProjectGateway projectGateway;
    private final ProjectValidator projectValidator;

    public CreateProjectUseCase(ProjectGateway projectGateway,
                                ProjectValidator projectValidator) {
        this.projectGateway = Objects.requireNonNull(projectGateway);
        this.projectValidator = Objects.requireNonNull(projectValidator);
    }

    /**
     * Execute the use case.
     *
     * @param request the create project request
     * @return the created project response
     * @throws DomainException if business rules are violated
     */
    public ProjectResponse execute(CreateProjectRequest request) {
        log.info("Executing CreateProjectUseCase with request: {}", request);

        // 1. Validate DTO constraints (Bean Validation already handled)
        projectValidator.validateName(request.name());
        projectValidator.validateColor(request.color());

        // 2. Build domain entity
        Project project = Project.builder()
                .organizationId(request.organizationId())
                .name(request.name())
                .description(request.description())
                .color(ProjectColor.of(request.color()))
                .build();

        // 3. Persist via gateway
        Project savedProject = projectGateway.save(project);

        // 4. Map to response
        ProjectResponse response = ProjectMapper.toResponse(savedProject);

        log.info("Successfully created project with id: {}", savedProject.id());
        return response;
    }
}
```

### 5.2 Input Validation (Two-Level)

#### Level 1: DTO Validation (Bean Validation)

```java
package com.felipemelozx.kairos.infrastructure.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a project.
 *
 * Bean Validation handles structural constraints.
 */
public record CreateProjectRequest(

        @NotNull(message = "organizationId is required")
        UUID organizationId,

        @NotBlank(message = "name is required")
        @Size(min = 1, max = 100, message = "name must be between 1 and 100 characters")
        String name,

        @Size(max = 500, message = "description must not exceed 500 characters")
        String description,

        @NotBlank(message = "color is required")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "color must be a valid hex color")
        String color

) {
}
```

#### Level 2: Business Rule Validation

```java
package com.felipemelozx.kairos.core.validator;

import com.felipemelozx.kairos.core.exception.DomainException;

/**
 * Validator for Project business rules.
 */
public final class ProjectValidator {

    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 100;
    private static final Set<String> RESERVED_NAMES = Set.of("all", "inbox", "today");

    public void validateName(String name) {
        if (name == null || name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw new DomainException("INVALID_PROJECT_NAME",
                    "Project name must be between 1 and 100 characters");
        }

        if (RESERVED_NAMES.contains(name.toLowerCase())) {
            throw new DomainException("RESERVED_PROJECT_NAME",
                    "Project name '" + name + "' is reserved");
        }
    }

    public void validateColor(String hexColor) {
        if (hexColor == null) {
            throw new DomainException("INVALID_COLOR", "Color cannot be null");
        }
        // Additional business rules for colors if needed
    }
}
```

---

## 6. Infrastructure Layer Design

### 6.1 Persistence (JPA)

#### JPA Entity

```java
package com.felipemelozx.kairos.infrastructure.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for Project persistence.
 *
 * NOTE: This is separate from the domain entity to maintain clean architecture.
 */
@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_projects_org_status", columnList = "organization_id, status"),
    @Index(name = "idx_projects_org_deleted", columnList = "organization_id, deleted_at")
})
public final class ProjectJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProjectStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Constructors, getters, setters...
}
```

#### Repository Implementation

```java
package com.felipemelozx.kairos.infrastructure.gateway;

import com.felipemelozx.kairos.core.gateway.ProjectGateway;
import com.felipemelozx.kairos.core.domain.Project;
import com.felipemelozx.kairos.infrastructure.entity.ProjectJpaEntity;
import com.felipemelozx.kairos.infrastructure.mapper.ProjectMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Gateway implementation for Project.
 *
 * This bridges the core layer (gateway interface) with JPA persistence.
 */
@Repository
public final class JpaProjectGateway implements ProjectGateway {

    private final SpringDataProjectRepository springRepository;
    private final ProjectMapper mapper;

    public JpaProjectGateway(SpringDataProjectRepository springRepository,
                             ProjectMapper mapper) {
        this.springRepository = springRepository;
        this.mapper = mapper;
    }

    @Override
    public Project save(Project project) {
        ProjectJpaEntity jpaEntity = mapper.toJpaEntity(project);
        ProjectJpaEntity saved = springRepository.save(jpaEntity);
        return mapper.toDomainEntity(saved);
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return springRepository.findById(id)
                .map(mapper::toDomainEntity);
    }

    @Override
    public List<Project> findByOrganizationId(UUID organizationId) {
        return springRepository.findByOrganizationIdAndDeletedAtIsNull(organizationId)
                .stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springRepository.softDeleteById(id, Instant.now());
    }

    /**
     * Spring Data JPA interface - internal to infrastructure layer.
     */
    interface SpringDataProjectRepository extends JpaRepository<ProjectJpaEntity, UUID> {
        @Query("SELECT p FROM ProjectJpaEntity p WHERE p.organizationId = :orgId AND p.deletedAt IS NULL")
        List<ProjectJpaEntity> findByOrganizationIdAndDeletedAtIsNull(@Param("orgId") UUID orgId);

        @Modifying
        @Query("UPDATE ProjectJpaEntity p SET p.deletedAt = :deletedAt WHERE p.id = :id")
        void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);
    }
}
```

### 6.2 Security Architecture

#### JWT Service

```java
package com.felipemelozx.kairos.infrastructure.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Token generation and validation service.
 */
@Service
public final class JwtService {

    private final Key signingKey;
    private final long accessTokenExpiration; // 15 minutes
    private final long refreshTokenExpiration; // 7 days

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-token-expiration:900000}") long accessExpiration,
                      @Value("${jwt.refresh-token-expiration:604800000}") long refreshExpiration) {
        this.signingKey = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secret));
        this.accessTokenExpiration = accessExpiration;
        this.refreshTokenExpiration = refreshExpiration;
    }

    public String generateAccessToken(UUID userId, String email, String name) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(accessTokenExpiration);

        return Jwts.builder()
                .setSubject(userId.toString())
                .addClaims(Map.of(
                        "email", email,
                        "name", name,
                        "type", "access"
                ))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(refreshTokenExpiration);

        return Jwts.builder()
                .setSubject(userId.toString())
                .addClaims(Map.of("type", "refresh"))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            throw new SecurityException("Invalid JWT token", e);
        }
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }
}
```

#### OAuth2 Success Handler

```java
package com.felipemelozx.kairos.infrastructure.security.oauth2;

import com.felipemelozx.kairos.infrastructure.dto.response.AuthResponse;
import com.felipemelozx.kairos.core.gateway.UserGateway;
import com.felipemelozx.kairos.core.domain.User;
import com.felipemelozx.kairos.infrastructure.security.jwt.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles successful OAuth2 authentication (Google Login).
 */
@Component
public final class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserGateway userGateway;
    private final JwtService jwtService;

    public OAuth2AuthenticationSuccessHandler(UserGateway userGateway,
                                             JwtService jwtService) {
        this.userGateway = userGateway;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String providerId = (String) attributes.get("sub");

        // Find or create user
        User user = userGateway.findByEmailAndProvider(email, "GOOGLE")
                .orElseGet(() -> createNewUser(email, name, providerId));

        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user.id(), user.email(), user.name());
        String refreshToken = jwtService.generateRefreshToken(user.id());

        // Return tokens in response
        AuthResponse authResponse = new AuthResponse(accessToken, refreshToken, user);

        response.setContentType("application/json");
        response.getWriter().write(toJson(authResponse));
    }

    private User createNewUser(String email, String name, String providerId) {
        User newUser = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name(name)
                .provider("GOOGLE")
                .providerId(providerId)
                .active(true)
                .createdAt(Instant.now())
                .build();

        return userGateway.save(newUser);
    }
}
```

### 6.3 Caching Strategy

```java
package com.felipemelozx.kairos.infrastructure.cache;

import com.felipemelozx.kairos.core.gateway.CacheGateway;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis cache gateway implementation.
 */
@Component
public final class RedisCacheGateway implements CacheGateway {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCacheAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    @Override
    public void evict(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void evictByPattern(String pattern) {
        redisTemplate.keys(pattern).forEach(redisTemplate::delete);
    }
}
```

**Cache Strategy:**

| Data Type | TTL | Eviction Strategy |
|-----------|-----|-------------------|
| Project by ID | 1 hour | Time-based |
| User profile | 30 minutes | Time-based |
| Organization members | 15 minutes | Time-based + write-through |
| Task lists | 5 minutes | Aggressive (frequent updates) |

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

### 7.2 Controller Example

```java
package com.felipemelozx.kairos.infrastructure.controller;

import com.felipemelozx.kairos.infrastructure.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.infrastructure.dto.response.ProjectResponse;
import com.felipemelozx.kairos.core.usecase.project.CreateProjectUseCase;
import com.felipemelozx.kairos.core.usecase.project.ListProjectsUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Project management.
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/projects")
public final class ProjectController {

    private final CreateProjectUseCase createProjectUseCase;
    private final ListProjectsUseCase listProjectsUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        // Override organizationId from path for security
        CreateProjectRequest scopedRequest = new CreateProjectRequest(
                organizationId,
                request.name(),
                request.description(),
                request.color()
        );

        ProjectResponse response = createProjectUseCase.execute(scopedRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> listProjects(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal Jwt jwt) {

        List<ProjectResponse> projects = listProjectsUseCase.execute(organizationId);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
            @PathVariable UUID organizationId,
            @PathVariable UUID projectId) {

        // Implementation...
        return ResponseEntity.ok(ApiResponse.success(/* ... */));
    }
}
```

### 7.3 Response Envelope

```java
package com.felipemelozx.kairos.infrastructure.controller;

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
        return new ApiResponse<>(
                true,
                data,
                null,
                Instant.now(),
                API_VERSION
        );
    }

    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(
                false,
                null,
                error,
                Instant.now(),
                API_VERSION
        );
    }
}
```

---

## 8. Cross-Cutting Concerns

### 8.1 Exception Handling

```java
package com.felipemelozx.kairos.infrastructure.exception;

import com.felipemelozx.kairos.core.exception.DomainException;
import com.felipemelozx.kairos.infrastructure.controller.ApiResponse;
import com.felipemelozx.kairos.infrastructure.controller.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for consistent error responses.
 */
@RestControllerAdvice
public final class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException ex) {
        ErrorResponse error = new ErrorResponse(
                ex.errorCode(),
                ex.getMessage(),
                null
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });

        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed",
                details
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                null
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(error));
    }
}
```

### 8.2 Logging Strategy

```java
package com.felipemelozx.kairos.infrastructure.util;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Logging utility for consistent log formatting.
 */
@Component
public final class LoggingHelper {

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String USER_ID_KEY = "userId";
    private static final String ORGANIZATION_ID_KEY = "organizationId";

    public static void setRequestContext(UUID userId, UUID organizationId) {
        MDC.put(REQUEST_ID_KEY, UUID.randomUUID().toString());
        if (userId != null) {
            MDC.put(USER_ID_KEY, userId.toString());
        }
        if (organizationId != null) {
            MDC.put(ORGANIZATION_ID_KEY, organizationId.toString());
        }
    }

    public static void clearRequestContext() {
        MDC.clear();
    }
}
```

---

## 9. Deployment Architecture

### 9.1 Docker Compose (Development)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16.8-alpine
    container_name: kairos-postgres
    environment:
      POSTGRES_DB: kairos
      POSTGRES_USER: kairos
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U kairos"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: kairos-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: kairos-app
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/kairos
      SPRING_DATASOURCE_USERNAME: kairos
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  postgres_data:
  redis_data:
```

### 9.2 Production Infrastructure

```
┌─────────────────────────────────────────────────────────┐
│                     Nginx / Cloud LB                     │
│                    (SSL Termination)                     │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              Kairos Backend (x2 instances)              │
│         (Blue-Green Deployment, Zero Downtime)          │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
┌───────▼────────┐      ┌────────▼────────┐
│  PostgreSQL    │      │     Redis       │
│  (Primary)     │      │   (Cache)       │
└────────────────┘      └─────────────────┘
        │
┌───────▼────────┐
│  PostgreSQL    │
│  (Replica)     │
│  (Read Replicas)│
└────────────────┘
```

### 9.3 Environment Configuration

| Environment | Profile | Database | Cache | Deployment |
|-------------|---------|----------|-------|------------|
| Development | `dev` | Docker Compose | Docker Compose | Local |
| Staging | `staging` | Cloud PostgreSQL | Cloud Redis | GitHub Actions |
| Production | `prod` | VPS PostgreSQL | VPS Redis | GitHub Actions |

---

## 10. Testing Strategy

### 10.1 Test Pyramid

```
                    ┌──────┐
                    │ E2E  │  (5% - Critical flows only)
                    ├──────┤
                    │  IT  │  (20% - Integration tests)
                    ├──────┤
                    │ Unit │  (75% - Use cases, domain logic)
                    └──────┘
```

### 10.2 Unit Test Example

```java
package com.felipemelozx.kairos.core.usecase.project;

import com.felipemelozx.kairos.application.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.application.dto.response.ProjectResponse;
import com.felipemelozx.kairos.application.ports.out.ProjectRepository;
import com.felipemelozx.kairos.domain.entity.Project;
import com.felipemelozx.kairos.domain.exception.DomainException;
import com.felipemelozx.kairos.domain.validator.ProjectValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateProjectUseCaseTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectValidator projectValidator;

    @InjectMocks
    private CreateProjectUseCase useCase;

    @Test
    void shouldCreateProjectSuccessfully() {
        // Given
        UUID orgId = UUID.randomUUID();
        CreateProjectRequest request = new CreateProjectRequest(
                orgId,
                "My Project",
                "Description",
                "#3B82F6"
        );

        Project savedProject = Project.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .name("My Project")
                .build();

        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        // When
        ProjectResponse response = useCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        verify(projectValidator).validateName("My Project");
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void shouldThrowExceptionWhenNameIsReserved() {
        // Given
        CreateProjectRequest request = new CreateProjectRequest(
                UUID.randomUUID(),
                "inbox",
                "Description",
                "#3B82F6"
        );

        doThrow(new DomainException("RESERVED_PROJECT_NAME", "Name is reserved"))
                .when(projectValidator).validateName("inbox");

        // When / Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("reserved");

        verify(projectRepository, never()).save(any(Project.class));
    }
}
```

### 10.3 Integration Test Example (Testcontainers)

```java
package com.felipemelozx.kairos.infrastructure.gateway;

import com.felipemelozx.kairos.core.domain.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
class JpaProjectGatewayIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:16.8-alpine"
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaProjectGateway gateway;

    @Test
    void shouldSaveAndRetrieveProject() {
        // Given
        UUID orgId = UUID.randomUUID();
        Project project = Project.builder()
                .organizationId(orgId)
                .name("Test Project")
                .build();

        // When
        Project saved = gateway.save(project);
        List<Project> projects = gateway.findByOrganizationId(orgId);

        // Then
        assertThat(projects).hasSize(1);
        assertThat(projects.get(0).name()).isEqualTo("Test Project");
    }
}
```

---

## 11. Performance Optimization

### 11.1 Database Optimization

| Strategy | Implementation |
|----------|----------------|
| **Partial Indexes** | Index only non-deleted records |
| **Connection Pooling** | HikariCP (default in Spring Boot) |
| **Query Optimization** | Use JOIN FETCH for eager loading |
| **Batch Operations** | `@BatchSize` for collections |

### 11.2 Caching Strategy

```java
@Cacheable(value = "projects", key = "#id")
public Project findById(UUID id) {
    return projectGateway.findById(id).orElseThrow();
}

@CacheEvict(value = "projects", key = "#project.id")
public Project update(Project project) {
    return projectGateway.save(project);
}
```

### 11.3 API Performance Targets

| Endpoint | Target (p95) | Strategy |
|----------|-------------|----------|
| GET /projects | < 100ms | Cache + indexed query |
| GET /tasks | < 150ms | Pagination + cache |
| POST /work-sessions | < 200ms | Async write-through cache |

---

## 12. Security Architecture

### 12.1 Authentication Flow

```
┌─────────┐                    ┌─────────────┐                    ┌──────────┐
│ Frontend│ ──OAuth2 Login──▶  │   Google    │ ──Callback + Code──▶ │ Backend  │
└─────────┘                    └─────────────┘                    └──────────┘
                                                                        │
                                                                        ▼
                                                                 ┌──────────────┐
                                                                 │ JWT Tokens   │
                                                                 │ (Access +    │
                                                                 │  Refresh)    │
                                                                 └──────────────┘
                                                                        │
                                                                 ┌──────▼──────┐
                                                                 │   Response   │
                                                                 │  with tokens │
                                                                 └──────────────┘
```

### 12.2 Authorization Flow

```
┌──────────┐
│ Request  │ ──JWT──▶ ┌──────────────────┐
│ + JWT    │          │ JWT Filter       │
└──────────┘          │ (Validate Token) │
                      └─────────┬─────────┘
                                │
                         ┌──────▼──────┐
                         │ User Context │
                         │ (Principal)  │
                         └──────┬───────┘
                                │
                      ┌─────────▼─────────┐
                      │ Controller Method │
                      │ @PreAuthorize     │
                      └───────────────────┘
```

### 12.3 Organization Scope Validation

Every request must include `organizationId` in path. System validates:

1. JWT contains valid user
2. User is member of the organization
3. User has required role (OWNER/ADMIN/MEMBER)

---

## 13. Migration Strategy

### 13.1 Flyway Migrations

```
src/main/resources/db/migration/
├── V1__Create_Users_Table.sql
├── V2__Create_Organizations_Table.sql
├── V3__Create_Organization_Members_Table.sql
├── V4__Create_Projects_Table.sql
├── V5__Create_Tasks_Table.sql
├── V6__Create_Time_Blocks_Table.sql
├── V7__Create_Work_Sessions_Table.sql
└── V8__Create_Indexes.sql
```

### 13.2 Migration Best Practices

- **Never modify** existing migrations (create new ones)
- **Use transactions** for data safety
- **Test migrations** on copy of production data
- **Rollback scripts** for critical migrations

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

### 14.2 Metrics Collection

```java
@Component
public final class MetricsHelper {

    private final MeterRegistry meterRegistry;

    public void recordProjectCreated(String organizationId) {
        Counter.builder("project.created")
                .tag("organization", organizationId)
                .register(meterRegistry)
                .increment();
    }

    public void recordTaskCompletion(Duration duration) {
        Timer.builder("task.completion.duration")
                .register(meterRegistry)
                .record(duration);
    }
}
```

### 14.3 Distributed Tracing (Future)

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

---

## 15. Development Workflow

### 15.1 Git Workflow

```
main (production)
  ↑
  │ (merge after PR approval)
  │
develop (integration)
  ↑
  │ (merge after review)
  │
feature/xxx (branch from develop)
```

### 15.2 Conventional Commits

```
feat: add project creation endpoint
fix: resolve task completion validation error
docs: update backend architecture documentation
refactor: extract validator to separate class
test: add integration tests for work sessions
chore: upgrade Spring Boot to 4.0.1
```

### 15.3 Code Review Checklist

- [ ] Follows clean architecture principles
- [ ] Domain layer is framework-agnostic
- [ ] Tests included (unit + integration)
- [ ] No security vulnerabilities
- [ ] Logging added for use cases
- [ ] Performance impact assessed
- [ ] Documentation updated

---

## 16. Architecture Decision Records (ADRs)

### ADR-001: Clean Architecture

**Status:** Accepted

**Context:** Need maintainable, testable backend for long-term product evolution.

**Decision:** Implement Clean/Hexagonal Architecture with strict layer separation.

**Consequences:**
- ✅ Framework-agnostic domain layer
- ✅ High testability
- ✅ Clear separation of concerns
- ❌ More boilerplate code
- ❌ Steeper learning curve for new developers

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
| Create a new use case | `core/usecase/{domain}/` |
| Add domain entity | `core/domain/` |
| Implement gateway | `infrastructure/gateway/` |
| Add REST endpoint | `infrastructure/controller/` |
| Configure security | `infrastructure/config/` |
| Add business rule validator | `core/validator/` |
| Create DTO | `infrastructure/dto/request/` or `infrastructure/dto/response/` |
| Add JPA entity | `infrastructure/entity/` |
| Create mapper | `infrastructure/mapper/` |

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

**Document Version:** 1.1
**Last Updated:** 2026-03-04
**Author:** Aria (Architect Agent) 🏛️
**Status:** Draft - Updated Package Structure (core/infrastructure)
**Changes:**
- Simplified package structure from 4-layer (application/domain/infrastructure/global) to 2-tier (core/infrastructure)
- Renamed "repository" to "gateway" throughout
- Updated all code examples and package references
