package com.felipemelozx.kairos.controller;

import com.felipemelozx.kairos.common.AppError;
import com.felipemelozx.kairos.common.ErrorCode;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Maps domain errors ({@link AppError}) to HTTP responses.
 * Single place where an {@link ErrorCode} becomes an HTTP status,
 * keeping the common error contract presentation-agnostic.
 */
public final class HttpErrorMapper {

    private HttpErrorMapper() {
    }

    public static HttpStatus toStatus(AppError error) {
        ErrorCode code;
        try {
            code = ErrorCode.valueOf(error.code());
        } catch (IllegalArgumentException ex) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (code) {
            case EMAIL_EXISTS -> HttpStatus.CONFLICT;
            case INVALID_CREDENTIALS, UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_PROJECT_NAME, INVALID_COLOR, INVALID_TIME_BLOCK_TITLE, INVALID_TIME_RANGE -> HttpStatus.BAD_REQUEST;
        };
    }

    public static <T> ResponseEntity<ApiResponse<T>> toResponse(AppError error) {
        return ResponseEntity.status(toStatus(error))
                .body(ApiResponse.error(new ErrorResponse(error.code(), error.message(), null)));
    }
}
