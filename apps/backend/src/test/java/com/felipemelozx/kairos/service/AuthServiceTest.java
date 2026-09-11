package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.dto.request.RegisterRequest;
import com.felipemelozx.kairos.dto.response.UserResponse;
import com.felipemelozx.kairos.entity.User;
import com.felipemelozx.kairos.entity.enums.AuthProvider;
import com.felipemelozx.kairos.exception.BusinessException;
import com.felipemelozx.kairos.repository.UserRepository;
import com.felipemelozx.kairos.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUserWhenEmailNotExists() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "John");
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            return u;
        });

        UserResponse result = authService.register(request);

        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.name()).isEqualTo("John");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("exists@example.com", "password123", "John");
        when(userRepository.existsByEmail("exists@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void shouldLoginWhenValidCredentials() {
        User user = createUser("test@example.com", "hashedPassword", "Test");
        when(userRepository.findByEmailAndActiveIsTrue("test@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        UserResponse result = authService.login(new com.felipemelozx.kairos.dto.request.LoginRequest("test@example.com", "password123"));

        assertThat(result.email()).isEqualTo("test@example.com");
    }

    @Test
    void shouldRejectLoginWhenInvalidPassword() {
        User user = createUser("test@example.com", "hashedPassword", "Test");
        when(userRepository.findByEmailAndActiveIsTrue("test@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new com.felipemelozx.kairos.dto.request.LoginRequest("test@example.com", "wrongPassword")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void shouldCreateUserOnFirstGoogleLogin() {
        when(userRepository.findByEmailAndActiveIsTrue("google@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            return u;
        });

        UserResponse result = authService.findOrCreateGoogleUser("google@example.com", "Google User", "https://picture.url");

        assertThat(result.email()).isEqualTo("google@example.com");
        assertThat(result.name()).isEqualTo("Google User");
        assertThat(result.provider()).isEqualTo("GOOGLE");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldLoginExistingUserOnGoogleLogin() {
        User existingUser = createUser("google@example.com", null, "Google User");
        existingUser.setProvider(AuthProvider.GOOGLE);
        when(userRepository.findByEmailAndActiveIsTrue("google@example.com")).thenReturn(Optional.of(existingUser));

        UserResponse result = authService.findOrCreateGoogleUser("google@example.com", "Google User", "https://picture.url");

        assertThat(result.email()).isEqualTo("google@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNormalizeEmailOnRegister() {
        RegisterRequest request = new RegisterRequest("  New@Example.COM  ", "password123", "John");
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            return u;
        });

        UserResponse result = authService.register(request);

        assertThat(result.email()).isEqualTo("new@example.com");
        verify(userRepository).existsByEmail("new@example.com");
    }

    @Test
    void shouldRejectRegisterWhenEmailExistsWithDifferentCase() {
        RegisterRequest request = new RegisterRequest("Example.com", "password123", "John");
        when(userRepository.existsByEmail("example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");

        verify(userRepository).existsByEmail("example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNormalizeEmailOnLogin() {
        User user = createUser("test@example.com", "hashedPassword", "Test");
        when(userRepository.findByEmailAndActiveIsTrue("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        UserResponse result = authService.login(
                new com.felipemelozx.kairos.dto.request.LoginRequest("  TEST@Example.COM  ", "password123"));

        assertThat(result.email()).isEqualTo("test@example.com");
        verify(userRepository).findByEmailAndActiveIsTrue("test@example.com");
    }

    @Test
    void shouldNormalizeEmailOnGoogleLogin() {
        when(userRepository.findByEmailAndActiveIsTrue("google@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            return u;
        });

        UserResponse result = authService.findOrCreateGoogleUser("  Google@Example.COM  ", "Google User", "https://picture.url");

        assertThat(result.email()).isEqualTo("google@example.com");
        verify(userRepository).findByEmailAndActiveIsTrue("google@example.com");
    }

    private User createUser(String email, String passwordHash, String name) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setName(name);
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        return user;
    }
}
