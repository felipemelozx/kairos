package com.felipemelozx.kairos.api;

import com.felipemelozx.kairos.dto.request.CreateTimeBlockRequest;
import com.felipemelozx.kairos.dto.request.UpdateTimeBlockRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.TimeBlockResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Time Blocks", description = "Time block management endpoints")
@SecurityRequirement(name = "cookieAuth")
public interface TimeBlockApi {

    @Operation(summary = "Create a time block", description = "Creates a time block owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Time block created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    ResponseEntity<ApiResponse<TimeBlockResponse>> create(
            CreateTimeBlockRequest request,
            UserDetails principal);

    @Operation(summary = "List time blocks", description = "Lists non-deleted time blocks owned by the authenticated user, optionally filtered by range")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Time blocks listed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid range filter"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    ResponseEntity<ApiResponse<List<TimeBlockResponse>>> list(
            Instant from,
            Instant to,
            UserDetails principal);

    @Operation(summary = "Get a time block", description = "Returns a non-deleted time block owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Time block found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Time block not found")
    })
    ResponseEntity<ApiResponse<TimeBlockResponse>> get(
            UUID blockId,
            UserDetails principal);

    @Operation(summary = "Update a time block", description = "Partially updates a time block owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Time block updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Time block or project not found")
    })
    ResponseEntity<ApiResponse<TimeBlockResponse>> update(
            UUID blockId,
            UpdateTimeBlockRequest request,
            UserDetails principal);

    @Operation(summary = "Delete a time block", description = "Soft deletes a time block owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Time block deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Time block not found")
    })
    ResponseEntity<ApiResponse<Void>> delete(
            UUID blockId,
            UserDetails principal);
}
