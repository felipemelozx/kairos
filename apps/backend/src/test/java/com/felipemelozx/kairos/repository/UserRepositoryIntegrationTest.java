package com.felipemelozx.kairos.repository;

import com.felipemelozx.kairos.entity.User;
import com.felipemelozx.kairos.entity.enums.AuthProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.8-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setPasswordHash("hashed");
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setCreatedAt(Instant.now());

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmailAndActiveIsTrue("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    void shouldNotFindInactiveUser() {
        User user = new User();
        user.setEmail("inactive@example.com");
        user.setName("Inactive");
        user.setPasswordHash("hashed");
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(false);
        user.setCreatedAt(Instant.now());

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmailAndActiveIsTrue("inactive@example.com");
        assertThat(found).isEmpty();
    }
}
