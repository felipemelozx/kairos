package com.felipemelozx.kairos.api;

import com.felipemelozx.kairos.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.dto.request.UpdateProjectRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.ProjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

@Tag(name = "Projects", description = "Project management endpoints")
@SecurityRequirement(name = "cookieAuth")
public interface ProjectApi {

    @Operation(summary = "Create a project", description = "Creates a project owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Project created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    ResponseEntity<ApiResponse<ProjectResponse>> create(
            CreateProjectRequest request,
            UserDetails principal);

    @Operation(summary = "List projects", description = "Lists all non-deleted projects owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Projects listed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    ResponseEntity<ApiResponse<List<ProjectResponse>>> list(UserDetails principal);

    @Operation(summary = "Get a project", description = "Returns a non-deleted project owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Project found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    ResponseEntity<ApiResponse<ProjectResponse>> get(
            UUID projectId,
            UserDetails principal);

    @Operation(summary = "Update a project", description = "Partially updates a project owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Project updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    ResponseEntity<ApiResponse<ProjectResponse>> update(
            UUID projectId,
            UpdateProjectRequest request,
            UserDetails principal);

    @Operation(summary = "Delete a project", description = "Soft deletes a project owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Project deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    ResponseEntity<ApiResponse<Void>> delete(
            UUID projectId,
            UserDetails principal);
}
