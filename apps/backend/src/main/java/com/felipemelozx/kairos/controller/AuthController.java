package com.felipemelozx.kairos.controller;

import com.felipemelozx.kairos.api.AuthApi;
import com.felipemelozx.kairos.common.AppError;
import com.felipemelozx.kairos.common.Result;
import com.felipemelozx.kairos.dto.request.LoginRequest;
import com.felipemelozx.kairos.dto.request.RegisterRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.UserResponse;
import com.felipemelozx.kairos.security.AuthCookieService;
import com.felipemelozx.kairos.security.jwt.JwtService;
import com.felipemelozx.kairos.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthCookieService authCookieService;

    public AuthController(AuthService authService, JwtService jwtService,
                          AuthCookieService authCookieService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.authCookieService = authCookieService;
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        return authService.register(request).fold(
                user -> {
                    authCookieService.issueAuthCookies(response, user.id(), 0);
                    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
                },
                AppError::toResponse);
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        return authService.login(request).fold(
                user -> {
                    authCookieService.issueAuthCookies(response, user.id());
                    return ResponseEntity.ok(ApiResponse.success(user));
                },
                AppError::toResponse);
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails principal,
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken,
            HttpServletResponse response) {
        UUID userId = resolveUserId(principal, refreshToken);
        if (userId != null) {
            authService.invalidateSession(userId);
        }
        authCookieService.clearAuthCookies(response);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken,
            HttpServletResponse response) {
        return authService.rotateRefreshToken(refreshToken).fold(
                session -> {
                    authCookieService.issueAuthCookies(response, session.userId(), session.tokenVersion());
                    return ResponseEntity.ok(ApiResponse.success(null));
                },
                AppError::toResponse);
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());
        return authService.findUserById(userId).fold(
                user -> ResponseEntity.ok(ApiResponse.success(UserResponse.from(user))),
                AppError::toResponse);
    }

    private UUID resolveUserId(UserDetails principal, String refreshToken) {
        if (principal != null) {
            try {
                return UUID.fromString(principal.getUsername());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        if (refreshToken != null && jwtService.validateToken(refreshToken)
                && jwtService.isRefreshToken(refreshToken)) {
            try {
                return UUID.fromString(jwtService.getUserIdFromToken(refreshToken));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
}
