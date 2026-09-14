package com.felipemelozx.kairos.api;

import com.felipemelozx.kairos.dto.request.CreateProjectRequest;
import com.felipemelozx.kairos.dto.request.UpdateProjectRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.ProjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@Tag(name = "Projects", description = "Project management endpoints")
@SecurityRequirement(name = "cookieAuth")
public interface ProjectApi {

    @PostMapping
    @Operation(summary = "Create a project", description = "Creates a project owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Project created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    ResponseEntity<ApiResponse<ProjectResponse>> create(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserDetails principal);

    @GetMapping
    @Operation(summary = "List projects", description = "Lists all non-deleted projects owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Projects listed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    ResponseEntity<ApiResponse<List<ProjectResponse>>> list(@AuthenticationPrincipal UserDetails principal);

    @GetMapping("/{projectId}")
    @Operation(summary = "Get a project", description = "Returns a non-deleted project owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Project found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    ResponseEntity<ApiResponse<ProjectResponse>> get(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails principal);

    @PatchMapping("/{projectId}")
    @Operation(summary = "Update a project", description = "Partially updates a project owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Project updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    ResponseEntity<ApiResponse<ProjectResponse>> update(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserDetails principal);

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete a project", description = "Soft deletes a project owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Project deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails principal);
}
