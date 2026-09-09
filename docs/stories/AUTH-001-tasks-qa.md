# Tasks: @qa — AUTH-001

## Context

- Story: `docs/stories/AUTH-001-login.md`
- OpenAPI Spec: `docs/openapi/auth.yaml`
- Architecture: `docs/architecture/backend-architecture.md`
- Your role: **Verify quality gates, write authorization tests, validate coverage**

---

## Phase 1: Authorization Tests (CRITICAL)

### Task 1.1: Owner Scoping Tests

**Every endpoint must verify that user B cannot access user A's data.**

Create `apps/backend/src/test/java/com/felipemelozx/kairos/security/AuthorizationIntegrationTest.java`:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AuthorizationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.8-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    private String loginAs(UUID userId) {
        return jwtService.generateAccessToken(userId);
    }

    @Test
    void shouldForbidUserBFromAccessingUserAMeEndpoint() {
        User userA = createUser("userA@example.com", "User A");
        User userB = createUser("userB@example.com", "User B");

        String tokenB = loginAs(userB.getId());

        // User B tries to access their own /me - should succeed
        HttpHeaders headersB = new HttpHeaders();
        headersB.add("Cookie", "ACCESS_TOKEN=" + tokenB);
        HttpEntity<Void> requestB = new HttpEntity<>(headersB);

        ResponseEntity<ApiResponse> responseB = restTemplate.exchange("/api/auth/me", HttpMethod.GET, requestB, ApiResponse.class);
        assertThat(responseB.getStatusCode()).isEqualTo(HttpStatus.OK);

        // The response should contain user B's data, NOT user A's
        UserResponse userData = (UserResponse) responseB.getBody().getData();
        assertThat(userData.email()).isEqualTo("userB@example.com");
        assertThat(userData.email()).isNotEqualTo("userA@example.com");
    }

    @Test
    void shouldReturn401WhenNoToken() {
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity("/api/auth/me", ApiResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenInvalidToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "ACCESS_TOKEN=invalid-token");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse> response = restTemplate.exchange("/api/auth/me", HttpMethod.GET, request, ApiResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenExpiredToken() {
        User user = createUser("user@example.com", "User");
        String expiredToken = jwtService.generateExpiredAccessToken(user.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "ACCESS_TOKEN=" + expiredToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse> response = restTemplate.exchange("/api/auth/me", HttpMethod.GET, request, ApiResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private User createUser(String email, String name) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash("hashed");
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }
}
```

---

## Phase 2: Integration Tests Validation

### Task 2.1: Verify Repository Tests

Check that all repository tests use **Testcontainers** (not H2):

```bash
grep -r "@DataJpaTest" apps/backend/src/test/
```

Verify each has:

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.8-alpine");

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
}
```

### Task 2.2: Verify Controller Tests

Check that all controller tests use **MockMvc** or **TestRestTemplate**:

```bash
grep -r "@SpringBootTest" apps/backend/src/test/
```

### Task 2.3: Verify Unit Tests

Check that all service tests use **Mockito**:

```bash
grep -r "@ExtendWith(MockitoExtension.class)" apps/backend/src/test/
```

---

## Phase 3: Coverage Validation

### Task 3.1: Backend Coverage

Run coverage report:

```bash
cd apps/backend
./mvnw jacoco:report
```

Open `apps/backend/target/site/jacoco/index.html` and verify:

- [ ] Overall coverage >= 80%
- [ ] `AuthService` coverage >= 80%
- [ ] `UserService` coverage >= 80%
- [ ] `AuthController` coverage >= 80%
- [ ] `UserRepository` coverage >= 80%

If coverage < 80%, identify missing tests and assign to @dev backend.

### Task 3.2: Frontend Coverage

Run coverage report:

```bash
cd apps/frontend
npm run coverage
```

Open `apps/frontend/coverage/lcov-report/index.html` and verify:

- [ ] Overall coverage >= 80%
- [ ] `auth-store` coverage >= 80%
- [ ] `auth-api` coverage >= 80%
- [ ] `LoginForm` coverage >= 80%
- [ ] `RegisterForm` coverage >= 80%

If coverage < 80%, identify missing tests and assign to @dev frontend.

---

## Phase 4: OpenAPI Validation

### Task 4.1: Verify OpenAPI Spec Matches Implementation

Start backend:

```bash
cd apps/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Access Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

Verify:

- [ ] All auth endpoints are listed
- [ ] Request/response schemas match `docs/openapi/auth.yaml`
- [ ] Cookie authentication is documented
- [ ] Error responses are documented

### Task 4.2: Test Endpoints via Swagger UI

1. **Register:**
   - POST `/api/auth/register` with `{ email, password, name }`
   - Verify 201 response
   - Verify cookies are set

2. **Login:**
   - POST `/api/auth/login` with `{ email, password }`
   - Verify 200 response
   - Verify cookies are set

3. **Me:**
   - GET `/api/auth/me`
   - Verify 200 response with user data

4. **Logout:**
   - POST `/api/auth/logout`
   - Verify cookies are cleared

5. **Refresh:**
   - POST `/api/auth/refresh`
   - Verify new access token cookie

### Task 4.3: Verify OpenAPI Spec File

Compare `docs/openapi/auth.yaml` with auto-generated spec:

```bash
curl http://localhost:8080/v3/api-docs > /tmp/auto-generated.yaml
diff docs/openapi/auth.yaml /tmp/auto-generated.yaml
```

If differences exist, update `docs/openapi/auth.yaml` to match implementation.

---

## Phase 5: Security Validation

### Task 5.1: Verify httpOnly Cookies

Use browser DevTools or curl:

```bash
curl -v -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

Verify response headers include:

```
Set-Cookie: ACCESS_TOKEN=...; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=900
Set-Cookie: REFRESH_TOKEN=...; HttpOnly; Secure; SameSite=Strict; Path=/api/auth/refresh; Max-Age=604800
```

Check:

- [ ] `HttpOnly` flag present (prevents XSS)
- [ ] `Secure` flag present (HTTPS only)
- [ ] `SameSite=Strict` present (CSRF protection)
- [ ] `Path` is correct
- [ ] `Max-Age` is correct (900 for access, 604800 for refresh)

### Task 5.2: Verify Password Hashing

Check database:

```sql
SELECT email, password_hash FROM users WHERE email = 'test@example.com';
```

Verify:

- [ ] `password_hash` is NOT plain text
- [ ] `password_hash` starts with `$2a$` or `$2b$` (BCrypt)

### Task 5.3: Verify Google OAuth Auto-Create

1. Login with Google for the first time
2. Check database:

```sql
SELECT * FROM users WHERE email = 'google-user@gmail.com';
```

Verify:

- [ ] User created automatically
- [ ] `provider` = 'GOOGLE'
- [ ] `password_hash` is NULL
- [ ] `avatar_url` is set from Google profile

---

## Phase 6: Lint & Typecheck

### Task 6.1: Backend Lint

```bash
cd apps/backend
./mvnw checkstyle:check
```

Verify:

- [ ] Zero checkstyle violations

### Task 6.2: Frontend Lint

```bash
cd apps/frontend
npm run lint
```

Verify:

- [ ] Zero lint errors

### Task 6.3: Frontend Typecheck

```bash
cd apps/frontend
npm run typecheck
```

Verify:

- [ ] Zero type errors

---

## Phase 7: E2E Tests (Optional)

### Task 7.1: Login Flow E2E

If Playwright/Cypress is set up:

```typescript
test('should login with email and password', async ({ page }) => {
  await page.goto('/');
  await page.click('button:has-text("Login with Email")');
  await page.fill('input[name="email"]', 'test@example.com');
  await page.fill('input[name="password"]', 'password123');
  await page.click('button:has-text("Login")');
  await expect(page).toHaveURL('/calendar');
});

test('should register new user', async ({ page }) => {
  await page.goto('/');
  await page.click('button:has-text("Login with Email")');
  await page.click('text=Register');
  await page.fill('input[name="name"]', 'John Doe');
  await page.fill('input[name="email"]', 'john@example.com');
  await page.fill('input[name="password"]', 'password123');
  await page.click('button:has-text("Register")');
  await expect(page).toHaveURL('/calendar');
});
```

---

## Phase 8: Final Checklist

Before marking story as done, verify:

### Backend

- [ ] All unit tests passing
- [ ] All integration tests passing (Testcontainers)
- [ ] All authorization tests passing
- [ ] 80%+ coverage
- [ ] Zero lint errors
- [ ] OpenAPI spec accessible at `/swagger-ui.html`
- [ ] All endpoints documented with `@Operation`
- [ ] httpOnly cookies working correctly
- [ ] Password hashing with BCrypt
- [ ] Google OAuth auto-creates user

### Frontend

- [ ] All unit tests passing
- [ ] 80%+ coverage
- [ ] Zero lint errors
- [ ] Zero typecheck errors
- [ ] Login form works
- [ ] Register form works
- [ ] Auth context works
- [ ] Protected routes work
- [ ] `/api/auth/me` called on app load

### Documentation

- [ ] `docs/stories/AUTH-001-login.md` updated with completion status
- [ ] OpenAPI spec in sync with implementation
- [ ] README updated if needed

---

## Quality Gates Summary

| Gate | Status |
|------|--------|
| Unit tests passing | ☐ |
| Integration tests passing | ☐ |
| Authorization tests passing | ☐ |
| Backend coverage >= 80% | ☐ |
| Frontend coverage >= 80% | ☐ |
| Zero lint errors | ☐ |
| Zero typecheck errors | ☐ |
| OpenAPI spec in sync | ☐ |
| httpOnly cookies working | ☐ |
| Password hashing correct | ☐ |
| Google OAuth working | ☐ |

---

## Issues & Blockers

If you find issues, create a task for the relevant agent:

- **Backend issues** → assign to @dev backend
- **Frontend issues** → assign to @dev frontend
- **Coverage gaps** → assign to relevant @dev
- **Documentation gaps** → assign to @dev backend

---

## Sign-Off

Once all quality gates pass, mark the story as done:

```markdown
## Status: DONE

- Completed: [date]
- Backend coverage: [X]%
- Frontend coverage: [X]%
- Authorization tests: [X] passing
- Signed off by: @qa
```
