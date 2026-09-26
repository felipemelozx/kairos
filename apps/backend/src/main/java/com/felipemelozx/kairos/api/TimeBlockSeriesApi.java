package com.felipemelozx.kairos.api;

import com.felipemelozx.kairos.dto.request.CreateSeriesRequest;
import com.felipemelozx.kairos.dto.request.UpdateSeriesRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.SeriesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

@Tag(name = "Time Block Series", description = "Recurring series management endpoints")
@SecurityRequirement(name = "cookieAuth")
public interface TimeBlockSeriesApi {

    @Operation(summary = "Create a series", description = "Creates a recurring series owned by the authenticated user and materializes its occurrences")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Series created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pattern or range"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    ResponseEntity<ApiResponse<SeriesResponse>> create(
            CreateSeriesRequest request,
            UserDetails principal);

    @Operation(summary = "List series", description = "Lists non-deleted series owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Series listed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    ResponseEntity<ApiResponse<List<SeriesResponse>>> list(
            UserDetails principal);

    @Operation(summary = "Get a series", description = "Returns a non-deleted series owned by the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Series found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Series not found")
    })
    ResponseEntity<ApiResponse<SeriesResponse>> get(
            UUID seriesId,
            UserDetails principal);

    @Operation(summary = "Split a series forward", description = "Closes the current series the day before effectiveFrom and creates a new series with the new rule; history is never rewritten")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Series split successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid effectiveFrom, pattern or range"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Series or project not found")
    })
    ResponseEntity<ApiResponse<SeriesResponse>> update(
            UUID seriesId,
            UpdateSeriesRequest request,
            UserDetails principal);

    @Operation(summary = "Delete a series", description = "Soft deletes a series and its future occurrences; past occurrences are preserved")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Series deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Series not found")
    })
    ResponseEntity<ApiResponse<Void>> delete(
            UUID seriesId,
            UserDetails principal);
}
