package com.felipemelozx.kairos.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OriginValidationFilterTest {

    private OriginValidationFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new OriginValidationFilter(List.of("http://localhost:3000", "https://kairos.example.com"));
    }

    @Test
    void shouldAllowGetRequestWithoutOriginCheck() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowOptionsRequestWithoutOriginCheck() throws Exception {
        when(request.getMethod()).thenReturn("OPTIONS");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowPostRequestWhenOriginInAllowlist() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Origin")).thenReturn("http://localhost:3000");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectPostRequestWhenOriginNotInAllowlist() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Origin")).thenReturn("http://evil.com");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(403, "Invalid origin");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldAllowPostRequestWhenOriginAbsentAndRefererInAllowlist() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Origin")).thenReturn(null);
        when(request.getHeader("Referer")).thenReturn("http://localhost:3000/some/page");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectPostRequestWhenOriginAbsentAndRefererNotInAllowlist() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Origin")).thenReturn(null);
        when(request.getHeader("Referer")).thenReturn("http://evil.com/page");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(403, "Invalid origin");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldAllowPostRequestWhenBothOriginAndRefererAbsent() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Origin")).thenReturn(null);
        when(request.getHeader("Referer")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectPutRequestWhenOriginNotInAllowlist() throws Exception {
        when(request.getMethod()).thenReturn("PUT");
        when(request.getHeader("Origin")).thenReturn("http://evil.com");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(403, "Invalid origin");
    }

    @Test
    void shouldRejectDeleteRequestWhenOriginNotInAllowlist() throws Exception {
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getHeader("Origin")).thenReturn("http://evil.com");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(403, "Invalid origin");
    }
}
