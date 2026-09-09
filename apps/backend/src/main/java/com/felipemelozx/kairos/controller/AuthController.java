package com.felipemelozx.kairos.controller;

import com.felipemelozx.kairos.api.AuthApi;
import com.felipemelozx.kairos.dto.request.LoginRequest;
import com.felipemelozx.kairos.dto.request.RegisterRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.UserResponse;
import com.felipemelozx.kairos.entity.User;
import com.felipemelozx.kairos.exception.BusinessException;
import com.felipemelozx.kairos.repository.UserRepository;
import com.felipemelozx.kairos.security.CookieUtils;
import com.felipemelozx.kairos.security.jwt.JwtService;
import com.felipemelozx.kairos.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
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

    public AuthController(AuthService authService, JwtService jwtService, UserRepository userRepository) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public ResponseEntity<ApiResponse<UserResponse>> register(
            RegisterRequest request,
            HttpServletResponse response) {
        UserResponse user = authService.register(request);

        UUID userId = user.id();
        String accessToken = jwtService.generateAccessToken(userId);
        String refreshToken = jwtService.generateRefreshToken(userId);
        CookieUtils.addAccessTokenCookie(response, accessToken);
        CookieUtils.addRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
    }

    @Override
    public ResponseEntity<ApiResponse<UserResponse>> login(
            LoginRequest request,
            HttpServletResponse response) {
        UserResponse user = authService.login(request);

        UUID userId = user.id();
        String accessToken = jwtService.generateAccessToken(userId);
        String refreshToken = jwtService.generateRefreshToken(userId);
        CookieUtils.addAccessTokenCookie(response, accessToken);
        CookieUtils.addRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        CookieUtils.clearCookies(response);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> refresh(
            String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || !jwtService.validateToken(refreshToken)) {
            throw new BusinessException("UNAUTHORIZED", "Refresh token expired or invalid");
        }

        String userId = jwtService.getUserIdFromToken(refreshToken);
        String newAccessToken = jwtService.generateAccessToken(UUID.fromString(userId));
        CookieUtils.addAccessTokenCookie(response, newAccessToken);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    public ResponseEntity<ApiResponse<UserResponse>> me(UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "User not found"));
        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)));
    }
}
