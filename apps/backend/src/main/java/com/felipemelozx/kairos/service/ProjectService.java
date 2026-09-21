package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.common.AppError;
import com.felipemelozx.kairos.common.ErrorCode;
import com.felipemelozx.kairos.common.Result;
import com.felipemelozx.kairos.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.dto.request.UpdateProjectRequest;
import com.felipemelozx.kairos.dto.response.ProjectResponse;
import com.felipemelozx.kairos.entity.Project;
import com.felipemelozx.kairos.entity.enums.ProjectStatus;
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
    public Result<ProjectResponse> create(UUID userId, CreateProjectRequest request) {
        log.info("Creating project for userId={}", userId);
        Result<Void> nameCheck = validateName(request.name());
        if (nameCheck instanceof Result.Err<Void> err) {
            return Result.err(err.error());
        }
        Result<Void> colorCheck = validateColor(request.color());
        if (colorCheck instanceof Result.Err<Void> err) {
            return Result.err(err.error());
        }

        Project project = new Project();
        project.setUserId(userId);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setColor(request.color());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedAt(Instant.now());

        Project saved = projectRepository.save(project);
        log.info("Project created: id={}, userId={}", saved.getId(), userId);
        return Result.ok(ProjectResponse.from(saved));
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
    public Result<ProjectResponse> getById(UUID id, UUID userId) {
        log.info("Fetching project: id={}, userId={}", id, userId);
        return projectRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .map(project -> Result.<ProjectResponse>ok(ProjectResponse.from(project)))
                .orElseGet(() -> Result.err(ErrorCode.NOT_FOUND, "Project not found"));
    }

    @Transactional
    public Result<ProjectResponse> update(UUID id, UUID userId, UpdateProjectRequest request) {
        log.info("Updating project: id={}, userId={}", id, userId);
        var maybeProject = projectRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId);
        if (maybeProject.isEmpty()) {
            return Result.err(ErrorCode.NOT_FOUND, "Project not found");
        }
        Project project = maybeProject.get();

        if (request.name() != null) {
            Result<Void> nameCheck = validateName(request.name());
            if (nameCheck instanceof Result.Err<Void> err) {
                return Result.err(err.error());
            }
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.color() != null) {
            Result<Void> colorCheck = validateColor(request.color());
            if (colorCheck instanceof Result.Err<Void> err) {
                return Result.err(err.error());
            }
            project.setColor(request.color());
        }
        if (request.status() != null) {
            project.setStatus(request.status());
        }

        Project saved = projectRepository.save(project);
        log.info("Project updated: id={}, userId={}", saved.getId(), userId);
        return Result.ok(ProjectResponse.from(saved));
    }

    @Transactional
    public Result<Void> softDelete(UUID id, UUID userId) {
        log.info("Deleting project: id={}, userId={}", id, userId);
        if (projectRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId).isEmpty()) {
            return Result.err(ErrorCode.NOT_FOUND, "Project not found");
        }

        projectRepository.softDeleteByIdAndUserId(id, userId, Instant.now());
        log.info("Project soft deleted: id={}, userId={}", id, userId);
        return Result.ok(null);
    }

    private Result<Void> validateName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            return Result.err(AppError.of(ErrorCode.INVALID_PROJECT_NAME,
                    "Project name must be between 1 and " + MAX_NAME_LENGTH + " characters"));
        }
        return Result.ok(null);
    }

    private Result<Void> validateColor(String color) {
        if (color == null || !COLOR_PATTERN.matcher(color).matches()) {
            return Result.err(ErrorCode.INVALID_COLOR);
        }
        return Result.ok(null);
    }
}
