package com.felipemelozx.kairos.repository;

import com.felipemelozx.kairos.entity.Project;
import com.felipemelozx.kairos.entity.User;
import com.felipemelozx.kairos.entity.enums.AuthProvider;
import com.felipemelozx.kairos.entity.enums.ProjectStatus;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ProjectRepositoryIntegrationTest {

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
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindActiveProjectByUser() {
        User user = createUser("owner@example.com");
        Project saved = projectRepository.save(newProject(user.getId(), "Study English", ProjectStatus.ACTIVE));

        List<Project> found = projectRepository.findByUserIdAndDeletedAtIsNull(user.getId());

        assertThat(found).extracting(Project::getId).containsExactly(saved.getId());
    }

    @Test
    void shouldNotFindSoftDeletedProject() {
        User user = createUser("softdelete@example.com");
        Project saved = projectRepository.save(newProject(user.getId(), "Archived", ProjectStatus.ACTIVE));
        projectRepository.flush();

        projectRepository.softDeleteByIdAndUserId(saved.getId(), user.getId(), Instant.now());

        assertThat(projectRepository.findByUserIdAndDeletedAtIsNull(user.getId())).isEmpty();
        assertThat(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(saved.getId(), user.getId())).isEmpty();
    }

    @Test
    void shouldNotFindProjectOfAnotherUser() {
        User owner = createUser("owner2@example.com");
        User other = createUser("other@example.com");
        Project saved = projectRepository.save(newProject(owner.getId(), "Private", ProjectStatus.ACTIVE));

        assertThat(projectRepository.findByUserIdAndDeletedAtIsNull(other.getId())).isEmpty();
        assertThat(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(saved.getId(), other.getId())).isEmpty();
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

    private Project newProject(UUID userId, String name, ProjectStatus status) {
        Project project = new Project();
        project.setUserId(userId);
        project.setName(name);
        project.setDescription("A description");
        project.setColor("#1A2B3C");
        project.setStatus(status);
        project.setCreatedAt(Instant.now());
        return project;
    }
}
