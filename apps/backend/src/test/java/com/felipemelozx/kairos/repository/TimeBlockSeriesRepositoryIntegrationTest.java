package com.felipemelozx.kairos.repository;

import com.felipemelozx.kairos.entity.TimeBlockSeries;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TimeBlockSeriesRepositoryIntegrationTest {

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
    private TimeBlockSeriesRepository seriesRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindSeriesByUser() {
        User user = createUser("series-owner@example.com");
        TimeBlockSeries saved = seriesRepository.save(newSeries(user.getId(), "Ingles"));

        List<TimeBlockSeries> found = seriesRepository.findByUserIdAndDeletedAtIsNullOrderByStartDateAsc(user.getId());

        assertThat(found).extracting(TimeBlockSeries::getId).containsExactly(saved.getId());
    }

    @Test
    void shouldNotFindSoftDeletedSeries() {
        User user = createUser("series-softdelete@example.com");
        TimeBlockSeries saved = seriesRepository.save(newSeries(user.getId(), "Gone"));
        seriesRepository.flush();

        seriesRepository.softDeleteByIdAndUserId(saved.getId(), user.getId(), Instant.now());

        assertThat(seriesRepository.findByUserIdAndDeletedAtIsNullOrderByStartDateAsc(user.getId())).isEmpty();
        assertThat(seriesRepository.findByIdAndUserIdAndDeletedAtIsNull(saved.getId(), user.getId())).isEmpty();
    }

    @Test
    void shouldNotFindSeriesOfAnotherUser() {
        User owner = createUser("series-owner2@example.com");
        User other = createUser("series-other@example.com");
        TimeBlockSeries saved = seriesRepository.save(newSeries(owner.getId(), "Private"));

        assertThat(seriesRepository.findByUserIdAndDeletedAtIsNullOrderByStartDateAsc(other.getId())).isEmpty();
        assertThat(seriesRepository.findByIdAndUserIdAndDeletedAtIsNull(saved.getId(), other.getId())).isEmpty();
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setName("Test User");
        user.setPasswordHash("hashed");
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    private TimeBlockSeries newSeries(java.util.UUID userId, String title) {
        TimeBlockSeries series = new TimeBlockSeries();
        series.setUserId(userId);
        series.setTitle(title);
        series.setStartDate(LocalDate.parse("2026-10-01"));
        series.setPattern("{\"type\":\"DAILY\",\"time\":\"09:00\",\"durationMinutes\":60}");
        series.setCreatedAt(Instant.now());
        return series;
    }
}
