package com.felipemelozx.kairos.controller;

import com.felipemelozx.kairos.api.TimeBlockApi;
import com.felipemelozx.kairos.common.ErrorCode;
import com.felipemelozx.kairos.common.Result;
import com.felipemelozx.kairos.dto.request.CreateTimeBlockRequest;
import com.felipemelozx.kairos.dto.request.UpdateTimeBlockRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.TimeBlockResponse;
import com.felipemelozx.kairos.service.TimeBlockService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/time-blocks")
public class TimeBlockController implements TimeBlockApi {

    private final TimeBlockService timeBlockService;

    public TimeBlockController(TimeBlockService timeBlockService) {
        this.timeBlockService = timeBlockService;
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<TimeBlockResponse>> create(
            @Valid @RequestBody CreateTimeBlockRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Result<UUID> userId = currentUserId(principal);
        if (userId instanceof Result.Err<UUID> err) {
            return HttpErrorMapper.toResponse(err.error());
        }
        UUID id = ((Result.Ok<UUID>) userId).value();
        return timeBlockService.create(id, request).fold(
                block -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(block)),
                HttpErrorMapper::toResponse);
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<TimeBlockResponse>>> list(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @AuthenticationPrincipal UserDetails principal) {
        Result<UUID> userId = currentUserId(principal);
        if (userId instanceof Result.Err<UUID> err) {
            return HttpErrorMapper.toResponse(err.error());
        }
        UUID id = ((Result.Ok<UUID>) userId).value();
        return timeBlockService.listByUser(id, from, to).fold(
                blocks -> ResponseEntity.ok(ApiResponse.success(blocks)),
                HttpErrorMapper::toResponse);
    }

    @Override
    @GetMapping("/{blockId}")
    public ResponseEntity<ApiResponse<TimeBlockResponse>> get(
            @PathVariable("blockId") UUID blockId,
            @AuthenticationPrincipal UserDetails principal) {
        Result<UUID> userId = currentUserId(principal);
        if (userId instanceof Result.Err<UUID> err) {
            return HttpErrorMapper.toResponse(err.error());
        }
        UUID id = ((Result.Ok<UUID>) userId).value();
        return timeBlockService.getById(blockId, id).fold(
                block -> ResponseEntity.ok(ApiResponse.success(block)),
                HttpErrorMapper::toResponse);
    }

    @Override
    @PatchMapping("/{blockId}")
    public ResponseEntity<ApiResponse<TimeBlockResponse>> update(
            @PathVariable("blockId") UUID blockId,
            @Valid @RequestBody UpdateTimeBlockRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Result<UUID> userId = currentUserId(principal);
        if (userId instanceof Result.Err<UUID> err) {
            return HttpErrorMapper.toResponse(err.error());
        }
        UUID id = ((Result.Ok<UUID>) userId).value();
        return timeBlockService.update(blockId, id, request).fold(
                block -> ResponseEntity.ok(ApiResponse.success(block)),
                HttpErrorMapper::toResponse);
    }

    @Override
    @DeleteMapping("/{blockId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("blockId") UUID blockId,
            @AuthenticationPrincipal UserDetails principal) {
        Result<UUID> userId = currentUserId(principal);
        if (userId instanceof Result.Err<UUID> err) {
            return HttpErrorMapper.toResponse(err.error());
        }
        UUID id = ((Result.Ok<UUID>) userId).value();
        return timeBlockService.softDelete(blockId, id).fold(
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
