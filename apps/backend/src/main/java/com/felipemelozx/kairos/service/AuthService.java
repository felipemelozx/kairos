package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.common.ErrorCode;
import com.felipemelozx.kairos.common.Result;
import com.felipemelozx.kairos.dto.request.LoginRequest;
import com.felipemelozx.kairos.dto.request.RegisterRequest;
import com.felipemelozx.kairos.dto.response.UserResponse;
import com.felipemelozx.kairos.entity.User;
import com.felipemelozx.kairos.entity.enums.AuthProvider;
import com.felipemelozx.kairos.repository.UserRepository;
import com.felipemelozx.kairos.security.jwt.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public Result<UserResponse> register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            return Result.err(ErrorCode.EMAIL_EXISTS);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setCreatedAt(Instant.now());

        User saved = userRepository.save(user);
        return Result.ok(UserResponse.from(saved));
    }

    @Transactional(readOnly = true)
    public Result<UserResponse> login(LoginRequest request) {
        Optional<User> maybeUser = userRepository.findByEmailAndActiveIsTrue(normalizeEmail(request.email()));
        if (maybeUser.isEmpty()) {
            return Result.err(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = maybeUser.get();

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            return Result.err(ErrorCode.INVALID_CREDENTIALS);
        }

        return Result.ok(UserResponse.from(user));
    }

    @Transactional
    public UserResponse findOrCreateGoogleUser(String email, String name, String picture) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> existingUser = userRepository.findByEmailAndActiveIsTrue(normalizedEmail);

        if (existingUser.isPresent()) {
            return UserResponse.from(existingUser.get());
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setName(name);
        user.setAvatarUrl(picture);
        user.setProvider(AuthProvider.GOOGLE);
        user.setActive(true);
        user.setCreatedAt(Instant.now());

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    @Transactional
    public void invalidateSession(java.util.UUID userId) {
        userRepository.findByIdForUpdate(userId).ifPresent(user -> {
            int current = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
            user.setTokenVersion(current + 1);
            userRepository.save(user);
        });
    }

    public record RotatedSession(java.util.UUID userId, int tokenVersion) {}

    @Transactional
    public Result<RotatedSession> rotateRefreshToken(String refreshToken) {
        if (refreshToken == null || !jwtService.validateToken(refreshToken)
                || !jwtService.isRefreshToken(refreshToken)) {
            return Result.err(ErrorCode.UNAUTHORIZED, "Refresh token expired or invalid");
        }
        String userId = jwtService.getUserIdFromToken(refreshToken);
        int tokenVersion = jwtService.getTokenVersionFromToken(refreshToken);
        java.util.UUID id;
        try {
            id = java.util.UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return Result.err(ErrorCode.UNAUTHORIZED, "Refresh token expired or invalid");
        }
        Optional<User> maybeUser = userRepository.findByIdForUpdate(id);
        if (maybeUser.isEmpty()) {
            return Result.err(ErrorCode.UNAUTHORIZED, "Refresh token expired or invalid");
        }
        User user = maybeUser.get();
        int currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        if (tokenVersion != currentVersion) {
            return Result.err(ErrorCode.UNAUTHORIZED, "Refresh token expired or invalid");
        }
        int nextVersion = currentVersion + 1;
        user.setTokenVersion(nextVersion);
        userRepository.save(user);
        return Result.ok(new RotatedSession(id, nextVersion));
    }

    @Transactional(readOnly = true)
    public Result<User> findUserById(java.util.UUID userId) {
        return userRepository.findById(userId)
                .map(Result::<User>ok)
                .orElseGet(() -> Result.err(ErrorCode.NOT_FOUND, "User not found"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
