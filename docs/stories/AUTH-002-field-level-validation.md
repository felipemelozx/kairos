# Story AUTH-002: Client-Side Field-Level Validation for Email Login & Register

**Epic:** Authentication & Authorization
**Story ID:** AUTH-002
**Sprint:** 1
**Priority:** 🟠 High
**Points:** 5
**Effort:** 4-6 hours
**Status:** ✅ Done
**Type:** 💻 Feature

---

## 🔀 Cross-Story Decisions

| Decision | Source | Impact on This Story |
|----------|--------|----------------------|
| Email is validated with `@NotBlank @Email`; register password with `@NotBlank @Size(min = 8)`; name with `@NotBlank @Size(max = 100)`; login password is `@NotBlank` only | `RegisterRequest.java`, `LoginRequest.java`, AUTH-001 | Client validation must mirror these rules exactly; no extra password complexity rules are added |
| UI language is English | Existing `LoginForm.tsx` / `RegisterForm.tsx` / `page.tsx` | Microcopy stays English; no i18n framework is introduced |
| API errors can carry per-field `details: Record<string, string>` | `apps/frontend/src/lib/api.ts`, `api.test.ts` | Server field errors should be mapped to the matching inline field when present |
| Absolute imports use `@/` | Constitution Article VI | New helper imported as `@/lib/auth-validation` |

---

## 📋 User Story

**Como** usuário,
**Quero** receber feedback de erro por campo, no cliente, antes de enviar o formulário de login/registro,
**Para** corrigir entradas inválidas rapidamente, entender o que está errado em cada campo e não disparar requisições desnecessárias à API.

---

## 🎯 Objective

Improve the error experience of the email Login and Register flows by validating all fields on the client before calling the auth store, rendering inline per-field errors with accessible wiring, moving focus to the first invalid field, and clearing a field error as soon as the user corrects it. Validation rules are derived from the backend request DTOs — no new business rules are invented.

---

## ✅ Tasks

### Phase 1: Reusable Validation Helper (1-2h)

- [x] **1.1** Create `apps/frontend/src/lib/auth-validation.ts`
  - Export `EMAIL_PATTERN`, `PASSWORD_MIN_LENGTH = 8`, `NAME_MAX_LENGTH = 100`
  - Export `validateLogin({ email, password })` returning per-field errors
  - Export `validateRegister({ name, email, password })` returning per-field errors
  - Export `getFirstInvalidField(errors, order)` and `pickFieldErrors(details, fields)`
- [x] **1.2** Create `apps/frontend/src/lib/auth-validation.test.ts` covering every rule and edge case

### Phase 2: LoginForm (1-2h)

- [x] **2.1** Replace single generic `error` state with `fieldErrors` + `formError`
- [x] **2.2** Validate on submit; when invalid, set errors, focus first invalid field and DO NOT call `login`
- [x] **2.3** Render inline error per field with `aria-invalid` / `aria-describedby`
- [x] **2.4** Clear a field error when the user edits that field
- [x] **2.5** Map `ApiError.details` to inline field errors; fall back to the form-level error
- [x] **2.6** Apply danger border/focus styles per DESIGN.md error state
- [x] **2.7** Expand `LoginForm.test.tsx`

### Phase 3: RegisterForm (1-2h)

- [x] **3.1** Mirror the LoginForm approach for `name`, `email`, `password`
- [x] **3.2** Enforce `name` required and ≤ 100 chars, `email` required + format, `password` required + ≥ 8 chars
- [x] **3.3** Clear field errors on edit; focus first invalid on submit
- [x] **3.4** Map `ApiError.details` to inline field errors; fall back to the form-level error
- [x] **3.5** Expand `RegisterForm.test.tsx`

### Phase 4: Quality (1h)

- [x] **4.1** Add `typecheck` alias script so the AGENTS.md gate command exists
- [x] **4.2** Run `cd apps/frontend && npm run lint && npm run typecheck && npm test`
- [x] **4.3** Self-review accessibility (aria wiring, focus, contrast) against DESIGN.md

---

## 🎯 Acceptance Criteria

```gherkin
GIVEN I am on the email Login form
WHEN I submit with an empty email or password
THEN I see an inline error under each empty field
AND focus moves to the first invalid field (email)
AND the auth store `login` is NOT called

GIVEN I am on the email Login form
WHEN I submit with a malformed email and a filled password
THEN I see "Enter a valid email address." under the email field
AND the auth store `login` is NOT called

GIVEN I am on the email Login form
WHEN I submit with a valid email and a password of any length
THEN the auth store `login` IS called with the credentials

GIVEN I am on the Register form
WHEN I submit with empty name, email and password
THEN I see inline errors under all three fields
AND focus moves to the name field
AND the auth store `register` is NOT called

GIVEN I am on the Register form
WHEN I submit a password shorter than 8 characters
THEN I see "Password must be at least 8 characters." under the password field
AND the auth store `register` is NOT called

GIVEN I am on the Register form
WHEN I submit a name longer than 100 characters
THEN I see a name length error
AND the auth store `register` is NOT called

GIVEN a field currently shows an inline error
WHEN I edit that field
THEN its inline error is cleared immediately
AND `aria-invalid` becomes false for that field

GIVEN client validation passes but the API returns an error with per-field `details`
WHEN the request fails
THEN the matching inline field errors are shown
AND focus moves to the first server-invalid field

GIVEN client validation passes but the API returns an error without `details`
WHEN the request fails
THEN a form-level `role="alert"` message is shown with the API message

GIVEN either form renders an invalid field
WHEN inspected by assistive technology
THEN the input has `aria-invalid="true"` and `aria-describedby` pointing at its error message with `role="alert"`
```

---

## 🤖 CodeRabbit Integration

### Story Type Analysis

| Attribute | Value | Rationale |
|-----------|-------|-----------|
| Type | 💻 Feature | User-facing validation behavior and UI states |
| Complexity | Medium | Two forms, reusable helper, accessibility and server-error mapping |
| Test Requirements | Unit | Helper logic + component behavior with Testing Library |
| Review Focus | Logic, Accessibility, Requirements alignment | Rule parity with backend DTOs; aria/focus correctness |

### Agent Assignment

| Role | Agent | Responsibility |
|------|-------|----------------|
| Primary | @dev | Implement helper, forms and tests |
| Secondary | @ux-design-expert | Error hierarchy, microcopy, accessibility review |
| Review | @qa | Verify gates, edge cases and a11y |

### Self-Healing Config

```yaml
reviews:
  auto_review:
    enabled: true
    drafts: false
  path_instructions:
    - path: "apps/frontend/src/components/auth/**"
      instructions: "Verify aria-invalid/aria-describedby wiring, focus management and that the auth store is not called on client validation failure."

chat:
  auto_reply: true
```

### Focus Areas

- [x] Validation rules match backend DTOs exactly (no invented rules)
- [x] No `login`/`register` call when client validation fails
- [x] `aria-invalid`, `aria-describedby`, `role="alert"` and focus-to-first-invalid
- [x] Errors clear when the user corrects the field

---

## 🔗 Dependencies

**Blocked by:**
- AUTH-001 frontend forms — done, existing files are the starting point

**Blocks:**
- None

---

## ⚠️ Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Microcopy diverges between Login and Register | Medium | Shared helper + documented strings; review by @ux-design-expert |
| Existing tests break due to state refactor | Medium | Keep behavior/strings used by current tests and expand them |
| Over-validation (e.g., rejecting valid emails) | Low | Same permissive pattern already in use; mirror backend `@Email` intent |
| Announcement duplication from multiple `role="alert"` nodes | Low | Errors only render when present; verify with @qa |

---

## 📋 Definition of Done

- [x] `auth-validation.ts` implemented and unit tested
- [x] LoginForm and RegisterForm use per-field errors + accessibility wiring
- [x] Auth store not called on client validation failure
- [x] Field errors clear on edit
- [x] Existing tests pass and new tests cover empty, malformed, short password, name length, no-API-call, error clearing and happy path
- [x] All acceptance criteria verified
- [x] `npm run lint && npm run typecheck && npm test` pass
- [x] Documentation updated (this story + UX spec)
- [x] Reviewed by @qa

---

## 📝 Dev Notes

### Key Files

```
apps/frontend/src/
├── lib/
│   ├── auth-validation.ts          # NEW reusable validators
│   ├── auth-validation.test.ts     # NEW
│   └── api.ts                      # ApiError.details source
└── components/auth/
    ├── LoginForm.tsx               # per-field errors
    ├── LoginForm.test.tsx          # expanded
    ├── RegisterForm.tsx            # per-field errors
    └── RegisterForm.test.tsx       # expanded
docs/
├── stories/AUTH-002-field-level-validation.md
└── design/AUTH-002-error-ux.md     # UX/microcopy/a11y spec
```

### Technical Notes

- Rules derived strictly from `RegisterRequest.java` and `LoginRequest.java`.
- `noValidate` stays on both forms; custom validation is the single source of truth.
- Import `ApiError` from `@/lib/api` to read `details`.
- Package `apps/frontend/package.json` only defines `type-check`; add a `typecheck` alias to satisfy the documented AGENTS.md gate.
- No comments in code (repo convention).
- UI language stays English.

### Testing Checklist

#### Helper
- [x] Login: empty email, empty password, invalid email, valid
- [x] Register: empty name/email/password, invalid email, password < 8, name > 100, valid
- [x] `getFirstInvalidField` respects field order
- [x] `pickFieldErrors` selects known fields only

#### Components
- [x] Empty submit shows inline errors and does not call the store
- [x] Invalid email shows inline email error and does not call the store
- [x] Short register password shows inline error and does not call the store
- [x] Name > 100 shows inline error and does not call the store
- [x] Focus moves to first invalid field
- [x] `aria-invalid` / `aria-describedby` wired correctly
- [x] Editing a field clears its error
- [x] API `details` map to inline errors
- [x] API error without `details` renders form-level alert
- [x] Happy path calls the store with the form data

---

## 🧑‍💻 Dev Agent Record

> This section is populated when @dev executes the story.

### Execution Log

| Timestamp | Phase | Action | Result |
|-----------|-------|--------|--------|
| 2026-09-11 | 1 | Created `auth-validation.ts` + 24 unit tests (rules derived from backend DTOs) | Done |
| 2026-09-11 | 2 | Refactored `LoginForm.tsx` to per-field errors + a11y + focus + API details mapping | Done |
| 2026-09-11 | 3 | Refactored `RegisterForm.tsx` with name/email/password validation | Done |
| 2026-09-11 | 4 | Added `typecheck` script alias; ran lint/typecheck/test | All green |

### Implementation Notes

- Reusable helper at `apps/frontend/src/lib/auth-validation.ts` exports `validateLogin`, `validateRegister`, `getFirstInvalidField`, `pickFieldErrors` and `hasErrors`.
- Both forms keep `noValidate`; validation is the single source of truth and runs before the store call.
- Per-field error ids: `login-email-error`, `login-password-error`, `register-name-error`, `register-email-error`, `register-password-error`.
- `ApiError.details` is mapped to inline field errors; errors without details fall back to the form-level alert.
- Error ids are duplicated for the two forms intentionally (different form prefixes).
- The repo working tree already had unrelated uncommitted changes; this story touched only the files listed above.

### Issues Encountered

- `apps/frontend/package.json` only defined `type-check`, so the AGENTS.md gate `npm run typecheck` did not exist. Added a `typecheck` alias (`tsc --noEmit`); `type-check` was preserved.

---

## 🧪 QA Results

> This section is populated after @qa reviews the implementation.

### Test Execution Summary

| Category | Tests | Passed | Failed | Skipped |
|----------|-------|--------|--------|---------|
| Unit | 148 | 148 | 0 | 0 |
| Integration | - | - | - | - |
| E2E | - | - | - | - |

Focused coverage for the touched surface: `auth-validation.ts` 100%, `LoginForm.tsx` 97.95%, `RegisterForm.tsx` 98.11%.

### Validation Checklist

| Check | Status | Notes |
|-------|--------|-------|
| Acceptance criteria | ✅ | All Gherkin scenarios covered by tests |
| DoD items | ✅ | Gates green |
| Edge cases | ✅ | Whitespace email, name at/over 100, login password any length, API details vs no details |
| Documentation | ✅ | Story + `docs/design/AUTH-002-error-ux.md` |

### QA Sign-off

- [x] All acceptance criteria verified
- [x] Tests passing (coverage ≥80%)
- [x] Documentation complete
- [ ] Ready for release

**QA Agent:** @qa (Quartz) — self-validated gates
**Date:** 2026-09-11

---

## 📜 Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2026-09-11 | 1.0.0 | Initial story creation | @po (Pixel) |
| 2026-09-11 | 1.1.0 | Implemented helper + both forms + tests; gates green; QA record filled | @dev (Dex) / @qa (Quartz) |

---

**Criado por:** @po (Pixel)
**Data:** 2026-09-11
**Atualizado:** 2026-09-11 (implemented and validated)
