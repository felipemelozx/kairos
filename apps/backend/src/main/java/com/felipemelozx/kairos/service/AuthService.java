package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.dto.request.LoginRequest;
import com.felipemelozx.kairos.dto.request.RegisterRequest;
import com.felipemelozx.kairos.dto.response.UserResponse;
import com.felipemelozx.kairos.entity.User;
import com.felipemelozx.kairos.entity.enums.AuthProvider;
import com.felipemelozx.kairos.exception.BusinessException;
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
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("EMAIL_EXISTS", "Email already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setCreatedAt(Instant.now());

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndActiveIsTrue(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        return UserResponse.from(user);
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
    public RotatedSession rotateRefreshToken(String refreshToken) {
        if (refreshToken == null || !jwtService.validateToken(refreshToken)
                || !jwtService.isRefreshToken(refreshToken)) {
            throw new BusinessException("UNAUTHORIZED", "Refresh token expired or invalid");
        }
        String userId = jwtService.getUserIdFromToken(refreshToken);
        int tokenVersion = jwtService.getTokenVersionFromToken(refreshToken);
        java.util.UUID id;
        try {
            id = java.util.UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("UNAUTHORIZED", "Refresh token expired or invalid");
        }
        User user = userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", "Refresh token expired or invalid"));
        int currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        if (tokenVersion != currentVersion) {
            throw new BusinessException("UNAUTHORIZED", "Refresh token expired or invalid");
        }
        int nextVersion = currentVersion + 1;
        user.setTokenVersion(nextVersion);
        userRepository.save(user);
        return new RotatedSession(id, nextVersion);
    }

    @Transactional(readOnly = true)
    public User findUserById(java.util.UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "User not found"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
