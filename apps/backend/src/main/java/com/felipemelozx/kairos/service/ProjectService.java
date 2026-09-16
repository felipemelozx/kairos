package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.dto.request.UpdateProjectRequest;
import com.felipemelozx.kairos.dto.response.ProjectResponse;
import com.felipemelozx.kairos.entity.Project;
import com.felipemelozx.kairos.entity.enums.ProjectStatus;
import com.felipemelozx.kairos.exception.BusinessException;
import com.felipemelozx.kairos.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private static final int MAX_NAME_LENGTH = 100;
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectResponse create(UUID userId, CreateProjectRequest request) {
        log.info("Creating project for userId={}", userId);
        validateName(request.name());
        validateColor(request.color());

        Project project = new Project();
        project.setUserId(userId);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setColor(request.color());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedAt(Instant.now());

        Project saved = projectRepository.save(project);
        log.info("Project created: id={}, userId={}", saved.getId(), userId);
        return ProjectResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listByUser(UUID userId) {
        log.info("Listing projects for userId={}", userId);
        List<ProjectResponse> result = projectRepository.findByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .map(ProjectResponse::from)
                .toList();
        log.info("Listed {} projects for userId={}", result.size(), userId);
        return result;
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(UUID id, UUID userId) {
        log.info("Fetching project: id={}, userId={}", id, userId);
        Project project = projectRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Project not found"));
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(UUID id, UUID userId, UpdateProjectRequest request) {
        log.info("Updating project: id={}, userId={}", id, userId);
        Project project = projectRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Project not found"));

        if (request.name() != null) {
            validateName(request.name());
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.color() != null) {
            validateColor(request.color());
            project.setColor(request.color());
        }
        if (request.status() != null) {
            project.setStatus(request.status());
        }

        Project saved = projectRepository.save(project);
        log.info("Project updated: id={}, userId={}", saved.getId(), userId);
        return ProjectResponse.from(saved);
    }

    @Transactional
    public void softDelete(UUID id, UUID userId) {
        log.info("Deleting project: id={}, userId={}", id, userId);
        projectRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Project not found"));

        projectRepository.softDeleteByIdAndUserId(id, userId, Instant.now());
        log.info("Project soft deleted: id={}, userId={}", id, userId);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException("INVALID_PROJECT_NAME",
                    "Project name must be between 1 and " + MAX_NAME_LENGTH + " characters");
        }
    }

    private void validateColor(String color) {
        if (color == null || !COLOR_PATTERN.matcher(color).matches()) {
            throw new BusinessException("INVALID_COLOR", "Color must be a valid hex color (#RRGGBB)");
        }
    }
}
