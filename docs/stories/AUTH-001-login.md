# AUTH-001: Authentication & Authorization

## Story

**As a** user  
**I want to** authenticate via Google OAuth or email/password  
**So that** I can securely access my Kairos data

---

## Acceptance Criteria

### Backend

- [x] Entity `User` with fields: `id`, `email`, `passwordHash`, `name`, `avatarUrl`, `provider` (GOOGLE/LOCAL), `active`, `createdAt`
- [x] Migration `V1__Create_users.sql` with unique constraint on `email`
- [x] `POST /api/auth/register` accepts `{ email, password, name }` and returns user with httpOnly cookies
- [x] `POST /api/auth/login` accepts `{ email, password }` and returns user with httpOnly cookies
- [x] `POST /api/auth/logout` clears both cookies
- [x] `POST /api/auth/refresh` accepts refresh cookie and returns new access cookie
- [x] `GET /api/auth/me` returns current authenticated user
- [x] Google OAuth2 auto-creates user on first login
- [x] httpOnly cookies: `ACCESS_TOKEN` (15 min), `REFRESH_TOKEN` (7 days)
- [x] OpenAPI annotations on all auth endpoints
- [ ] All tests passing (unit, integration, authorization) — unit tests pass; integration tests require Docker

### Frontend

- [ ] Landing page with "Login with Google" button
- [ ] Landing page with "Login with Email" button (shows register/login forms)
- [ ] Register form with fields: `email`, `password`, `name`
- [ ] Login form with fields: `email`, `password`
- [ ] Auth context (Zustand store) with `user` state and `login`, `register`, `logout` actions
- [ ] API client configured with `credentials: 'include'` for cookies
- [ ] Protected route middleware redirects to `/` if not authenticated
- [ ] `GET /api/auth/me` called on app load to restore session
- [ ] All tests passing (unit, integration)

### Quality Gates

- [ ] Unit tests: `AuthService`, `UserService` (80%+ coverage)
- [ ] Integration tests: `UserRepository`, `AuthController` (Testcontainers)
- [ ] Authorization tests: user B cannot access user A's `/api/auth/me`
- [ ] Frontend tests: login form, register form, auth context
- [ ] OpenAPI spec in sync with implementation
- [ ] Zero lint/typecheck errors

---

## Technical Tasks (TDD Order)

### Phase 1: OpenAPI Contract

**Owner:** @dev backend  
**Deliverable:** `docs/openapi/auth.yaml`

1. Define OpenAPI 3.0 spec for all auth endpoints
2. Include request/response schemas
3. Document cookie authentication
4. Review with team

---

### Phase 2: Backend TDD

**Owner:** @dev backend  
**Approach:** Red-Green-Refactor

#### Task 2.1: User Entity + Migration

**Red:**
- Write integration test: `shouldSaveAndFindUserByEmail`
- Run test → fails (no entity, no table)

**Green:**
- Create `User` entity with JPA annotations
- Create migration `V1__Create_users.sql`
- Run test → passes

**Refactor:**
- Review entity structure
- Ensure indexes are correct

#### Task 2.2: Register Endpoint

**Red:**
- Write integration test: `shouldRegisterUserWhenValidRequest`
- Write unit test: `shouldHashPasswordBeforeSaving`
- Run tests → fail (no endpoint)

**Green:**
- Create `AuthController.register()`
- Create `AuthService.register()`
- Create `UserRepository`
- Implement BCrypt password hashing
- Set httpOnly cookies
- Run tests → pass

**Refactor:**
- Extract password hashing to utility
- Add OpenAPI annotations

#### Task 2.3: Login Endpoint

**Red:**
- Write integration test: `shouldLoginUserWhenValidCredentials`
- Write integration test: `shouldRejectLoginWhenInvalidPassword`
- Run tests → fail

**Green:**
- Create `AuthController.login()`
- Create `AuthService.login()`
- Implement password verification
- Set httpOnly cookies
- Run tests → pass

**Refactor:**
- Add OpenAPI annotations

#### Task 2.4: Logout Endpoint

**Red:**
- Write integration test: `shouldClearCookiesOnLogout`
- Run test → fail

**Green:**
- Create `AuthController.logout()`
- Clear cookies in response
- Run test → pass

**Refactor:**
- Add OpenAPI annotations

#### Task 2.5: Refresh Endpoint

**Red:**
- Write integration test: `shouldRefreshAccessTokenWhenValidRefreshToken`
- Write integration test: `shouldRejectRefreshWhenExpiredToken`
- Run tests → fail

**Green:**
- Create `AuthController.refresh()`
- Create `JwtService.refreshAccessToken()`
- Validate refresh token
- Set new access cookie
- Run tests → pass

**Refactor:**
- Add OpenAPI annotations

#### Task 2.6: Me Endpoint

**Red:**
- Write integration test: `shouldReturnCurrentUserWhenAuthenticated`
- Write integration test: `shouldReturn401WhenNotAuthenticated`
- Run tests → fail

**Green:**
- Create `AuthController.me()`
- Extract user from JWT cookie
- Return user data
- Run tests → pass

**Refactor:**
- Add OpenAPI annotations

#### Task 2.7: Google OAuth2

**Red:**
- Write integration test: `shouldCreateUserOnFirstGoogleLogin`
- Write integration test: `shouldLoginExistingUserOnGoogleLogin`
- Run tests → fail

**Green:**
- Configure Spring Security OAuth2
- Create `OAuth2AuthenticationSuccessHandler`
- Implement auto-create logic
- Set httpOnly cookies after OAuth
- Run tests → pass

**Refactor:**
- Add OpenAPI annotations

#### Task 2.8: Authorization Tests

**Red:**
- Write test: `shouldForbidUserBFromAccessingUserAMeEndpoint`
- Run test → fail

**Green:**
- Ensure all endpoints filter by authenticated userId
- Run test → pass

---

### Phase 3: Frontend TDD

**Owner:** @dev frontend  
**Approach:** Red-Green-Refactor

#### Task 3.1: Auth Context

**Red:**
- Write unit test: `shouldStoreUserAfterLogin`
- Write unit test: `shouldClearUserAfterLogout`
- Run tests → fail

**Green:**
- Create Zustand store `useAuthStore`
- Implement `login`, `register`, `logout`, `fetchMe` actions
- Run tests → pass

**Refactor:**
- Extract API client to separate module

#### Task 3.2: API Client

**Red:**
- Write unit test: `shouldIncludeCredentialsInRequests`
- Run test → fail

**Green:**
- Create `apiClient` with `credentials: 'include'`
- Implement `auth.register`, `auth.login`, `auth.logout`, `auth.me`
- Run test → pass

**Refactor:**
- Add error handling

#### Task 3.3: Login Form

**Red:**
- Write unit test: `shouldCallLoginApiOnSubmit`
- Write unit test: `shouldShowErrorOnInvalidCredentials`
- Run tests → fail

**Green:**
- Create `LoginForm` component
- Implement form validation
- Call `auth.login` on submit
- Run tests → pass

**Refactor:**
- Add loading state

#### Task 3.4: Register Form

**Red:**
- Write unit test: `shouldCallRegisterApiOnSubmit`
- Write unit test: `shouldValidateEmailFormat`
- Write unit test: `shouldValidatePasswordLength`
- Run tests → fail

**Green:**
- Create `RegisterForm` component
- Implement form validation (email, password min 8 chars, name required)
- Call `auth.register` on submit
- Run tests → pass

**Refactor:**
- Add loading state

#### Task 3.5: Landing Page

**Red:**
- Write unit test: `shouldRenderLoginWithGoogleButton`
- Write unit test: `shouldRenderLoginWithEmailButton`
- Run tests → fail

**Green:**
- Update `page.tsx` with auth buttons
- "Login with Google" → redirect to `/oauth2/authorization/google`
- "Login with Email" → show login/register forms
- Run tests → pass

**Refactor:**
- Add responsive design

#### Task 3.6: Protected Routes

**Red:**
- Write unit test: `shouldRedirectToHomeWhenNotAuthenticated`
- Run test → fail

**Green:**
- Create `ProtectedRoute` component
- Check auth state on mount
- Redirect to `/` if not authenticated
- Run test → pass

**Refactor:**
- Add loading state

#### Task 3.7: Auth on App Load

**Red:**
- Write unit test: `shouldCallMeEndpointOnAppLoad`
- Run test → fail

**Green:**
- Call `auth.fetchMe()` in `layout.tsx` or app init
- Update auth state based on response
- Run test → pass

**Refactor:**
- Handle errors gracefully

---

### Phase 4: Quality Assurance

**Owner:** @qa  
**Approach:** Verify all quality gates

#### Task 4.1: Authorization Tests

- Write test: `shouldForbidUserBFromAccessingUserAProject`
- Write test: `shouldForbidUserBFromAccessingUserATimeBlock`
- Ensure all endpoints have owner-scoping tests

#### Task 4.2: Integration Tests

- Verify all repository tests use Testcontainers
- Verify all controller tests use MockMvc
- Ensure 80%+ coverage

#### Task 4.3: OpenAPI Validation

- Verify OpenAPI spec matches implementation
- Test endpoints via Swagger UI
- Ensure all endpoints documented

#### Task 4.4: E2E Tests (Optional)

- Write E2E test: `shouldLoginWithEmailAndPassword`
- Write E2E test: `shouldRegisterNewUser`
- Write E2E test: `shouldLoginWithGoogle` (if possible in CI)

#### Task 4.5: Coverage Report

- Run `./mvnw jacoco:report`
- Verify 80%+ coverage
- Run `npm run coverage` for frontend
- Verify 80%+ coverage

---

## Definition of Done

- [ ] All acceptance criteria met
- [ ] All tests passing (unit, integration, authorization)
- [ ] 80%+ code coverage (backend + frontend)
- [ ] OpenAPI spec in sync with implementation
- [ ] Zero lint/typecheck errors
- [ ] Code reviewed by @qa
- [ ] Documentation updated (README, AGENTS.md if needed)

---

## Agent Tasks

- **@dev backend:** `docs/stories/AUTH-001-tasks-dev-backend.md`
- **@dev frontend:** `docs/stories/AUTH-001-tasks-dev-frontend.md`
- **@qa:** `docs/stories/AUTH-001-tasks-qa.md`

## Artifacts

- **OpenAPI Spec:** `docs/openapi/auth.yaml`
- **Architecture:** `docs/architecture/backend-architecture.md`

---

## Dependencies

- None (first story)

---

## Estimated Effort

- **@dev backend:** 2-3 days
- **@dev frontend:** 2-3 days
- **@qa:** 1 day

---

## Notes

- TDD approach: Red-Green-Refactor for all tasks
- OpenAPI spec must be defined before implementation
- httpOnly cookies for security (no localStorage)
- Google OAuth auto-creates user on first login
- All endpoints must be owner-scoped (userId from JWT)
