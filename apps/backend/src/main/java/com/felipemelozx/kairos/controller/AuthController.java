package com.felipemelozx.kairos.controller;

import com.felipemelozx.kairos.api.AuthApi;
import com.felipemelozx.kairos.dto.request.LoginRequest;
import com.felipemelozx.kairos.dto.request.RegisterRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.UserResponse;
import com.felipemelozx.kairos.entity.User;
import com.felipemelozx.kairos.exception.BusinessException;
import com.felipemelozx.kairos.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final AuthCookieService authCookieService;

    public AuthController(AuthService authService, JwtService jwtService, UserRepository userRepository,
                          AuthCookieService authCookieService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authCookieService = authCookieService;
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        UserResponse user = authService.register(request);

        authCookieService.issueAuthCookies(response, user.id(), 0);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        UserResponse user = authService.login(request);

        authCookieService.issueAuthCookies(response, user.id());

        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails principal,
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken,
            HttpServletResponse response) {
        UUID userId = resolveUserId(principal, refreshToken);
        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
                int current = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
                user.setTokenVersion(current + 1);
                userRepository.save(user);
            });
        }
        authCookieService.clearAuthCookies(response);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || !jwtService.validateToken(refreshToken)
                || !jwtService.isRefreshToken(refreshToken)) {
            throw new BusinessException("UNAUTHORIZED", "Refresh token expired or invalid");
        }

        String userId = jwtService.getUserIdFromToken(refreshToken);
        int tokenVersion = jwtService.getTokenVersionFromToken(refreshToken);
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("UNAUTHORIZED", "Refresh token expired or invalid");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", "Refresh token expired or invalid"));
        int currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        if (tokenVersion != currentVersion) {
            throw new BusinessException("UNAUTHORIZED", "Refresh token expired or invalid");
        }
        authCookieService.issueAuthCookies(response, id, currentVersion);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "User not found"));
        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)));
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
