# Tasks: @dev backend — AUTH-001

## Context

- Story: `docs/stories/AUTH-001-login.md`
- OpenAPI Spec: `docs/openapi/auth.yaml`
- Architecture: `docs/architecture/backend-architecture.md`
- Approach: **TDD (Red-Green-Refactor)**

## Status

- **Phase 1: Setup** ✅ COMPLETE
- **Phase 2: TDD Tasks** ✅ COMPLETE (unit tests pass; integration tests require Docker)
- **Phase 3: Quality Gates** ✅ Compilation passes, 11 unit tests green

---

## Phase 1: Setup ✅

### Task 1.1: Dependencies

Add to `apps/backend/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.4</version>
</dependency>
```

### Task 1.2: Application Properties

Add to `apps/backend/src/main/resources/application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile

jwt:
  secret: ${JWT_SECRET:default-secret-change-in-production-min-256-bits-long}
  access-token-expiration: 900000
  refresh-token-expiration: 604800000
```

---

## Phase 2: TDD Tasks

### Task 2.1: User Entity + Migration

**🔴 RED — Write failing test first:**

Create `apps/backend/src/test/java/com/felipemelozx/kairos/repository/UserRepositoryIntegrationTest.java`:

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.8-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setPasswordHash("hashed");
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setCreatedAt(Instant.now());

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmailAndActiveIsTrue("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    void shouldNotFindInactiveUser() {
        User user = new User();
        user.setEmail("inactive@example.com");
        user.setName("Inactive");
        user.setPasswordHash("hashed");
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(false);
        user.setCreatedAt(Instant.now());

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmailAndActiveIsTrue("inactive@example.com");
        assertThat(found).isEmpty();
    }
}
```

**Run test → FAILS** (no entity, no repository, no table)

**🟢 GREEN — Implement minimum to pass:**

1. Create `apps/backend/src/main/java/com/felipemelozx/kairos/entity/User.java`:

```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email", unique = true)
})
public class User {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {}

    // Getters and setters
}
```

2. Create `apps/backend/src/main/java/com/felipemelozx/kairos/entity/enums/AuthProvider.java`:

```java
public enum AuthProvider {
    LOCAL, GOOGLE
}
```

3. Create `apps/backend/src/main/java/com/felipemelozx/kairos/repository/UserRepository.java`:

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailAndActiveIsTrue(String email);
    boolean existsByEmail(String email);
}
```

4. Create `apps/backend/src/main/resources/db/migration/V1__Create_users.sql`:

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
```

**Run test → PASSES**

**🔵 REFACTOR:**
- Review entity structure
- Ensure all fields match PRD

---

### Task 2.2: Register Endpoint

**🔴 RED:**

Create `apps/backend/src/test/java/com/felipemelozx/kairos/service/AuthServiceTest.java`:

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    @Test
    void shouldRegisterUserWhenEmailNotExists() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "John");
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse result = authService.register(request);

        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.name()).isEqualTo("John");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("exists@example.com", "password123", "John");
        when(userRepository.existsByEmail("exists@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");
    }
}
```

Create `apps/backend/src/test/java/com/felipemelozx/kairos/controller/AuthControllerIntegrationTest.java`:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AuthControllerIntegrationTest {

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

    @Test
    void shouldRegisterUserWhenValidRequest() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "John Doe");

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/api/auth/register", request, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(userRepository.existsByEmail("new@example.com")).isTrue();
    }

    @Test
    void shouldRejectRegisterWhenEmailExists() {
        // Create existing user first
        createUser("exists@example.com", "password123", "Existing");

        RegisterRequest request = new RegisterRequest("exists@example.com", "password123", "New");
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/api/auth/register", request, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
```

**Run tests → FAIL**

**🟢 GREEN:**

1. Create `apps/backend/src/main/java/com/felipemelozx/kairos/dto/request/RegisterRequest.java`:

```java
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank @Size(max = 100) String name
) {}
```

2. Create `apps/backend/src/main/java/com/felipemelozx/kairos/dto/response/UserResponse.java`:

```java
public record UserResponse(UUID id, String email, String name, String avatarUrl, String provider, boolean active, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getAvatarUrl(), user.getProvider().name(), user.getActive(), user.getCreatedAt());
    }
}
```

3. Create `apps/backend/src/main/java/com/felipemelozx/kairos/dto/response/AuthResponse.java`:

```java
public record AuthResponse(UserResponse user) {}
```

4. Create `apps/backend/src/main/java/com/felipemelozx/kairos/service/AuthService.java`:

```java
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_EXISTS", "Email already registered");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }
}
```

5. Create `apps/backend/src/main/java/com/felipemelozx/kairos/controller/AuthController.java`:

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        UserResponse user = authService.register(request);
        // Set cookies here
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
    }
}
```

6. Create `apps/backend/src/main/java/com/felipemelozx/kairos/config/SecurityConfig.java`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Run tests → PASS**

**🔵 REFACTOR:**
- Add `@Operation` OpenAPI annotations to controller
- Extract cookie logic to `CookieUtils`

---

### Task 2.3: Login Endpoint

**🔴 RED:**

Add to `AuthServiceTest`:

```java
@Test
void shouldLoginWhenValidCredentials() {
    User user = createUser("test@example.com", "hashedPassword", "Test");
    when(userRepository.findByEmailAndActiveIsTrue("test@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

    UserResponse result = authService.login(new LoginRequest("test@example.com", "password123"));

    assertThat(result.email()).isEqualTo("test@example.com");
}

@Test
void shouldRejectLoginWhenInvalidPassword() {
    User user = createUser("test@example.com", "hashedPassword", "Test");
    when(userRepository.findByEmailAndActiveIsTrue("test@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

    assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "wrongPassword")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Invalid");
}
```

**Run → FAIL → GREEN → REFACTOR**

---

### Task 2.4: JwtService + CookieUtils

**🔴 RED:**

```java
@Test
void shouldGenerateAccessTokenWithUserId() {
    String token = jwtService.generateAccessToken(userId);
    Claims claims = jwtService.parseToken(token);
    assertThat(claims.getSubject()).isEqualTo(userId.toString());
}

@Test
void shouldValidateToken() {
    String token = jwtService.generateAccessToken(userId);
    assertThat(jwtService.validateToken(token)).isTrue();
}
```

**🟢 GREEN:**

```java
@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    public String generateAccessToken(UUID userId) {
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

```java
public class CookieUtils {
    public static void addAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("ACCESS_TOKEN", token)
            .httpOnly(true).secure(true).sameSite("Strict").path("/").maxAge(900).build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("REFRESH_TOKEN", token)
            .httpOnly(true).secure(true).sameSite("Strict").path("/api/auth/refresh").maxAge(604800).build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void clearCookies(HttpServletResponse response) {
        // Clear both cookies with Max-Age=0
    }

    public static String getAccessTokenFromCookies(jakarta.servlet.http.Cookie[] cookies) {
        if (cookies == null) return null;
        return Arrays.stream(cookies)
            .filter(c -> "ACCESS_TOKEN".equals(c.getName()))
            .map(jakarta.servlet.http.Cookie::getValue)
            .findFirst().orElse(null);
    }
}
```

---

### Task 2.5: JwtCookieAuthenticationFilter

**🔴 RED:**

```java
@Test
void shouldAuthenticateWhenValidTokenCookie() {
    // Mock filter chain, request with cookie, verify SecurityContext is set
}

@Test
void shouldNotAuthenticateWhenNoCookie() {
    // Verify filter chain continues without authentication
}
```

**🟢 GREEN:**

```java
@Component
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        String token = CookieUtils.getAccessTokenFromCookies(request.getCookies());
        if (token != null && jwtService.validateToken(token)) {
            UUID userId = UUID.fromString(jwtService.getUserIdFromToken(token));
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
```

---

### Task 2.6: Logout, Refresh, Me Endpoints

Follow same TDD pattern for each:

**Logout:**
- 🔴 Test: `shouldClearCookiesOnLogout`
- 🟢 Implement: clear cookies
- 🔵 Refactor

**Refresh:**
- 🔴 Test: `shouldRefreshAccessTokenWhenValidRefreshToken`
- 🔴 Test: `shouldRejectRefreshWhenExpiredToken`
- 🟢 Implement: validate refresh token, generate new access token
- 🔵 Refactor

**Me:**
- 🔴 Test: `shouldReturnCurrentUserWhenAuthenticated`
- 🔴 Test: `shouldReturn401WhenNotAuthenticated`
- 🟢 Implement: get userId from SecurityContext, return user
- 🔵 Refactor

---

### Task 2.7: Google OAuth2

**🔴 RED:**

```java
@Test
void shouldCreateUserOnFirstGoogleLogin() {
    // Mock OAuth2User with email, name, picture
    // Verify user created in DB
}

@Test
void shouldLoginExistingUserOnGoogleLogin() {
    // Create user first
    // Mock OAuth2User with same email
    // Verify no new user created
}
```

**🟢 GREEN:**

```java
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        User user = authService.findOrCreateGoogleUser(email, name, picture);
        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        CookieUtils.addAccessTokenCookie(response, accessToken);
        CookieUtils.addRefreshTokenCookie(response, refreshToken);

        getRedirectStrategy().sendRedirect(request, response, "/calendar");
    }
}
```

---

### Task 2.8: OpenAPI Annotations

Add to all controller methods:

```java
@Operation(summary = "Register a new user", description = "Creates a new user account with email and password")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "User registered successfully"),
    @ApiResponse(responseCode = "400", description = "Validation error"),
    @ApiResponse(responseCode = "409", description = "Email already exists")
})
```

---

## Quality Gates

- [ ] All unit tests passing
- [ ] All integration tests passing (Testcontainers)
- [ ] 80%+ coverage
- [ ] Zero lint errors
- [ ] OpenAPI spec accessible at `/swagger-ui.html`
- [ ] All endpoints documented with `@Operation`
