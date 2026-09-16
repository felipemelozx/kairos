package com.felipemelozx.kairos.security.oauth2;

import com.felipemelozx.kairos.dto.response.UserResponse;
import com.felipemelozx.kairos.entity.User;
import com.felipemelozx.kairos.entity.enums.AuthProvider;
import com.felipemelozx.kairos.repository.UserRepository;
import com.felipemelozx.kairos.security.AuthCookieService;
import com.felipemelozx.kairos.security.csrf.CsrfTokenService;
import com.felipemelozx.kairos.security.jwt.JwtService;
import com.felipemelozx.kairos.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    private static final String FRONTEND_URL = "http://localhost:3000";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String CSRF_TOKEN = "known-csrf-token";

    @Mock
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    @Mock
    private CsrfTokenService csrfTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private OAuth2AuthenticationSuccessHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        AuthCookieService authCookieService = new AuthCookieService(jwtService, csrfTokenService, userRepository);
        handler = new OAuth2AuthenticationSuccessHandler(authService, authCookieService, FRONTEND_URL);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldEmitCsrfCookieWithExpectedAttributesWhenOAuth2LoginSucceeds() throws Exception {
        stubSuccessfulOAuth2Login();

        handler.onAuthenticationSuccess(request, response, authentication);

        String csrfCookie = findCookie(response.getHeaders("Set-Cookie"), "CSRF_TOKEN");
        assertThat(csrfCookie).contains("CSRF_TOKEN=" + CSRF_TOKEN);
        assertThat(csrfCookie).contains("Secure");
        assertThat(csrfCookie).contains("SameSite=Strict");
        assertThat(csrfCookie).contains("Max-Age=900");
        assertThat(csrfCookie).doesNotContain("HttpOnly");
        assertThat(csrfCookie).doesNotContain("Domain=");
    }

    @Test
    void shouldEmitAccessAndRefreshCookiesWhenOAuth2LoginSucceeds() throws Exception {
        stubSuccessfulOAuth2Login();

        handler.onAuthenticationSuccess(request, response, authentication);

        Collection<String> setCookies = response.getHeaders("Set-Cookie");
        assertThat(findCookie(setCookies, "ACCESS_TOKEN")).contains("ACCESS_TOKEN=access-token");
        assertThat(findCookie(setCookies, "REFRESH_TOKEN")).contains("REFRESH_TOKEN=refresh-token");
    }

    @Test
    void shouldRedirectToFrontendWhenOAuth2LoginSucceeds() throws Exception {
        stubSuccessfulOAuth2Login();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo(FRONTEND_URL + "/");
    }

    private void stubSuccessfulOAuth2Login() {
        DefaultOAuth2User oAuth2User = new DefaultOAuth2User(
                Collections.<GrantedAuthority>emptyList(),
                Map.of(
                        "email", "user@example.com",
                        "name", "Test User",
                        "picture", "https://picture.url"),
                "email");
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(authService.findOrCreateGoogleUser("user@example.com", "Test User", "https://picture.url"))
                .thenReturn(new UserResponse(
                        USER_ID,
                        "user@example.com",
                        "Test User",
                        "https://picture.url",
                        "GOOGLE",
                        true,
                        Instant.now()));
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("user@example.com");
        user.setName("Test User");
        user.setProvider(AuthProvider.GOOGLE);
        user.setActive(true);
        user.setTokenVersion(0);
        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateAccessToken(USER_ID, 0)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(USER_ID, 0)).thenReturn("refresh-token");
        when(csrfTokenService.generateToken()).thenReturn(CSRF_TOKEN);
    }

    private String findCookie(Collection<String> setCookies, String name) {
        List<String> cookies = new ArrayList<>(setCookies);
        return cookies.stream()
                .filter(cookie -> cookie.startsWith(name + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cookie not found: " + name));
    }
}
