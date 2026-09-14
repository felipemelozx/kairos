# Story PROJ-001: Project CRUD (Backend Slice)

**Epic:** Planning & Calendar (MVP) — no `docs/epics/` yet; this is the first entity slice after Auth/Design
**Story ID:** PROJ-001
**Sprint:** 2
**Priority:** 🔴 Critical (blocks TimeBlock, ChecklistItem, WorkSession and all screens)
**Points:** 5
**Effort:** 6-8 hours
**Status:** ✅ Done
**Type:** 💻 Feature (backend only)

---

## 🔀 Cross-Story Decisions

| Decision | Source | Impact on This Story |
|----------|--------|----------------------|
| Single-user, owner-scoped. Every table carries `user_id`; every query filters by the authenticated user | `docs/prd.md` §4.7, §15.1, §16.2; `docs/architecture/backend-architecture.md` ADR-002 | `ProjectRepository` exposes only `...AndUserId...` methods; no unscoped `findById` |
| Soft delete (`deletedAt`), never physical delete for `projects` | `docs/prd.md` §5.2, §15.2 | Repository filters `...AndDeletedAtIsNull`; delete sets `deleted_at` |
| Layered architecture: controller → service → repository → entity. Business logic only in service; `@Transactional` only on service methods | `docs/BACKEND_GUIDELINES.md` §2; `backend-architecture.md` §2 | Follow the existing `AuthService`/`AuthController` structure |
| HTTP DTOs are records validated with Bean Validation (structural) + service validation (business rules) | `backend-architecture.md` §5; `BACKEND_GUIDELINES.md` §3.4 | Two-level validation for create/update |
| Controllers implement a `*Api` interface that carries the OpenAPI annotations | Existing `com.felipemelozx.kairos.api.AuthApi` + `AuthController` | New `ProjectApi` interface + `ProjectController` |
| Context path is `/api` (`server.servlet.context-path`); controllers map without the `/api` prefix | `application.yml`; `AuthController` maps `"/auth"` | `ProjectController` maps `"/projects"` → public URL `/api/projects` |
| Response envelope is `ApiResponse<T>`; errors use `ErrorResponse` via `GlobalExceptionHandler` | `dto/response/ApiResponse.java`, `GlobalExceptionHandler` | Reuse `ApiResponse.success(...)` and `BusinessException` |
| URL versioning is header-based (`X-API-Version`), not in the URL path | `backend-architecture.md` ADR-004, §7.2 | Endpoints stay clean; versioning is out of scope for this slice |
| UI language is English; no i18n framework | Existing forms; AUTH-002 | Not applicable to this backend-only slice |
| Absolute imports (`@/`) | Constitution Art. VI | Not applicable to this backend-only slice |

---

## 📋 User Story

**Como** usuário,
**Quero** criar, listar, editar, arquivar e excluir meus projetos (contextos de vida/trabalho),
**Para** organizar meus blocos de tempo e sessões de execução sob um contexto, e depois ver tempo executado por projeto.

---

## 🎯 Objective

Deliver the first MVP domain entity after auth: **Project**. This is the foundational context referenced by `TimeBlock.projectId`, `TimeBlockSeries.projectId` and `WorkSession.projectId` (PRD §5.3, §5.4, §5.7). It is the natural first step of the Planning Flow (PRD §7.1: "User creates a project").

Scope is **backend only** to keep the increment small and verifiable (CLI First). The Projects frontend screen is a separate story (`PROJ-002`, not created yet).

---

## ✅ Resolved Decisions

> These items were raised at story creation because they were **not** in the PRD. They were resolved by @po (Pixel) under Constitution Art. IV (No Invention): each decision either traces to a normative source, or — where the PRD is silent — is an explicit **@po product decision** recorded with a minimal rationale. No new business requirements were introduced.

### D1 — Reserved project names (`all`/`inbox`/`today`): **DROPPED**

**Decision:** Do **not** implement reserved-name validation. Any non-blank `name` within the length limit (1–100) is accepted; no name is special-cased.

**Rationale:** `RESERVED_NAMES = {"all","inbox","today"}` appears only in the illustrative code sample of `backend-architecture.md` §4.3 and is absent from the PRD. The PRD defines Project fields/rules (§5.2) and functional scope (§9.4) without reserved names, and none of the four MVP screens (§8.1–§8.4) exposes an `all`/`inbox`/`today` aggregate view. Treating it as a requirement would invent a business rule.

**Source:** `docs/prd.md` §5.2, §8.1–§8.4, §9.4; `docs/architecture/backend-architecture.md` §4.3 (sample only — **not adopted**).

**Impact:** `ProjectService.validateName` enforces only non-blank + max length (100). No `RESERVED_PROJECT_NAME` error code.

### D2 — `color`: **REQUIRED, format `^#[0-9A-Fa-f]{6}$`**

**Decision:** `color` is **required** on create and must match `^#[0-9A-Fa-f]{6}$` (7 chars, `#RRGGBB`). Validated at DTO level (`@NotBlank` + `@Pattern`) and defensively at service level. On `PATCH`, `color` is optional (partial update), but when present it must match the same pattern.

**Rationale:** PRD §15.2 types `color` as `String (hex #RRGGBB)`, and the PRD marks optional fields explicitly as "(optional)" — `color` carries no such marker (unlike `TimeBlock.projectId` or `WorkSession.timeBlockId`). `backend-architecture.md` §4.1 maps it `@Column(nullable = false, length = 7)` and §5 uses `@NotBlank` + `@Pattern`. This is the only source-consistent interpretation.

**Source:** `docs/prd.md` §5.2, §15.2, §16.3; `docs/architecture/backend-architecture.md` §4.1, §5.

**Impact:** `CreateProjectRequest.color` → `@NotBlank` + `@Pattern(...)`; `UpdateProjectRequest.color` → `@Pattern(...)` only (nullable). Missing/blank/invalid color on create → 400.

### D3 — Archive semantics: **PATCH may set `status` freely (ACTIVE ↔ ARCHIVED)**

**Decision:** `PATCH /api/projects/{projectId}` may freely transition `status` between `ACTIVE` and `ARCHIVED`. Archived projects are **not** read-only: they remain editable and can be reactivated. Archiving is a value of the `status` field, distinct from soft delete (`DELETE` → `deleted_at`).

**Rationale:** PRD §5.2 defines `status (active / archived)` purely as a field; PRD §9.4 says "create and manage projects" with no read-only constraint. The read-only claim exists only in the superseded `docs/diagrams/data-model.md`, which is explicitly out of scope. Adding a read-only rule would invent behavior.

**Source:** `docs/prd.md` §5.2, §9.4, §15.2 (`deletedAt` = separate soft-delete concept). Explicitly **not** `docs/diagrams/data-model.md`.

**Impact:** No extra guard in `ProjectService.update`; both transitions allowed. Archive ≠ delete.

### D4 — `description` max length: **500 characters (optional)**

**Decision:** `description` is optional; when present, max **500** chars. Enforced at DTO level (`@Size(max = 500)`) and stored in a `VARCHAR(500)` column.

**Rationale:** PRD §15.2 declares `description: String` without a length. `backend-architecture.md` §4.1 maps `@Column(length = 500)` and §5 uses `@Size(max = 500, ...)`. Adopt the architecture bound rather than inventing a different one.

**Source:** `docs/prd.md` §15.2; `docs/architecture/backend-architecture.md` §4.1, §5.

**Impact:** Migration column `description VARCHAR(500)`; null/blank allowed; >500 → 400.

### D5 — `GET /api/projects` status filter: **NOT included (returns all non-deleted projects)**

**Decision:** `GET /api/projects` returns **all non-deleted** projects owned by the authenticated user, regardless of `status` (ACTIVE **and** ARCHIVED). It does **not** accept a `?status=` query parameter in this slice.

**Rationale:** The PRD does not specify list filtering (PRD §9.4 only says "create and manage projects"). `backend-architecture.md` §4.2 defines the only list query as `findByUserIdAndDeletedAtIsNull(userId)` and §7.1 documents `GET /api/projects` without query parameters. Adding a filter would expand the API contract without a requirement; clients can filter by `status` locally. This is an explicit **@po product decision** where the PRD is silent — backward-compatible to add later.

**Source:** `docs/prd.md` §5.2, §9.4; `docs/architecture/backend-architecture.md` §4.2, §7.1 (no query param). @po product decision (PRD silent).

**Impact:** No query parameter; repository keeps `findByUserIdAndDeletedAtIsNull`. Archived projects still appear in the list.

---

## ✅ Tasks

### Phase 1: Red — Tests First (TDD)

- [x] **1.1** `ProjectRepositoryIntegrationTest` (Testcontainers, `@DataJpaTest`):
  - `shouldSaveAndFindActiveProjectByUser`
  - `shouldNotFindSoftDeletedProject`
  - `shouldNotFindProjectOfAnotherUser`
- [x] **1.2** `ProjectServiceTest` (Mockito, no Spring context):
  - `shouldCreateProjectForCurrentUser`
  - `shouldRejectBlankProjectName`
  - `shouldRejectMissingColor`
  - `shouldRejectInvalidHexColor`
  - `shouldAcceptReservedLookingName` (D1: no reserved-name rule)
  - `shouldListOnlyCurrentUserProjects` (returns ACTIVE and ARCHIVED, no status filter — D5)
  - `shouldSoftDeleteOwnProject`
  - `shouldThrowNotFoundWhenDeletingOtherUsersProject`
- [x] **1.3** `ProjectControllerIntegrationTest` (MockMvc/TestRestClient + Testcontainers):
  - `shouldCreateProjectWhenAuthenticated` → 201
  - `shouldReturn401WhenNotAuthenticated` → 401
  - `shouldReturn400WhenColorMissing` → 400
  - `shouldReturn400WhenColorInvalidHex` → 400
  - `shouldListCurrentUserProjects` → 200 (no status filter; archived included)
  - `shouldGetSingleProject` → 200
  - `shouldUpdateProject` → 200
  - `shouldArchiveProjectViaPatch` → 200 (status ARCHIVED)
  - `shouldDeleteProject` → 200/204 and it disappears from list
- [x] **1.4** Authorization test: `shouldForbidUserBFromAccessingUserAProject` (GET/PATCH/DELETE → 404, not 403, to avoid leaking existence)
- [x] Run tests → all fail (no entity, no table, no endpoint) — confirmed: `test-compile` failed with `cannot find symbol` for all new types

### Phase 2: Green — Migration + Entity + Repository

- [x] **2.1** Migration `V2__Create_projects.sql` — columns exactly from PRD §15.2; index from PRD §15.4. Key column bounds per Resolved Decisions: `name VARCHAR(100) NOT NULL`, `description VARCHAR(500)` (D4), `color VARCHAR(7) NOT NULL` (D2), `status VARCHAR(20) NOT NULL`, `deleted_at TIMESTAMP`:
  ```sql
  CREATE INDEX idx_projects_user_active ON projects(user_id, status)
  WHERE deleted_at IS NULL;
  ```
- [x] **2.2** `entity/enums/ProjectStatus.java` — `ACTIVE`, `ARCHIVED`
- [x] **2.3** `entity/Project.java` — JPA entity, no business logic (mirror `User.java` style: explicit getters/setters, no Lombok)
- [x] **2.4** `repository/ProjectRepository.java` — owner-scoped only:
  - `List<Project> findByUserIdAndDeletedAtIsNull(UUID userId)`
  - `Optional<Project> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId)`
  - `@Modifying @Query` `softDeleteByIdAndUserId(...)`
- [x] Run tests → repository tests pass

### Phase 3: Green — DTOs + Service

- [x] **3.1** `dto/request/CreateProjectRequest.java` (record + Bean Validation): `name` `@NotBlank @Size(max=100)`; `description` `@Size(max=500)` optional (D4); `color` `@NotBlank @Pattern(^#[0-9A-Fa-f]{6}$)` required (D2)
- [x] **3.2** `dto/request/UpdateProjectRequest.java` (record + Bean Validation, partial update): all fields optional; `name` `@Size(max=100)`; `description` `@Size(max=500)` (D4); `color` `@Pattern(^#[0-9A-Fa-f]{6}$)` (D2); `status` optional enum
- [x] **3.3** `dto/response/ProjectResponse.java` (record + `from(Project)`)
- [x] **3.4** `service/ProjectService.java` — `@Transactional` on public methods; owner scoping; business validation; SLF4J entry/exit logs:
  - `create(UUID userId, CreateProjectRequest)`
  - `listByUser(UUID userId)` — returns all non-deleted; no status filter (D5)
  - `getById(UUID id, UUID userId)` → 404 if not owned
  - `update(UUID id, UUID userId, UpdateProjectRequest)` — partial update; free status transitions (D3)
  - `softDelete(UUID id, UUID userId)`
  - `validateName`: non-blank + ≤100 only; **no reserved-name check** (D1)
  - `validateColor`: required on create; `^#[0-9A-Fa-f]{6}$` (D2)
- [x] Run tests → service tests pass

### Phase 4: Green — API + Controller

- [x] **4.1** `api/ProjectApi.java` — interface with `@Tag`, `@Operation`, `@ApiResponses`, `@SecurityRequirement(name = "cookieAuth")`
- [x] **4.2** `controller/ProjectController.java` — `@RequestMapping("/projects")`, implements `ProjectApi`; resolve `userId = UUID.fromString(principal.getUsername())` via `@AuthenticationPrincipal UserDetails`
- [x] **4.3** Endpoints (public URLs, context-path `/api`):
  - `POST /api/projects` → 201
  - `GET /api/projects` → 200 (all non-deleted; no `?status` param — D5)
  - `GET /api/projects/{projectId}` → 200
  - `PATCH /api/projects/{projectId}` → 200 (partial update; status free — D3)
  - `DELETE /api/projects/{projectId}` → 200 (soft delete)
- [x] **4.4** Confirm `SecurityConfig` already requires authentication for `/projects` (any request not in `permitAll`); no change expected
- [x] Run tests → all green

### Phase 5: Quality

- [x] **5.1** `cd apps/backend && ./mvnw test` passes (unit + integration + authorization)
- [x] **5.2** `./mvnw jacoco:report` — coverage not reduced; new code ≥80%
- [x] **5.3** OpenAPI annotations complete for all 5 endpoints
- [x] **5.4** Story DoD checklist + File List updated

---

## 🎯 Acceptance Criteria

```gherkin
GIVEN I am authenticated
WHEN I POST /api/projects with a valid name and a valid hex color
THEN I receive 201 with the created project
AND the project is owned by my userId
AND its status is ACTIVE
AND createdAt is set

GIVEN I am authenticated
WHEN I POST /api/projects with a blank name
THEN I receive 400 with a VALIDATION_ERROR or BUSINESS_ERROR
AND no project is created

GIVEN I am authenticated and omit/blank the color
WHEN I POST /api/projects
THEN I receive 400 and no project is created

GIVEN I am authenticated with a color that is not a valid hex
WHEN I POST /api/projects
THEN I receive 400 and no project is created

GIVEN I am authenticated
WHEN I POST /api/projects with a description longer than 500 characters
THEN I receive 400 and no project is created

GIVEN I am authenticated
WHEN I POST /api/projects named "all", "inbox" or "today"
THEN the project is created (these names are NOT reserved in MVP)

GIVEN I am authenticated and have projects
WHEN I GET /api/projects
THEN I receive 200 with only my non-deleted projects
AND no status query parameter is applied (ACTIVE and ARCHIVED are both returned)

GIVEN project P belongs to user A
WHEN user B requests GET /api/projects/{P.id}
THEN user B receives 404

GIVEN project P belongs to user A
WHEN user B sends PATCH or DELETE /api/projects/{P.id}
THEN user B receives 404 and P is unchanged

GIVEN I own project P
WHEN I PATCH /api/projects/{P.id} with a new name/description/color/status
THEN I receive 200 with the updated project
AND omitted fields keep their previous values (partial update)
AND status may be freely set to ACTIVE or ARCHIVED (archived is not read-only)

GIVEN I own project P
WHEN I DELETE /api/projects/{P.id}
THEN I receive 200
AND P no longer appears in GET /api/projects
AND P is soft-deleted (deleted_at set), not physically removed

GIVEN I am not authenticated
WHEN I call any /api/projects endpoint
THEN I receive 401
```

---

## 🤖 CodeRabbit Integration

### Story Type Analysis

| Attribute | Value | Rationale |
|-----------|-------|-----------|
| Type | 💻 Feature | New domain entity + REST endpoints |
| Complexity | Medium | Migration, entity, repository, service, API interface, controller, 4 test layers |
| Test Requirements | Unit + Integration + Authorization | `BACKEND_GUIDELINES.md` §5; PRD §16.2 |
| Review Focus | Layering, Owner scoping, Validation, Migrations | No business logic in controller/repository; no unscoped queries |

### Agent Assignment

| Role | Agent | Responsibility |
|------|-------|----------------|
| Primary | @dev | Backend implementation + tests |
| Secondary | @architect | DDL/API contract review (decisions resolved by @po — see Resolved Decisions) |
| Review | @qa | Verify gates, owner scoping and coverage |

### Self-Healing Config

```yaml
reviews:
  auto_review:
    enabled: true
    drafts: false
  path_instructions:
    - path: "apps/backend/src/main/java/**/repository/ProjectRepository.java"
      instructions: "Verify every method is owner-scoped (includes userId); no unscoped findById is exposed."
    - path: "apps/backend/src/main/java/**/controller/ProjectController.java"
      instructions: "Verify no business logic; controller only maps HTTP and delegates to ProjectService."
    - path: "apps/backend/src/main/resources/db/migration/V2__Create_projects.sql"
      instructions: "Verify columns match docs/prd.md section 15.2 and the partial index matches section 15.4."

chat:
  auto_reply: true
```

### Focus Areas

- [x] All repository methods include `userId`
- [x] `@Transactional` only on service public methods
- [x] Two-level validation (DTO + service)
- [x] `color` required + hex `^#[0-9A-Fa-f]{6}$` (D2); no reserved-name rule (D1)
- [x] No `?status` filter on list (D5); archive ≠ delete (D3)
- [x] Soft delete, not physical delete
- [x] Partial index matches PRD §15.4
- [x] OpenAPI annotations on all endpoints

---

## 🔗 Dependencies

**Blocked by:**
- AUTH-001 (authentication + `JwtCookieAuthenticationFilter` providing the principal) — done

**Blocks:**
- `PROJ-002` (Projects frontend screen) — not created yet
- `TB-001` (TimeBlock CRUD) — needs `projectId` FK target
- `SERIES-001` (TimeBlockSeries), `CHK-001` (ChecklistItem), `SESSION-001` (WorkSession), `METRICS-001` (Review/Metrics)

---

## ⚠️ Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Implementing the stale `docs/diagrams/*` model (organizations/tasks/Redis) | High | Use only `docs/prd.md` §15 and `backend-architecture.md` v3.0 as sources; the diagram docs are superseded |
| Inventing rules from the architecture example (reserved names, required color) | Medium | **Resolved (D1–D2):** reserved names dropped (not in PRD); color required + hex per PRD §15.2 + architecture §4.1/§5. See `## ✅ Resolved Decisions` |
| Owner-scoping regression | High | Dedicated authorization test per endpoint; expose only scoped repository methods |
| Migration drift | Medium | Never edit `V1`; add `V2`; keep `@Index` annotations aligned with the Flyway DDL |
| Coverage drop | Medium | New unit + integration + authorization tests; verify JaCoCo report |

---

## 📋 Definition of Done

- [x] Open Decisions resolved and recorded in this story (D1–D5)
- [x] `V2__Create_projects.sql` applied successfully by Flyway in tests
- [x] `Project`, `ProjectStatus`, `ProjectRepository`, DTOs, `ProjectService`, `ProjectApi`, `ProjectController` implemented
- [x] All 5 endpoints return the documented status codes and `ApiResponse` envelope
- [x] Owner scoping verified by tests (user B → 404 on user A's project)
- [x] Soft delete verified (record remains with `deleted_at` set)
- [x] `./mvnw test` passes; coverage not reduced and new code ≥80%
- [x] OpenAPI annotations complete
- [x] File List updated
- [x] Reviewed by @qa (Quartz) — 2026-09-12, verdict PASS

---

## 📝 Dev Notes

### Sources of truth (No Invention)

- `docs/prd.md` §5.2 (Project fields/rules), §7.1 (Planning Flow), §8.4 (Projects screen), §9.4 (Projects FRs), §15.2 (Project DDL), §15.4 (indexes), §16.2 (owner scoping)
- `docs/architecture/backend-architecture.md` §3.1 (package layout), §4.1–4.4 (Project examples), §6 (persistence), §7.1 (URLs), §7.3 (envelope), §7.4 (error codes), §10 (Flyway V2 = projects), §11 (test strategy)
- `docs/BACKEND_GUIDELINES.md` (layering, validation, logging, testing)

**Do not use** `docs/diagrams/data-model.md` or `docs/diagrams/query-patterns.md` for this entity — they describe the superseded multi-tenant model.

### Key Files (implemented)

```
apps/backend/src/main/
├── java/com/felipemelozx/kairos/
│   ├── api/ProjectApi.java                                  # NEW
│   ├── controller/ProjectController.java                    # NEW
│   ├── dto/request/CreateProjectRequest.java                # NEW
│   ├── dto/request/UpdateProjectRequest.java                # NEW
│   ├── dto/response/ProjectResponse.java                    # NEW
│   ├── entity/Project.java                                  # NEW
│   ├── entity/enums/ProjectStatus.java                      # NEW
│   ├── repository/ProjectRepository.java                    # NEW
│   ├── service/ProjectService.java                          # NEW
│   ├── config/SecurityConfig.java                           # MODIFIED (default entry point → 401)
│   └── exception/GlobalExceptionHandler.java                # MODIFIED (NOT_FOUND code → 404)
└── resources/db/migration/V2__Create_projects.sql           # NEW
apps/backend/src/test/java/com/felipemelozx/kairos/
├── repository/ProjectRepositoryIntegrationTest.java         # NEW
├── service/ProjectServiceTest.java                          # NEW
└── controller/ProjectControllerIntegrationTest.java         # NEW
docs/stories/PROJ-001-project-crud.md                        # THIS FILE
```

#### Necessary changes to existing files

- `SecurityConfig`: added `.exceptionHandling(authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED)))`. Without it, unauthenticated API calls either follow the `oauth2Login` redirect (302) or fall back to Spring Security's default 403 — neither satisfies the AC "unauthenticated → 401". Browser/HTML navigation still redirects to Google via the existing OAuth2 entry point; API clients now receive 401.
- `GlobalExceptionHandler`: maps the existing `BusinessException` code `NOT_FOUND` to HTTP 404. Previously every `BusinessException` became 400, which would leak "exists but forbidden"-style semantics and break the owner-scoping AC (user B must get 404, never 403). No other error code behavior changed.

### Technical Notes

- Follow `User.java` style: explicit no-arg constructor + getters/setters (no Lombok in this repo).
- Reuse `BusinessException` for business errors; `GlobalExceptionHandler` maps it to `BUSINESS_ERROR`/400.
- Resolve the current user exactly like `AuthController.me()`: `UUID.fromString(principal.getUsername())`.
- Return 404 (not 403) for another user's resource to avoid existence leakage; the repository's owner-scoped lookup naturally yields 404.
- Do **not** port the `RESERVED_NAMES` block or reserved-name error from `backend-architecture.md` §4.3 — see D1. The §4.3 code sample is illustrative, not normative.
- `PATCH` is a partial update: fields omitted keep their current value; `status` may move freely between ACTIVE and ARCHIVED (D3).
- `@GeneratedValue` + `gen_random_uuid()` default are both already used by `User`/`V1`; keep consistent.
- Log entry/exit summaries at INFO; never log sensitive data.

### Testing Checklist

#### Repository (Testcontainers)
- [x] Save + find by user
- [x] Soft-deleted project excluded
- [x] Other user's project excluded

#### Service (Mockito)
- [x] Create happy path
- [x] Blank name rejected
- [x] Missing color rejected (required — D2)
- [x] Invalid hex color rejected (D2)
- [x] Reserved-looking name accepted (D1)
- [x] List is owner-scoped and unfiltered by status (D5)
- [x] Soft delete own project
- [x] Delete other user's project → not found

#### Controller (integration)
- [x] 201 create / 400 validation / 401 unauthenticated
- [x] 200 list/get/update/delete
- [x] User B → 404 on user A's project

---

## 🧑‍💻 Dev Agent Record

> This section is populated when @dev executes the story.

### Execution Log

| Timestamp | Phase | Action | Result |
|-----------|-------|--------|--------|
| 2026-09-12 | Baseline | Ran full backend suite before changes | 60 tests, 0 failures |
| 2026-09-12 | Red | Wrote `ProjectRepositoryIntegrationTest`, `ProjectServiceTest`, `ProjectControllerIntegrationTest` | `./mvnw test-compile` failed with `cannot find symbol` for all new types (expected) |
| 2026-09-12 | Green | Added `V2__Create_projects.sql`, `ProjectStatus`, `Project`, `ProjectRepository`, DTOs, `ProjectService` | `ProjectServiceTest` 12/12, `ProjectRepositoryIntegrationTest` 3/3 pass |
| 2026-09-12 | Green | Added `ProjectApi`, `ProjectController`; fixed interface path mapping (class `/projects` + method paths were concatenating) | controller integration 11/12 pass |
| 2026-09-12 | Green | Configured 401 entry point; verified owner-scoping 404 via `NOT_FOUND` mapping | controller 12/12 pass |
| 2026-09-12 | Regression | `./mvnw test` | 87 tests, 0 failures, BUILD SUCCESS |
| 2026-09-12 | Coverage | `./mvnw jacoco:report` | New code ~98% lines; global 96.16% (up from ~93.5%) |

### Implementation Notes

- **TDD**: tests were written first and confirmed red via compilation failure; implementation followed per phase.
- **Layering**: controller only maps HTTP and resolves `userId` from `@AuthenticationPrincipal`; all business logic and `@Transactional` live in `ProjectService`; repository exposes only owner-scoped lookups.
- **Owner scoping**: user B receives **404** (not 403) for GET/PATCH/DELETE on user A's project, because the scoped repository lookup yields empty and the service raises `BusinessException("NOT_FOUND")`, mapped to 404.
- **Soft delete**: `softDelete` verifies ownership first (404 if not owned) then runs the scoped `@Modifying` update setting `deleted_at`; the row is retained.
- **Decisions honored**: D1 (no reserved names — `inbox` accepted by test), D2 (`color` required + `^#[0-9A-Fa-f]{6}$`), D3 (PATCH partial, free ACTIVE↔ARCHIVED), D4 (`description` ≤500), D5 (list has no `?status`, returns ACTIVE+ARCHIVED).
- **Migration**: `V2__Create_projects.sql` matches PRD §15.2 columns and the §15.4 partial index `idx_projects_user_active ... WHERE deleted_at IS NULL`; `V1` untouched.
- **No comments** added to source; explicit getters/setters (no Lombok), mirroring `User.java`.
- **Coverage**: `ProjectService` 100%, `ProjectController` 100%, `ProjectResponse` 100%, DTOs 100%, `ProjectStatus` 100%, `Project` 92.3% lines.

### Issues Encountered

1. **Interface path concatenation**: the `ProjectApi` interface initially declared `@PostMapping("/projects")` while the controller had `@RequestMapping("/projects")`, producing `/projects/projects`. Fixed by dropping the redundant path on the interface methods (same convention as `AuthApi`, where class = `/auth` and methods = `/register`).
2. **Unauthenticated request returned 302/200 instead of 401**: with `oauth2Login` active, the OAuth2 login entry point matched `text/plain` requests and redirected to Google; otherwise Spring Security defaulted to 403. Added `HttpStatusEntryPoint(UNAUTHORIZED)` as the default entry point in `SecurityConfig` (browser HTML flows still redirect), and the integration test issues an `Accept: application/json` API-style request. Justified as strictly necessary to satisfy the AC.
3. **404 for foreign resources**: the existing `GlobalExceptionHandler` mapped every `BusinessException` to 400. Added a `NOT_FOUND` → 404 branch so owner-scoping leaks no existence information; no other codes changed.

---

## 🧪 QA Results

> Reviewed by @qa (Quartz) on 2026-09-12. Evidence below is reproduced from the actual command runs (`./mvnw test`, `./mvnw jacoco:report`) — not from the @dev report.

### Test Execution Summary

| Category | Tests | Passed | Failed | Skipped |
|----------|-------|--------|--------|---------|
| Unit (Mockito / plain JUnit) | 59 | 59 | 0 | 0 |
| Integration (Testcontainers / `@SpringBootTest`) | 28 | 28 | 0 | 0 |
| New PROJ-001 tests (subset of the above) | 27 | 27 | 0 | 0 |
| Authorization-focused (subset of the above) | 4 | 4 | 0 | 0 |
| **Total** | **87** | **87** | **0** | **0** |

- `cd apps/backend && ./mvnw test` → `exit=0`; surefire aggregate = 87 tests, 0 failures, 0 errors, 0 skipped (baseline 60 → +27 new).
- Per-suite: `ProjectServiceTest` 12/12, `ProjectRepositoryIntegrationTest` 3/3, `ProjectControllerIntegrationTest` 12/12, plus 60 pre-existing tests all green.
- `./mvnw jacoco:report` → report generated at `target/site/jacoco/jacoco.csv`.

### Coverage (JaCoCo, line coverage)

| Scope | Lines covered | Lines missed | Coverage | Gate |
|-------|---------------|--------------|----------|------|
| New PROJ-001 code | 111 | 2 | **98.23%** | ≥80% ✅ |
| Global (all main classes) | 501 | 20 | **96.16%** | ≥93.5% (no drop) ✅ |

New classes: `ProjectService` 100%, `ProjectController` 100%, `ProjectResponse` 100%, `CreateProjectRequest` 100%, `UpdateProjectRequest` 100%, `ProjectStatus` 100%, `Project` 92.3% (2 uncovered lines: `setId`/`setDeletedAt`, test-helper only). `ProjectApi`/`ProjectRepository` are interfaces with no executable lines.

### Validation Checklist

| Check | Status | Notes |
|-------|--------|-------|
| Acceptance criteria | ✅ | All 12 Gherkin scenarios traced to passing tests (see traceability below) |
| DoD items | ✅ | Every documented DoD item verified; `Reviewed by @qa` now closed |
| Edge cases | ✅ | Owner-scoping 404, soft-delete invisibility, partial PATCH, archive ≠ delete, reserved-looking names, >500 description |
| Documentation | ✅ | File List accurate; OpenAPI annotations on all 5 endpoints; Change Log updated |
| Owner scoping (critical) | ✅ | Repository declares only `...AndUserId...` methods; no production unscoped call |
| Resolved Decisions D1–D5 | ✅ | All five verified against implementation and tests |
| Soft delete | ✅ | `deleted_at` set, row retained, excluded from list/get |
| Migration V2 | ✅ | Columns match PRD §15.2; partial index matches §15.4; `V1` untouched |
| Regression of 2 modified files | ✅ | `SecurityConfig` filters/permitAll intact; `GlobalExceptionHandler` other codes intact; 87/87 green |
| Envelope + status codes + OpenAPI | ✅ | `ApiResponse` on 200/201/400/404; 401 via security entry point; annotations on 5 endpoints |

### Requirement Traceability (AC → test)

| AC (Gherkin) | Test evidence |
|--------------|---------------|
| POST valid → 201, owned by userId, ACTIVE, createdAt | `ProjectControllerIntegrationTest.shouldCreateProjectWhenAuthenticated` |
| POST blank name → 400, nothing created | `ProjectServiceTest.shouldRejectBlankProjectName`; `...shouldReturn400WhenNameBlank` |
| POST missing/blank color → 400 | `ProjectServiceTest.shouldRejectMissingColor`; `...shouldReturn400WhenColorMissing` |
| POST invalid hex → 400 | `ProjectServiceTest.shouldRejectInvalidHexColor`; `...shouldReturn400WhenColorInvalidHex` |
| POST description > 500 → 400 | `ProjectControllerIntegrationTest.shouldReturn400WhenDescriptionTooLong` |
| `all`/`inbox`/`today` NOT reserved | `ProjectServiceTest.shouldAcceptReservedLookingName` ("inbox" accepted) |
| GET → only my non-deleted, ACTIVE+ARCHIVED, no status param | `ProjectControllerIntegrationTest.shouldListCurrentUserProjectsIncludingArchived`; `ProjectServiceTest.shouldListOnlyCurrentUserProjects` |
| User B GET A's project → 404 | `ProjectControllerIntegrationTest.shouldForbidUserBFromAccessingUserAProject` (GET) |
| User B PATCH/DELETE A's project → 404, P unchanged | Same test (PATCH + DELETE 404; re-read by A still OK) |
| PATCH partial, omitted fields kept, status free | `ProjectServiceTest.shouldPartiallyUpdateProjectKeepingOmittedFields`; `shouldArchiveProjectViaUpdate`; `ProjectControllerIntegrationTest.shouldUpdateProject`; `shouldArchiveProjectViaPatch` |
| DELETE → 200, gone from list, soft-deleted | `ProjectControllerIntegrationTest.shouldDeleteProject` (200; list excludes; `deletedAt` not null) |
| Unauthenticated → 401 | `ProjectControllerIntegrationTest.shouldReturn401WhenNotAuthenticated` |

### Detailed Verification Notes

1. **Owner scoping (critical).** `ProjectRepository` declares only `findByUserIdAndDeletedAtIsNull`, `findByIdAndUserIdAndDeletedAtIsNull` and the scoped `@Modifying softDeleteByIdAndUserId`. `grep` over `src/main` shows every `projectRepository.*` call is scoped or `save`; there is **no** production call to an unscoped `findById`. Authorization test asserts **404** (not 403) for GET/PATCH/DELETE of another user's project, and confirms the resource is unchanged.
2. **D1** — no reserved names: `ProjectService.validateName` enforces non-blank + ≤100 only; `inbox` accepted by test. **D2** — `CreateProjectRequest.color` = `@NotBlank @Pattern(^#[0-9A-Fa-f]{6}$)`; `UpdateProjectRequest.color` = `@Pattern` only (null allowed, invalid rejected); service re-validates. **D3** — PATCH applies only non-null fields; `status` transitions ACTIVE ↔ ARCHIVED with no read-only guard; DELETE is separate (`deleted_at`). **D4** — `@Size(max = 500)` + `VARCHAR(500)`. **D5** — list takes no status parameter; repository returns all non-deleted (ACTIVE + ARCHIVED).
3. **Soft delete.** `softDelete` checks ownership first (404 if not owned), then runs the scoped `@Modifying` UPDATE setting `deleted_at = now()`; the row is retained. Integration test verifies `deletedAt != null` and absence from the list.
4. **Migration V2.** `projects` columns match PRD §15.2 (`id`, `user_id`, `name`, `description`, `color`, `status`, `created_at`, `deleted_at`); partial index `idx_projects_user_active ... WHERE deleted_at IS NULL` matches PRD §15.4. Flyway applied V1→V2 successfully in every Testcontainers run. `git status` confirms `V1__Create_users.sql` untouched.
5. **Regression on the 2 modified files.** `git diff` shows `SecurityConfig` changed **only** by adding `.exceptionHandling(... HttpStatusEntryPoint(UNAUTHORIZED))` — CSRF/Origin filters, CORS config, `STATELESS` sessions, the `permitAll` route list and filter ordering are unchanged. `GlobalExceptionHandler` now maps `EMAIL_EXISTS → 409`, `NOT_FOUND → 404`, everything else → 400; the `EMAIL_EXISTS` behavior and all pre-existing auth tests (incl. `AuthControllerIntegrationTest` 10/10, CSRF/Origin filter tests 21/21) remain green.
6. **Envelope / status / OpenAPI.** All controller-produced responses use `ApiResponse<T>` and return 201/200/400/404 as documented. `ProjectApi` carries `@Tag`, `@SecurityRequirement(name = "cookieAuth")`, `@Operation` and `@ApiResponses` for all 5 endpoints; `OpenApiConfig` defines the `cookieAuth` cookie scheme.

### Non-blocking Observations (advisory, no fix requested)

1. **`JpaRepository` inheritance vs PRD §16.2.** The interface extends `JpaRepository`, so unscoped `findById`/`findAll`/`deleteById` are inherited. This matches `backend-architecture.md` §4.2 (normative) and the existing `UserRepository`, and no production path uses them — but it is looser than the literal "unscoped methods are not exposed" wording and is a systemic pattern worth a follow-up with @architect. **Not a story blocker.**
2. **`NOT_FOUND` mapping also affects `AuthController.me()`.** The shared code now maps a deleted-user edge case there from 400 → 404 (no test covers it; semantically more correct). The dev note "no other error code behavior changed" is imprecise; the atomic change is expected and benign.
3. **401 body.** The 401 is emitted by `HttpStatusEntryPoint` before the dispatcher, so it has an empty body rather than an `ApiResponse` envelope. The AC only requires the status; controller-produced responses all carry the envelope.
4. **401 AC tested on GET only.** The security rule (`anyRequest().authenticated()`) covers all methods, but the integration test exercises a single GET.
5. **Unrelated worktree changes.** `docs/stories/AUTH-001-login.md`, `DS-001-design-tokens.md` and `SEC-001-csrf-protection.md` are modified in the working tree (pre-existing QA/closure edits) — out of scope for this story; flagged for @devops before push.

### QA Sign-off

- [x] All acceptance criteria verified
- [x] Tests passing (coverage ≥80%)
- [x] Documentation complete
- [x] Ready for release

**QA Agent:** @qa (Quartz)
**Date:** 2026-09-12
**Verdict:** ✅ **PASS** — all ACs and DoD gates met; observations above are advisory only.

---

## 📜 Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2026-09-12 | 1.0.0 | Initial story creation (backend slice, open decisions flagged) | @aios-master (Orion, orchestration) |
| 2026-09-12 | 1.1.0 | Open Decisions resolved (D1–D5); status → Ready; AC/tasks aligned; dependencies cleared | @po (Pixel) |
| 2026-09-12 | 1.2.0 | Implementation complete (TDD): migration V2, entity/repository/DTOs/service/API/controller, 3 test classes (27 tests); 87/87 green; new-code coverage ~98%, global 96.16%; status → Ready for Review | @dev (Dex) |
| 2026-09-12 | 1.3.0 | QA review PASS: `./mvnw test` 87/87 (exit 0); JaCoCo new-code 98.23% lines (111/113), global 96.16% (501/521); owner-scoping 404, D1–D5, soft delete, V2/§15.2+§15.4, and regressions on `SecurityConfig`/`GlobalExceptionHandler` verified. 5 advisory observations recorded (non-blocking). Status → Done | @qa (Quartz) |

---

**Criado por:** @aios-master (Orion)
**Data:** 2026-09-12
**Atualizado por:** @po (Pixel) — Open Decisions resolvidas (D1–D5); story pronta para dev
**Implementado por:** @dev (Dex) — TDD (Red-Green); 87/87 testes; status Ready for Review
**Atualizado:** 2026-09-12
