package com.felipemelozx.kairos.security.filters;

import com.felipemelozx.kairos.security.csrf.CsrfTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
public class CsrfValidationFilter extends OncePerRequestFilter {

    private static final String CSRF_COOKIE_NAME = "CSRF_TOKEN";
    private static final String CSRF_HEADER_NAME = "X-CSRF-Token";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/oauth2/**",
            "/login/oauth2/**"
    );

    private final CsrfTokenService csrfTokenService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public CsrfValidationFilter(CsrfTokenService csrfTokenService) {
        this.csrfTokenService = csrfTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SAFE_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            path = path.substring(contextPath.length());
        }

        for (String publicPath : PUBLIC_PATHS) {
            if (pathMatcher.match(publicPath, path)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        String headerToken = request.getHeader(CSRF_HEADER_NAME);
        String cookieToken = getCookieValue(request, CSRF_COOKIE_NAME);

        if (headerToken == null || cookieToken == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing CSRF token");
            return;
        }

        if (!MessageDigest.isEqual(
                headerToken.getBytes(StandardCharsets.UTF_8),
                cookieToken.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF token mismatch");
            return;
        }

        if (!csrfTokenService.validateToken(cookieToken)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
