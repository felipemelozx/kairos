package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.dto.request.UpdateProjectRequest;
import com.felipemelozx.kairos.dto.response.ProjectResponse;
import com.felipemelozx.kairos.entity.Project;
import com.felipemelozx.kairos.entity.enums.ProjectStatus;
import com.felipemelozx.kairos.exception.BusinessException;
import com.felipemelozx.kairos.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void shouldCreateProjectForCurrentUser() {
        UUID userId = UUID.randomUUID();
        CreateProjectRequest request = new CreateProjectRequest("Study English", "Grammar", "#1A2B3C");
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(UUID.randomUUID());
            return project;
        });

        ProjectResponse result = projectService.create(userId, request);

        assertThat(result.name()).isEqualTo("Study English");
        assertThat(result.description()).isEqualTo("Grammar");
        assertThat(result.color()).isEqualTo("#1A2B3C");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.userId()).isEqualTo(userId);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void shouldRejectBlankProjectName() {
        UUID userId = UUID.randomUUID();
        CreateProjectRequest request = new CreateProjectRequest("   ", null, "#1A2B3C");

        assertThatThrownBy(() -> projectService.create(userId, request))
                .isInstanceOf(BusinessException.class);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void shouldRejectMissingColor() {
        UUID userId = UUID.randomUUID();
        CreateProjectRequest request = new CreateProjectRequest("Study", null, null);

        assertThatThrownBy(() -> projectService.create(userId, request))
                .isInstanceOf(BusinessException.class);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void shouldRejectInvalidHexColor() {
        UUID userId = UUID.randomUUID();
        CreateProjectRequest request = new CreateProjectRequest("Study", null, "red");

        assertThatThrownBy(() -> projectService.create(userId, request))
                .isInstanceOf(BusinessException.class);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void shouldAcceptReservedLookingName() {
        UUID userId = UUID.randomUUID();
        CreateProjectRequest request = new CreateProjectRequest("inbox", null, "#FFFFFF");
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(UUID.randomUUID());
            return project;
        });

        ProjectResponse result = projectService.create(userId, request);

        assertThat(result.name()).isEqualTo("inbox");
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void shouldListOnlyCurrentUserProjects() {
        UUID userId = UUID.randomUUID();
        Project active = project(userId, ProjectStatus.ACTIVE);
        Project archived = project(userId, ProjectStatus.ARCHIVED);
        when(projectRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(List.of(active, archived));

        List<ProjectResponse> result = projectService.listByUser(userId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProjectResponse::status).containsExactlyInAnyOrder("ACTIVE", "ARCHIVED");
        verify(projectRepository).findByUserIdAndDeletedAtIsNull(userId);
    }

    @Test
    void shouldGetProjectByIdForCurrentUser() {
        UUID userId = UUID.randomUUID();
        Project project = project(userId, ProjectStatus.ACTIVE);
        when(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(project.getId(), userId))
                .thenReturn(Optional.of(project));

        ProjectResponse result = projectService.getById(project.getId(), userId);

        assertThat(result.id()).isEqualTo(project.getId());
    }

    @Test
    void shouldThrowNotFoundWhenGettingOtherUsersProject() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById(projectId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("NOT_FOUND"));
    }

    @Test
    void shouldPartiallyUpdateProjectKeepingOmittedFields() {
        UUID userId = UUID.randomUUID();
        Project project = project(userId, ProjectStatus.ACTIVE);
        when(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(project.getId(), userId))
                .thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse result = projectService.update(project.getId(), userId,
                new UpdateProjectRequest("Renamed", null, null, null));

        assertThat(result.name()).isEqualTo("Renamed");
        assertThat(result.description()).isEqualTo("A description");
        assertThat(result.color()).isEqualTo("#1A2B3C");
        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldArchiveProjectViaUpdate() {
        UUID userId = UUID.randomUUID();
        Project project = project(userId, ProjectStatus.ACTIVE);
        when(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(project.getId(), userId))
                .thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse result = projectService.update(project.getId(), userId,
                new UpdateProjectRequest(null, null, null, ProjectStatus.ARCHIVED));

        assertThat(result.status()).isEqualTo("ARCHIVED");
    }

    @Test
    void shouldSoftDeleteOwnProject() {
        UUID userId = UUID.randomUUID();
        Project project = project(userId, ProjectStatus.ACTIVE);
        when(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(project.getId(), userId))
                .thenReturn(Optional.of(project));

        projectService.softDelete(project.getId(), userId);

        verify(projectRepository).softDeleteByIdAndUserId(eq(project.getId()), eq(userId), any(Instant.class));
    }

    @Test
    void shouldThrowNotFoundWhenDeletingOtherUsersProject() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.softDelete(projectId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("NOT_FOUND"));
        verify(projectRepository, never()).softDeleteByIdAndUserId(any(UUID.class), any(UUID.class), any(Instant.class));
    }

    private Project project(UUID userId, ProjectStatus status) {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setUserId(userId);
        project.setName("Study");
        project.setDescription("A description");
        project.setColor("#1A2B3C");
        project.setStatus(status);
        project.setCreatedAt(Instant.now());
        return project;
    }
}
