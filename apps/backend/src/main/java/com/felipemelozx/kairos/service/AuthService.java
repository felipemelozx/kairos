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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
