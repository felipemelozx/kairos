package com.felipemelozx.kairos.repository;

import com.felipemelozx.kairos.entity.TimeBlock;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TimeBlockRepositoryIntegrationTest {

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
    private TimeBlockRepository timeBlockRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindBlockByUser() {
        User user = createUser("tb-owner@example.com");
        TimeBlock saved = timeBlockRepository.save(newBlock(user.getId(), "Deep Work",
                Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T10:00:00Z")));

        List<TimeBlock> found = timeBlockRepository.findByUserIdAndDeletedAtIsNullOrderByStartDateTimeAsc(user.getId());

        assertThat(found).extracting(TimeBlock::getId).containsExactly(saved.getId());
    }

    @Test
    void shouldNotFindSoftDeletedBlock() {
        User user = createUser("tb-softdelete@example.com");
        TimeBlock saved = timeBlockRepository.save(newBlock(user.getId(), "Gone",
                Instant.parse("2026-10-02T09:00:00Z"), Instant.parse("2026-10-02T10:00:00Z")));
        timeBlockRepository.flush();

        timeBlockRepository.softDeleteByIdAndUserId(saved.getId(), user.getId(), Instant.now());

        assertThat(timeBlockRepository.findByUserIdAndDeletedAtIsNullOrderByStartDateTimeAsc(user.getId())).isEmpty();
        assertThat(timeBlockRepository.findByIdAndUserIdAndDeletedAtIsNull(saved.getId(), user.getId())).isEmpty();
    }

    @Test
    void shouldNotFindBlockOfAnotherUser() {
        User owner = createUser("tb-owner2@example.com");
        User other = createUser("tb-other@example.com");
        TimeBlock saved = timeBlockRepository.save(newBlock(owner.getId(), "Private",
                Instant.parse("2026-10-03T09:00:00Z"), Instant.parse("2026-10-03T10:00:00Z")));

        assertThat(timeBlockRepository.findByUserIdAndDeletedAtIsNullOrderByStartDateTimeAsc(other.getId())).isEmpty();
        assertThat(timeBlockRepository.findByIdAndUserIdAndDeletedAtIsNull(saved.getId(), other.getId())).isEmpty();
    }

    @Test
    void shouldListBlocksInRangeOrdered() {
        User user = createUser("tb-range@example.com");
        TimeBlock early = timeBlockRepository.save(newBlock(user.getId(), "Early",
                Instant.parse("2026-10-05T09:00:00Z"), Instant.parse("2026-10-05T10:00:00Z")));
        TimeBlock late = timeBlockRepository.save(newBlock(user.getId(), "Late",
                Instant.parse("2026-10-06T09:00:00Z"), Instant.parse("2026-10-06T10:00:00Z")));
        timeBlockRepository.save(newBlock(user.getId(), "Outside",
                Instant.parse("2026-11-01T09:00:00Z"), Instant.parse("2026-11-01T10:00:00Z")));

        List<TimeBlock> found = timeBlockRepository
                .findByUserIdAndStartDateTimeGreaterThanEqualAndStartDateTimeLessThanAndDeletedAtIsNullOrderByStartDateTimeAsc(
                        user.getId(), Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-10T00:00:00Z"));

        assertThat(found).extracting(TimeBlock::getId).containsExactly(early.getId(), late.getId());
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

    private TimeBlock newBlock(UUID userId, String title, Instant start, Instant end) {
        TimeBlock block = new TimeBlock();
        block.setUserId(userId);
        block.setTitle(title);
        block.setStartDateTime(start);
        block.setEndDateTime(end);
        block.setOverride(false);
        block.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        return block;
    }
}
