# SEC-001: CSRF Protection (Double Submit Cookie + Origin Check + CORS)

## Story

**As a** user  
**I want to** my session protected against CSRF attacks  
**So that** an attacker cannot perform state-changing actions on my behalf

---

## Acceptance Criteria

### Backend

- [x] `CsrfTokenService` generates signed CSRF tokens (HMAC-SHA256, 32 bytes random, 15 min expiry)
- [x] `CsrfTokenService` validates signed CSRF tokens (constant-time comparison)
- [x] `CsrfValidationFilter` validates `X-CSRF-Token` header matches `CSRF_TOKEN` cookie on state-changing methods (POST/PUT/PATCH/DELETE)
- [x] `CsrfValidationFilter` skips public routes: `/auth/login`, `/auth/register`, `/auth/refresh`, `/oauth2/**`, `/login/oauth2/**`
- [x] `OriginValidationFilter` validates `Origin` header against allowlist on state-changing methods
- [x] `OriginValidationFilter` falls back to `Referer` when `Origin` absent; logs WARN when both absent
- [x] `CookieUtils.addCsrfTokenCookie` sets `CSRF_TOKEN` cookie: `Secure`, `SameSite=Strict`, `httpOnly=false`, host-only (no `Domain`), maxAge 900s
- [x] `CookieUtils.clearCsrfTokenCookie` clears CSRF cookie on logout
- [ ] CSRF cookie emitted in: `/auth/login`, `/auth/register`, `/auth/refresh`, OAuth2 success (OAuth2 flow without test)
- [x] CORS configured: allowlist-based, `allowCredentials=true`, explicit allowed methods/headers
- [x] `SecurityConfig` registers both filters in correct order
- [x] All tests passing

### Frontend

- [x] `lib/csrf.ts` — `getCsrfToken()` reads `CSRF_TOKEN` cookie
- [x] `lib/api.ts` — `apiFetch` wrapper injects `X-CSRF-Token` header on POST/PUT/PATCH/DELETE
- [x] All tests passing

### Quality Gates

- [x] Unit tests: `CsrfTokenService` (generate, validate, reject tampered/expired)
- [x] Unit tests: `OriginValidationFilter` (allowlist, reject malicious origin, fail-open)
- [x] Unit tests: `CsrfValidationFilter` (skip public, reject missing header, reject mismatch)
- [x] Integration test: login sets CSRF cookie, logout clears it
- [x] Frontend tests: `getCsrfToken`, `apiFetch` header injection
- [x] Zero lint/typecheck errors
- [x] `./mvnw test` passes
- [x] `npm run lint && npm run typecheck && npm test` passes

---

## Technical Tasks (TDD Order)

### Phase 1: Backend TDD

**Owner:** @dev backend

#### Task 1.1: CsrfTokenService

**Red:**
- Write unit test: `shouldGenerateValidSignedToken`
- Write unit test: `shouldValidateValidToken`
- Write unit test: `shouldRejectTamperedToken`
- Write unit test: `shouldRejectExpiredToken`
- Run tests → fail (no service)

**Green:**
- Create `security/csrf/CsrfTokenService.java`
- Generate: 32 bytes `SecureRandom` → base64url → payload `random.expEpoch` → HMAC-SHA256 → `payload.signature`
- Validate: split, verify HMAC (constant-time), check expiry
- Run tests → pass

**Refactor:**
- Review constant-time comparison usage

#### Task 1.2: CookieUtils CSRF methods

**Red:**
- Write unit test: `shouldAddCsrfTokenCookieWithCorrectAttributes`
- Write unit test: `shouldClearCsrfTokenCookie`
- Run tests → fail

**Green:**
- Add `addCsrfTokenCookie(response, token)` — Secure, SameSite=Strict, httpOnly=false, host-only, maxAge 900
- Add `clearCsrfTokenCookie(response)` — maxAge 0
- Run tests → pass

#### Task 1.3: OriginValidationFilter

**Red:**
- Write unit test: `shouldAllowRequestWhenOriginInAllowlist`
- Write unit test: `shouldRejectRequestWhenOriginNotInAllowlist`
- Write unit test: `shouldAllowRequestWhenOriginAbsentAndRefererInAllowlist`
- Write unit test: `shouldAllowRequestWhenBothAbsentWithWarning`
- Run tests → fail

**Green:**
- Create `security/filters/OriginValidationFilter.java`
- Check Origin → Referer → fail-open with WARN
- Run tests → pass

#### Task 1.4: CsrfValidationFilter

**Red:**
- Write unit test: `shouldSkipPublicRoutes`
- Write unit test: `shouldRejectWhenHeaderMissing`
- Write unit test: `shouldRejectWhenHeaderDoesNotMatchCookie`
- Write unit test: `shouldRejectWhenTokenInvalid`
- Write unit test: `shouldAllowWhenValid`
- Run tests → fail

**Green:**
- Create `security/filters/CsrfValidationFilter.java`
- Skip public routes, compare header vs cookie (constant-time), validate signature
- Run tests → pass

#### Task 1.5: SecurityConfig + CORS

**Red:**
- Write integration test: `shouldReturn403WhenOriginInvalid`
- Write integration test: `shouldReturn403WhenCsrfHeaderMissing`
- Run tests → fail

**Green:**
- Register filters in `SecurityConfig`
- Configure CORS with allowlist
- Add properties to `application.yml`
- Run tests → pass

#### Task 1.6: Emit CSRF cookie on auth endpoints

**Red:**
- Write integration test: `shouldSetCsrfCookieOnLogin`
- Write integration test: `shouldSetCsrfCookieOnRegister`
- Write integration test: `shouldSetCsrfCookieOnRefresh`
- Write integration test: `shouldClearCsrfCookieOnLogout`
- Run tests → fail

**Green:**
- Update `AuthController` to emit CSRF cookie on login/register/refresh
- Update `OAuth2AuthenticationSuccessHandler` to emit CSRF cookie
- Update `CookieUtils.clearCookies` to clear CSRF cookie
- Run tests → pass

### Phase 2: Frontend

**Owner:** @dev frontend

#### Task 2.1: CSRF utility

**Red:**
- Write unit test: `shouldReturnTokenFromCookie`
- Write unit test: `shouldReturnNullWhenCookieMissing`
- Run tests → fail

**Green:**
- Create `src/lib/csrf.ts` with `getCsrfToken()`
- Run tests → pass

#### Task 2.2: API wrapper

**Red:**
- Write unit test: `shouldInjectCsrfHeaderOnPost`
- Write unit test: `shouldNotInjectCsrfHeaderOnGet`
- Run tests → fail

**Green:**
- Create `src/lib/api.ts` with `apiFetch`
- Run tests → pass

### Phase 3: Quality Assurance

**Owner:** @qa

- Verify all tests pass
- Verify zero lint/typecheck errors
- Verify CORS headers correct in responses
- Verify CSRF cookie attributes correct

---

## Definition of Done

- [ ] All acceptance criteria met (OAuth2 success CSRF cookie emission without test)
- [x] All tests passing
- [x] Zero lint/typecheck errors
- [ ] Code reviewed by @qa

---

## File List

### Backend (main)
- `apps/backend/src/main/java/com/felipemelozx/kairos/security/csrf/CsrfTokenService.java`
- `apps/backend/src/main/java/com/felipemelozx/kairos/security/filters/OriginValidationFilter.java`
- `apps/backend/src/main/java/com/felipemelozx/kairos/security/filters/CsrfValidationFilter.java`
- `apps/backend/src/main/java/com/felipemelozx/kairos/security/CookieUtils.java` (CSRF cookie methods)
- `apps/backend/src/main/java/com/felipemelozx/kairos/config/SecurityConfig.java` (filters + CORS)
- `apps/backend/src/main/java/com/felipemelozx/kairos/controller/AuthController.java` (CSRF cookie emission)
- `apps/backend/src/main/java/com/felipemelozx/kairos/security/oauth2/OAuth2AuthenticationSuccessHandler.java` (CSRF cookie emission)
- `apps/backend/src/main/resources/application.yml` (`app.security.*`)

### Backend (test)
- `apps/backend/src/test/java/com/felipemelozx/kairos/security/csrf/CsrfTokenServiceTest.java`
- `apps/backend/src/test/java/com/felipemelozx/kairos/security/filters/OriginValidationFilterTest.java`
- `apps/backend/src/test/java/com/felipemelozx/kairos/security/filters/CsrfValidationFilterTest.java`
- `apps/backend/src/test/java/com/felipemelozx/kairos/controller/AuthControllerIntegrationTest.java` (login/register/refresh/logout + 403s)

### Frontend
- `apps/frontend/src/lib/csrf.ts`
- `apps/frontend/src/lib/api.ts`
- `apps/frontend/src/lib/csrf.test.ts`
- `apps/frontend/src/lib/api.test.ts`

### Scripts
- `scripts/start-backend.sh` (.env sourcing)

## Dependencies

- AUTH-001 (authentication)

---

## Estimated Effort

- **@dev backend:** 1-2 days
- **@dev frontend:** 0.5 day
- **@qa:** 0.5 day

---

## Notes

- CSRF token signed with HMAC-SHA256 to mitigate cookie tossing/fixation
- Cookie is host-only (no `Domain`) to prevent subdomain cookie tossing
- `SameSite=Strict` on CSRF cookie provides additional protection
- CORS configured with explicit allowlist (no wildcards with credentials)
- Public auth routes (login/register/refresh) skip CSRF check but are protected by Origin check
- Integration tests (task 1.5) exposed two `SecurityConfig` bugs, fixed in this story: `/auth/refresh` and `/error` were missing from `permitAll`, causing anonymous requests to be redirected to Google OAuth instead of returning 403/401/refreshing tokens
