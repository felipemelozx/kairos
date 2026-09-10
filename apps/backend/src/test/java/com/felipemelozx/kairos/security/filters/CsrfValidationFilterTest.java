package com.felipemelozx.kairos.security.filters;

import com.felipemelozx.kairos.security.csrf.CsrfTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsrfValidationFilterTest {

    private CsrfValidationFilter filter;

    @Mock
    private CsrfTokenService csrfTokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CsrfValidationFilter(csrfTokenService);
    }

    @Test
    void shouldAllowGetRequestWithoutCsrfCheck() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowPostToLoginWithoutCsrfCheck() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getContextPath()).thenReturn("/api");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowPostToRegisterWithoutCsrfCheck() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        when(request.getContextPath()).thenReturn("/api");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowPostToRefreshWithoutCsrfCheck() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/refresh");
        when(request.getContextPath()).thenReturn("/api");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowPostToOAuth2WithoutCsrfCheck() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/oauth2/authorization/google");
        when(request.getContextPath()).thenReturn("/api");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectPostWhenHeaderMissing() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/logout");
        when(request.getContextPath()).thenReturn("/api");
        when(request.getHeader("X-CSRF-Token")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("CSRF_TOKEN", "some-token")});

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(403, "Missing CSRF token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldRejectPostWhenCookieMissing() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/logout");
        when(request.getContextPath()).thenReturn("/api");
        when(request.getHeader("X-CSRF-Token")).thenReturn("some-token");
        when(request.getCookies()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(403, "Missing CSRF token");
    }

    @Test
    void shouldRejectPostWhenHeaderDoesNotMatchCookie() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/logout");
        when(request.getContextPath()).thenReturn("/api");
        when(request.getHeader("X-CSRF-Token")).thenReturn("header-token");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("CSRF_TOKEN", "cookie-token")});

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(403, "CSRF token mismatch");
    }

    @Test
    void shouldRejectPostWhenTokenInvalid() throws Exception {
        String token = "valid-token";
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/logout");
        when(request.getContextPath()).thenReturn("/api");
        when(request.getHeader("X-CSRF-Token")).thenReturn(token);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("CSRF_TOKEN", token)});
        when(csrfTokenService.validateToken(token)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(403, "Invalid CSRF token");
    }

    @Test
    void shouldAllowPostWhenValid() throws Exception {
        String token = "valid-token";
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/logout");
        when(request.getContextPath()).thenReturn("/api");
        when(request.getHeader("X-CSRF-Token")).thenReturn(token);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("CSRF_TOKEN", token)});
        when(csrfTokenService.validateToken(token)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowPutWhenValid() throws Exception {
        String token = "valid-token";
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/api/some/resource");
        when(request.getContextPath()).thenReturn("/api");
        when(request.getHeader("X-CSRF-Token")).thenReturn(token);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("CSRF_TOKEN", token)});
        when(csrfTokenService.validateToken(token)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowDeleteWhenValid() throws Exception {
        String token = "valid-token";
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/some/resource");
        when(request.getContextPath()).thenReturn("/api");
        when(request.getHeader("X-CSRF-Token")).thenReturn(token);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("CSRF_TOKEN", token)});
        when(csrfTokenService.validateToken(token)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
