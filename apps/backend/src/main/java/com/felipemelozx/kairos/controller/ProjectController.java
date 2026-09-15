package com.felipemelozx.kairos.controller;

import com.felipemelozx.kairos.api.ProjectApi;
import com.felipemelozx.kairos.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.dto.request.UpdateProjectRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.ProjectResponse;
import com.felipemelozx.kairos.exception.BusinessException;
import com.felipemelozx.kairos.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController implements ProjectApi {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public ResponseEntity<ApiResponse<ProjectResponse>> create(CreateProjectRequest request, UserDetails principal) {
        UUID userId = currentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(projectService.create(userId, request)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> list(UserDetails principal) {
        UUID userId = currentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(projectService.listByUser(userId)));
    }

    @Override
    public ResponseEntity<ApiResponse<ProjectResponse>> get(UUID projectId, UserDetails principal) {
        UUID userId = currentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(projectService.getById(projectId, userId)));
    }

    @Override
    public ResponseEntity<ApiResponse<ProjectResponse>> update(UUID projectId, UpdateProjectRequest request,
                                                               UserDetails principal) {
        UUID userId = currentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(projectService.update(projectId, userId, request)));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> delete(UUID projectId, UserDetails principal) {
        UUID userId = currentUserId(principal);
        projectService.softDelete(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private UUID currentUserId(UserDetails principal) {
        if (principal == null || principal.getUsername() == null) {
            throw new BusinessException("UNAUTHORIZED", "Not authenticated");
        }
        try {
            return UUID.fromString(principal.getUsername());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("UNAUTHORIZED", "Not authenticated");
        }
    }
}
