# Story TB-001: TimeBlock CRUD (Backend Slice)

**Epic:** Planning & Calendar (MVP)
**Story ID:** TB-001
**Sprint:** 3
**Priority:** 🔴 Critical (blocks Calendar §9.1, ChecklistItem CHK-001, WorkSession SESSION-001, Review metrics)
**Points:** 5
**Effort:** 6-8 hours
**Status:** ✅ Done (2026-09-23) — full `./mvnw test` green: 126 tests, 0 failures; JaCoCo LINE 91.5% global, TimeBlock classes 78–100%.
**Type:** 💻 Feature (backend only)

---

## 🔀 Cross-Story Decisions

| Decision | Source | Impact on This Story |
|----------|--------|----------------------|
| Single-user, owner-scoped. Every table carries `user_id`; every query filters by the authenticated user | `docs/prd.md` §4.7, §15.1, §16.2; `backend-architecture.md` ADR-002 | `TimeBlockRepository` exposes only `...AndUserId...` methods; no unscoped `findById` in production paths |
| Soft delete (`deletedAt`), never physical delete for `time_blocks` | `docs/prd.md` §5.3, §15.2 (deletedAt); arch §6.1 | Repository filters `...AndDeletedAtIsNull`; delete sets `deleted_at` |
| Layered architecture: controller → service → repository → entity. Business logic only in service; `@Transactional` only on service methods | `docs/BACKEND_GUIDELINES.md` §2; `backend-architecture.md` §2 | Follow `ProjectService`/`ProjectController` structure (Result pattern) |
| HTTP DTOs are records validated with Bean Validation (structural) + service validation (business rules) | `backend-architecture.md` §5; `BACKEND_GUIDELINES.md` §3.4 | Two-level validation for create/update |
| Controllers implement a `*Api` interface that carries the OpenAPI annotations | `com.felipemelozx.kairos.api.ProjectApi` + `ProjectController` | New `TimeBlockApi` interface + `TimeBlockController` |
| Context path is `/api`; controllers map without the `/api` prefix | `application.yml`; `ProjectController` maps `"/projects"` | `TimeBlockController` maps `"/time-blocks"` → public URL `/api/time-blocks` |
| Response envelope is `ApiResponse<T>`; errors via `HttpErrorMapper` (Result pattern) | `dto/response/ApiResponse.java`, `controller/HttpErrorMapper.java` | Reuse `Result.ok/err`, `ErrorCode.NOT_FOUND` → 404 |
| URL versioning is header-based (`X-API-Version`), not in the URL path | `backend-architecture.md` ADR-004, §7.2 | Endpoints stay clean; versioning out of scope |
| Migration numbering: V1 users, V2 projects, V3 token_version → next is **V4 time_blocks** | `apps/backend/src/main/resources/db/migration/`, arch §10 | New `V4__Create_time_blocks.sql`; never edit V1–V3 |

---

## 📋 User Story

**Como** usuário,
**Quero** criar, listar, editar e excluir meus blocos de tempo (intenções na agenda, opcionais por projeto),
**Para** planejar meu dia/semana no calendário e depois executar com o timer e medir progresso real.

---

## 🎯 Objective

Deliver the second MVP domain entity: **TimeBlock** (PRD §5.3). This is the calendar atom referenced by `ChecklistItem.timeBlockId` (§5.6) and `WorkSession.timeBlockId` (§5.7), and rendered by the Calendar screen (§8.1/§9.1).

Scope is **backend only** (CLI First). Calendar UI, series materialization (SERIES-001), checklists (CHK-001) and timer/sessions (SESSION-001) are separate stories.

---

## ✅ Resolved Decisions (@po, Art. IV — no invention)

> Each decision traces to a normative source, or is an explicit **@po product decision** where the PRD is silent.

### D1 — Overlap: **ALLOWED (no overlap rejection)**

**Decision:** Overlapping time blocks are **allowed**. The service does NOT reject `POST/PATCH` whose range overlaps another block.

**Rationale:** PRD never requires overlap rejection. PRD §6 explicitly says "Multiple blocks per day: each block runs its own independent timer → separate work sessions". Rejecting overlaps would invent a business rule.

**Source:** `docs/prd.md` §5.3 (rules list has no overlap rule), §6. @po product decision (PRD silent → allow).

**Impact:** No overlap query in service; no `OVERLAP` error code.

### D2 — `title`: **REQUIRED, 1–200 chars**

**Decision:** `title` required on create (`@NotBlank @Size(max=200)`); optional on PATCH but when present non-blank and ≤200.

**Rationale:** PRD §15.2 types `title: String` with no length; legacy `data-model.md` (superseded for tenancy, but the only length source) defines `time_blocks.title VARCHAR(200)`, consistent with `tasks.title VARCHAR(200)`. Adopt 200 rather than inventing another bound.

**Source:** `docs/prd.md` §5.3, §15.2; `docs/diagrams/data-model.md` time_blocks (length only — tenancy columns ignored). @po decision on length bound.

**Impact:** New `ErrorCode.INVALID_TIME_BLOCK_TITLE` → 400 via `HttpErrorMapper`.

### D3 — `endDateTime > startDateTime`: **REQUIRED (400)**

**Decision:** `endDateTime` must be strictly after `startDateTime` on create and whenever either is patched. Violations → 400.

**Rationale:** PRD §5.3 rules + §15.3 rule 1 + legacy CHECK constraint.

**Source:** `docs/prd.md` §5.3, §15.3.1; `backend-architecture.md` §3.3 (`TimeBlockService.create` enforces).

**Impact:** DB `CHECK (end_datetime > start_datetime)` + service validation. New `ErrorCode.INVALID_TIME_RANGE` → 400.

### D4 — `projectId`: **OPTIONAL, must be owned + non-deleted when present**

**Decision:** `projectId` optional. When present, the referenced project must exist, belong to the user and be non-deleted (ARCHIVED counts as valid — consistent with PROJ-001 D3). Otherwise → 404 `NOT_FOUND` (no existence leak, same convention as owner-scoping).

**Rationale:** PRD §5.3: `projectId (optional)`; §15.3.5 owner scoping; PROJ-001 D3 archived ≠ read-only.

**Source:** `docs/prd.md` §5.3, §15.2, §15.3.5.

**Impact:** `TimeBlockService` injects `ProjectRepository` for ownership check. No new error code (reuse `NOT_FOUND`).

### D5 — `seriesId` / `isOverride`: **server-managed, not patchable in this slice**

**Decision:** `seriesId` accepted on create as nullable UUID (no FK — `time_block_series` table does not exist yet; enforced when SERIES-001 lands). `isOverride` defaults `false` on create. `PATCH` does **not** accept `seriesId`/`isOverride` — they are managed by the future series engine (PRD §5.4, §10 rules 3–4).

**Rationale:** PRD §5.3 defines the fields; §5.4/§10 assign their lifecycle to series split/override logic (SERIES-001 scope). Letting clients freely flip them now would pre-empt that design.

**Source:** `docs/prd.md` §5.3, §5.4, §10. @po decision (PRD silent on standalone-edit API).

**Impact:** `CreateTimeBlockRequest` has `seriesId` (nullable); `UpdateTimeBlockRequest` has only `title/startDateTime/endDateTime/projectId`.

### D6 — List: **`GET /api/time-blocks` with optional `?from&?to` range filter**

**Decision:** List returns all non-deleted blocks of the user ordered by `startDateTime`. Optional `from`/`to` (ISO-8601 instants) filter `start_datetime >= from AND start_datetime < to`. Both or neither; `from >= to` → 400.

**Rationale:** PRD §9.1 calendar views (Today/Week/Month) + §15.4 calendar index query pattern. Minimal backward-compatible filter; clients can filter locally otherwise.

**Source:** `docs/prd.md` §9.1, §15.4; `backend-architecture.md` §7.1 (`GET /api/time-blocks`). @po decision on query params (PRD silent on exact params).

**Impact:** Repository `findByUserIdAndDeletedAtIsNullOrderByStartDateTimeAsc` + `findByUserIdAndStartDateTimeGreaterThanEqualAndStartDateTimeLessThanAndDeletedAtIsNullOrderByStartDateTimeAsc`. New `ErrorCode.INVALID_TIME_RANGE` reused for bad range.

---

## ✅ Tasks

### Phase 1: Red — Tests First (TDD)

- [x] **1.1** `TimeBlockRepositoryIntegrationTest` (Testcontainers, `@DataJpaTest`):
  - `shouldSaveAndFindBlockByUser`
  - `shouldNotFindSoftDeletedBlock`
  - `shouldNotFindBlockOfAnotherUser`
  - `shouldListBlocksInRangeOrdered`
- [x] **1.2** `TimeBlockServiceTest` (Mockito, no Spring context):
  - `shouldCreateBlockForCurrentUser` (standalone, no project)
  - `shouldCreateBlockWithOwnedProject`
  - `shouldRejectBlankTitle`
  - `shouldRejectEndBeforeStart` (D3)
  - `shouldRejectUnknownProject` (D4 → NOT_FOUND)
  - `shouldRejectForeignProject` (D4 → NOT_FOUND)
  - `shouldAllowOverlappingBlocks` (D1)
  - `shouldListOnlyCurrentUserBlocks`
  - `shouldPartiallyUpdateBlock`
  - `shouldSoftDeleteOwnBlock`
  - `shouldThrowNotFoundWhenDeletingOtherUsersBlock`
- [x] **1.3** `TimeBlockControllerIntegrationTest` (TestRestTemplate + Testcontainers):
  - `shouldCreateBlockWhenAuthenticated` → 201
  - `shouldReturn401WhenNotAuthenticated` → 401
  - `shouldReturn400WhenEndBeforeStart` → 400
  - `shouldReturn400WhenTitleBlank` → 400
  - `shouldReturn404WhenProjectNotOwned` → 404
  - `shouldListBlocks` → 200
  - `shouldListBlocksFilteredByRange` → 200
  - `shouldGetSingleBlock` → 200
  - `shouldUpdateBlock` → 200
  - `shouldDeleteBlock` → 200 and disappears from list
- [x] **1.4** Authorization test: `shouldForbidUserBFromAccessingUserABlock` (GET/PATCH/DELETE → 404)
- [x] Run tests → all fail (no entity, no table, no endpoint) — confirmed: `test-compile` failed with `cannot find symbol` for all new TimeBlock types (2026-09-23)

### Phase 2: Green — Migration + Entity + Repository

- [x] **2.1** Migration `V4__Create_time_blocks.sql` — columns from PRD §15.2; index from §15.4; CHECK from §15.3.1. `series_id UUID NULL` (no FK until SERIES-001); `project_id UUID NULL REFERENCES projects(id)`:
  ```sql
  CREATE TABLE time_blocks (
    ...,
    CONSTRAINT chk_time_blocks_range CHECK (end_datetime > start_datetime)
  );
  CREATE INDEX idx_timeblocks_user_range ON time_blocks(user_id, start_datetime, end_datetime)
  WHERE deleted_at IS NULL;
  ```
- [x] **2.2** `entity/TimeBlock.java` — JPA entity, explicit getters/setters, no Lombok, no business logic
- [x] **2.3** `repository/TimeBlockRepository.java` — owner-scoped only (+ range query, ordered)
- [x] Run tests → repository tests pass (verified in Docker full run, 2026-09-23)

### Phase 3: Green — DTOs + Service

- [x] **3.1** `dto/request/CreateTimeBlockRequest.java` (record + Bean Validation)
- [x] **3.2** `dto/request/UpdateTimeBlockRequest.java` (record, partial update; no seriesId/isOverride — D5)
- [x] **3.3** `dto/response/TimeBlockResponse.java` (record + `from(TimeBlock)`)
- [x] **3.4** `common/ErrorCode.java` += `INVALID_TIME_BLOCK_TITLE`, `INVALID_TIME_RANGE`; `HttpErrorMapper` maps both → 400
- [x] **3.5** `service/TimeBlockService.java` — `@Transactional` on public methods; owner scoping; D1–D6; SLF4J entry/exit logs:
  - `create`, `listByUser(userId, from, to)`, `getById`, `update` (partial), `softDelete`
- [x] Run tests → service tests pass (`TimeBlockServiceTest` 11/11 green, 2026-09-23)

### Phase 4: Green — API + Controller

- [x] **4.1** `api/TimeBlockApi.java` — interface with `@Tag`, `@Operation`, `@ApiResponses`, `@SecurityRequirement(name = "cookieAuth")`
- [x] **4.2** `controller/TimeBlockController.java` — `@RequestMapping("/time-blocks")`, implements `TimeBlockApi`
- [x] **4.3** Endpoints (public URLs, context-path `/api`):
  - `POST /api/time-blocks` → 201
  - `GET /api/time-blocks[?from&to]` → 200 (ordered; D6)
  - `GET /api/time-blocks/{blockId}` → 200
  - `PATCH /api/time-blocks/{blockId}` → 200 (partial; D5)
  - `DELETE /api/time-blocks/{blockId}` → 200 (soft delete)
- [x] Run tests → all green (full suite with Docker, 2026-09-23 — see Dev Notes)

### Phase 5: Quality

- [x] **5.1** Full `./mvnw test` with Docker GREEN (2026-09-23): 126 tests, 0 failures, BUILD SUCCESS (was: unit-only 72/72 while Docker was unavailable).
- [x] **5.2** `./mvnw jacoco:report` — LINE 91.5% global; TimeBlock classes 78–100% (controller 78.9%, service 89.1%, entity 93.8%, DTOs 100%).
- [x] **5.3** OpenAPI annotations complete for all endpoints (`TimeBlockApi`: 5 operations with @ApiResponses)
- [x] **5.4** Story DoD checklist + File List updated (this edit)

## 🧑‍💻 Dev Agent Record (@dev perspective, executed by @aios-master — subagent depth limit)

### Execution Log

| Timestamp | Phase | Action | Result |
|-----------|-------|--------|--------|
| 2026-09-23 | Red | Wrote `TimeBlockRepositoryIntegrationTest`, `TimeBlockServiceTest`, `TimeBlockControllerIntegrationTest` | `test-compile` failed with `cannot find symbol` for all new TimeBlock types (expected) |
| 2026-09-23 | Green | Added `V4__Create_time_blocks.sql`, `TimeBlock`, `TimeBlockRepository`, DTOs, `ErrorCode`+`HttpErrorMapper` codes, `TimeBlockService` | `TimeBlockServiceTest` 11/11 pass |
| 2026-09-23 | Green | Added `TimeBlockApi`, `TimeBlockController` (from/to range guard → 400, else delegate) | main+test compile green |
| 2026-09-23 | QA-unit | `./mvnw test` unit-only (8 suites) | 72 tests, 0 failures, BUILD SUCCESS |
| 2026-09-23 | QA-full | `./mvnw test` full | 79 run, 0 failures, 7 errors — all `IllegalState: Could not find a valid Docker environment` (dockerd needs root; no sudo in this env). Pre-existing IT suites fail identically → environmental blocker, not regression |
| 2026-09-23 | QA-cov | `./mvnw jacoco:report` (unit-only) | new code 63.79%, global 85.30% — IT-dependent lines uncovered; meaningful gate needs Docker run |

### Issues Encountered

1. **Nested subagent dispatch blocked**: Task tool refused (`Subagent depth limit reached (1)`) because this Orion session itself runs as a subagent. All agent perspectives (@sm/@po story, @architect contract, @dev TDD, @qa gates) were executed inline by @aios-master, recorded per-role in this story.
2. **No Docker daemon**: `dockerd` requires root; environment has no sudo. All 7 Testcontainers suites (4 pre-existing + 3 new) cannot execute here. V4 migration SQL therefore validated by review only (mirrors V2 style, CHECK + partial index per PRD §15.3.1/§15.4) — Flyway application MUST be confirmed in the Docker run.

---

## 🎯 Acceptance Criteria

```gherkin
GIVEN I am authenticated
WHEN I POST /api/time-blocks with title + valid start/end (no project)
THEN 201 with the block owned by my userId, isOverride=false, createdAt set

GIVEN I am authenticated and own project P
WHEN I POST /api/time-blocks with projectId=P
THEN 201 with projectId=P

GIVEN I am authenticated
WHEN I POST /api/time-blocks with blank title
THEN 400 and nothing created

GIVEN I am authenticated
WHEN I POST /api/time-blocks with endDateTime <= startDateTime
THEN 400 and nothing created

GIVEN I am authenticated
WHEN I POST /api/time-blocks with projectId unknown or owned by another user
THEN 404 and nothing created

GIVEN I own block B (any range)
WHEN I POST /api/time-blocks overlapping B
THEN 201 (overlaps allowed — D1)

GIVEN I am authenticated with blocks
WHEN I GET /api/time-blocks
THEN 200 with only my non-deleted blocks ordered by startDateTime

GIVEN I am authenticated with blocks inside/outside [from,to)
WHEN I GET /api/time-blocks?from=..&to=..
THEN 200 with only my blocks in range

GIVEN block B belongs to user A
WHEN user B requests GET/PATCH/DELETE /api/time-blocks/{B.id}
THEN 404 and B unchanged

GIVEN I own block B
WHEN I PATCH /api/time-blocks/{B.id} with new title/start/end/projectId
THEN 200 with updated block; omitted fields kept

GIVEN I own block B
WHEN I DELETE /api/time-blocks/{B.id}
THEN 200, B gone from list, row retained with deleted_at set

GIVEN I am not authenticated
WHEN I call any /api/time-blocks endpoint
THEN 401
```

---

## 🤖 CodeRabbit Integration

| Role | Agent | Responsibility |
|------|-------|----------------|
| Primary | @dev | Backend implementation + tests (TDD) |
| Secondary | @architect | DDL/API contract review (D1–D6 resolved by @po) |
| Review | @qa | Verify gates, owner scoping and coverage |

Focus: owner-scoped repo methods only; `@Transactional` on service only; two-level validation; CHECK end>start; no FK on series_id yet; soft delete; range index matches PRD §15.4; OpenAPI on all endpoints.

---

## 🔗 Dependencies

**Blocked by:** PROJ-001 (projectId FK target) — done
**Blocks:** SERIES-001 (seriesId FK + materialization), CHK-001 (ChecklistItem.timeBlockId), SESSION-001 (WorkSession.timeBlockId), Calendar/Review UI

---

## ⚠️ Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Inventing overlap rule | Medium | D1 resolved: allowed, per PRD §6 |
| series_id FK to nonexistent table | High | D5: plain UUID column, no FK; SERIES-001 adds constraint |
| projectId leak (existence oracle) | High | D4: 404 for unknown/foreign project; auth test per endpoint |
| Migration drift | Medium | Never edit V1–V3; new V4 only; `@Table` indexes aligned |
| Coverage drop | Medium | Unit + integration + authorization tests; JaCoCo gate |

---

## 📋 Definition of Done

- [x] Open Decisions resolved and recorded (D1–D6)
- [x] `V4__Create_time_blocks.sql` applied by Flyway in tests (verified 2026-09-23 Docker run)
- [x] `TimeBlock`, `TimeBlockRepository`, DTOs, `TimeBlockService`, `TimeBlockApi`, `TimeBlockController` implemented
- [x] All endpoints return documented status codes with `ApiResponse` envelope (verified by ITs)
- [x] Owner scoping verified (user B → 404) (verified by ITs)
- [x] Soft delete verified (verified by ITs)
- [x] Unit tests pass; full `./mvnw test` green (122 tests, 0 failures, 2026-09-23); JaCoCo LINE 91.0% global
- [x] OpenAPI annotations complete
- [x] Bruno collection updated (`bruno/time-blocks/` 6 requests + `README.md` + `csrfToken` env var)
- [x] File List updated
- [x] Reviewed by @qa (Quartz) — full gates PASS (Docker run 2026-09-23)

---

## 📝 Dev Notes

### Sources of truth (No Invention)

- `docs/prd.md` §5.3 (fields/rules), §6 (timer/multi-block), §9.1 (calendar), §10 (recurrence — SERIES scope), §15.2/15.3/15.4 (DDL/rules/indexes), §16.2 (owner scoping)
- `docs/architecture/backend-architecture.md` §2–§7 (layering/validation/envelope), §10 (V4 = time_blocks), ADR-002
- `docs/BACKEND_GUIDELINES.md` (layering, validation, logging, testing)
- `docs/stories/PROJ-001-project-crud.md` (Result pattern, 404-owner-scoping, 401 entry point, test layout)

### File List (to update during implementation)

```
apps/backend/src/main/
├── resources/db/migration/V4__Create_time_blocks.sql   # NEW
├── java/com/felipemelozx/kairos/
│   ├── entity/TimeBlock.java                           # NEW
│   ├── repository/TimeBlockRepository.java             # NEW
│   ├── dto/request/CreateTimeBlockRequest.java         # NEW
│   ├── dto/request/UpdateTimeBlockRequest.java         # NEW
│   ├── dto/response/TimeBlockResponse.java             # NEW
│   ├── service/TimeBlockService.java                   # NEW
│   ├── api/TimeBlockApi.java                           # NEW
│   ├── controller/TimeBlockController.java             # NEW
│   └── common/ErrorCode.java                           # MODIFIED (+2 codes)
│   └── controller/HttpErrorMapper.java                 # MODIFIED (map 2 codes → 400)
apps/backend/src/test/java/com/felipemelozx/kairos/
 │   ├── repository/TimeBlockRepositoryIntegrationTest.java  # NEW
 │   ├── service/TimeBlockServiceTest.java                   # NEW
 │   └── controller/TimeBlockControllerIntegrationTest.java  # NEW
 apps/backend/bruno/
 │   ├── time-blocks/create.bru                              # NEW
 │   ├── time-blocks/list.bru                                # NEW
 │   ├── time-blocks/list-range.bru                          # NEW
 │   ├── time-blocks/get.bru                                 # NEW
 │   ├── time-blocks/update.bru                              # NEW
 │   ├── time-blocks/delete.bru                              # NEW
 │   ├── environments/dev.bru                                # MODIFIED (+csrfToken)
 │   └── README.md                                           # MODIFIED (Time Blocks section)
 docs/stories/TB-001-timeblock-crud.md                  # THIS FILE
```

---

## 📜 Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2026-09-23 | 1.0.0 | Story created (backend slice, D1–D6 resolved by @po) | @aios-master (Orion, orchestrating as @sm/@po — subagent depth limit prevented nested dispatch) |
| 2026-09-23 | 1.1.0 | Implementation complete (TDD Red→Green): V4, entity/repo/DTOs/service/API/controller, 3 test classes (26 tests); unit 72/72 green; ITs pending Docker; status → In Progress | @aios-master (Orion, executing as @dev — subagent depth limit) |
| 2026-09-23 | 1.2.0 | Bruno collection updated: `bruno/time-blocks/` (6 requests) + `README.md` Time Blocks section + `csrfToken` env var (standing rule: endpoints sempre acompanham Bruno); AGENTS.md rule 5 registrada | Dex (@dev) |
| 2026-09-23 | 1.3.0 | Full `./mvnw test` with Docker GREEN (122 tests, 0 failures, BUILD SUCCESS); JaCoCo LINE 91.0% global (TimeBlock 72–100%); DoD all checked; status → Done | Quartz (@qa) |
| 2026-09-23 | 1.4.0 | CodeRabbit PR #10 review addressed (4/4): seriesId + range docs fixed, range validation moved controller→service (Result), story Phase 2.1/5 entries aligned; +4 service tests (126 total green); controller coverage 72.1→78.9% (Sonar new-code gate); logout.bru + CSRF header (Bruno 403 fix) | Dex (@dev) |

---

**Criado por:** @aios-master (Orion, atuando como @sm/@po)
**Data:** 2026-09-23
