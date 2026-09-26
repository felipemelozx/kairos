package com.felipemelozx.kairos.controller;

import com.felipemelozx.kairos.api.TimeBlockSeriesApi;
import com.felipemelozx.kairos.common.ErrorCode;
import com.felipemelozx.kairos.common.Result;
import com.felipemelozx.kairos.dto.request.CreateSeriesRequest;
import com.felipemelozx.kairos.dto.request.UpdateSeriesRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.SeriesResponse;
import com.felipemelozx.kairos.service.TimeBlockSeriesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/time-block-series")
public class TimeBlockSeriesController implements TimeBlockSeriesApi {

    private final TimeBlockSeriesService seriesService;

    public TimeBlockSeriesController(TimeBlockSeriesService seriesService) {
        this.seriesService = seriesService;
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<SeriesResponse>> create(
            @Valid @RequestBody CreateSeriesRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Result<UUID> userId = currentUserId(principal);
        if (userId instanceof Result.Err<UUID> err) {
            return HttpErrorMapper.toResponse(err.error());
        }
        UUID id = ((Result.Ok<UUID>) userId).value();
        return seriesService.create(id, request).fold(
                series -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(series)),
                HttpErrorMapper::toResponse);
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<SeriesResponse>>> list(
            @AuthenticationPrincipal UserDetails principal) {
        Result<UUID> userId = currentUserId(principal);
        if (userId instanceof Result.Err<UUID> err) {
            return HttpErrorMapper.toResponse(err.error());
        }
        UUID id = ((Result.Ok<UUID>) userId).value();
        return seriesService.listByUser(id).fold(
                series -> ResponseEntity.ok(ApiResponse.success(series)),
                HttpErrorMapper::toResponse);
    }

    @Override
    @GetMapping("/{seriesId}")
    public ResponseEntity<ApiResponse<SeriesResponse>> get(
            @PathVariable("seriesId") UUID seriesId,
            @AuthenticationPrincipal UserDetails principal) {
        Result<UUID> userId = currentUserId(principal);
        if (userId instanceof Result.Err<UUID> err) {
            return HttpErrorMapper.toResponse(err.error());
        }
        UUID id = ((Result.Ok<UUID>) userId).value();
        return seriesService.getById(seriesId, id).fold(
                series -> ResponseEntity.ok(ApiResponse.success(series)),
                HttpErrorMapper::toResponse);
    }

    @Override
    @PatchMapping("/{seriesId}")
    public ResponseEntity<ApiResponse<SeriesResponse>> update(
            @PathVariable("seriesId") UUID seriesId,
            @Valid @RequestBody UpdateSeriesRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Result<UUID> userId = currentUserId(principal);
        if (userId instanceof Result.Err<UUID> err) {
            return HttpErrorMapper.toResponse(err.error());
        }
        UUID id = ((Result.Ok<UUID>) userId).value();
        return seriesService.update(seriesId, id, request).fold(
                series -> ResponseEntity.ok(ApiResponse.success(series)),
                HttpErrorMapper::toResponse);
    }

    @Override
    @DeleteMapping("/{seriesId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("seriesId") UUID seriesId,
            @AuthenticationPrincipal UserDetails principal) {
        Result<UUID> userId = currentUserId(principal);
        if (userId instanceof Result.Err<UUID> err) {
            return HttpErrorMapper.toResponse(err.error());
        }
        UUID id = ((Result.Ok<UUID>) userId).value();
        return seriesService.softDelete(seriesId, id).fold(
                ignored -> ResponseEntity.ok(ApiResponse.success(null)),
                HttpErrorMapper::toResponse);
    }

    private Result<UUID> currentUserId(UserDetails principal) {
        if (principal == null || principal.getUsername() == null) {
            return Result.err(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Result.ok(UUID.fromString(principal.getUsername()));
        } catch (IllegalArgumentException ex) {
            return Result.err(ErrorCode.UNAUTHORIZED);
        }
    }
}
