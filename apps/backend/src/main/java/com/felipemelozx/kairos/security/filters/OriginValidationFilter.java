package com.felipemelozx.kairos.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

@Component
public class OriginValidationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OriginValidationFilter.class);
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final List<String> allowedOrigins;

    public OriginValidationFilter(
            @Value("${app.security.allowed-origins:http://localhost:3000}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SAFE_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader("Origin");
        if (origin != null) {
            if (isAllowed(origin)) {
                filterChain.doFilter(request, response);
                return;
            }
            log.warn("Blocked request with disallowed Origin: {}", origin);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid origin");
            return;
        }

        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI refererUri = URI.create(referer);
                String refererOrigin = refererUri.getScheme() + "://" + refererUri.getHost()
                        + (refererUri.getPort() > 0 ? ":" + refererUri.getPort() : "");
                if (isAllowed(refererOrigin)) {
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                log.warn("Failed to parse Referer header: {}", referer);
            }
            log.warn("Blocked request with disallowed Referer: {}", referer);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid origin");
            return;
        }

        log.warn("Request without Origin and Referer headers, method={}, path={}", request.getMethod(), request.getRequestURI());
        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String origin) {
        return allowedOrigins.contains(origin);
    }
}
