package com.felipemelozx.kairos.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.Arrays;

public class CookieUtils {

    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN";
    private static final String CSRF_TOKEN_COOKIE = "CSRF_TOKEN";
    private static final String SAME_SITE_STRICT = "Strict";
    // Shared parent path so the cookie is sent to both /api/auth/refresh
    // and /api/auth/logout (a narrower path would hide it from logout,
    // leaving a stolen refresh token valid after logout).
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    public static void addAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
            .httpOnly(true)
            .secure(true)
            .sameSite(SAME_SITE_STRICT)
            .path("/")
            .maxAge(900) // 15 minutes
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
            .httpOnly(true)
            .secure(true)
            .sameSite(SAME_SITE_STRICT)
            .path(REFRESH_COOKIE_PATH)
            .maxAge(604800) // 7 days
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void addCsrfTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(CSRF_TOKEN_COOKIE, token)
            .httpOnly(false)
            .secure(true)
            .sameSite(SAME_SITE_STRICT)
            .path("/")
            .maxAge(900)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void clearCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
            .httpOnly(true)
            .secure(true)
            .sameSite(SAME_SITE_STRICT)
            .path("/")
            .maxAge(0)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
            .httpOnly(true)
            .secure(true)
            .sameSite(SAME_SITE_STRICT)
            .path(REFRESH_COOKIE_PATH)
            .maxAge(0)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        ResponseCookie csrfCookie = ResponseCookie.from(CSRF_TOKEN_COOKIE, "")
            .httpOnly(false)
            .secure(true)
            .sameSite(SAME_SITE_STRICT)
            .path("/")
            .maxAge(0)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());
    }

    public static String getAccessTokenFromCookies(Cookie[] cookies) {
        if (cookies == null) return null;
        return Arrays.stream(cookies)
            .filter(c -> ACCESS_TOKEN_COOKIE.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }

    public static String getRefreshTokenFromCookies(Cookie[] cookies) {
        if (cookies == null) return null;
        return Arrays.stream(cookies)
            .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }
}
