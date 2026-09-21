package com.felipemelozx.kairos.common;

import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.ErrorResponse;
import org.springframework.http.ResponseEntity;

/**
 * Expected business-rule failure. Carries code + message + status
 * so controllers can build the response without a GlobalExceptionHandler.
 */
public record AppError(String code, String message, org.springframework.http.HttpStatus status) {

    public static AppError of(ErrorCode errorCode) {
        return new AppError(errorCode.code(), errorCode.defaultMessage(), errorCode.status());
    }

    public static AppError of(ErrorCode errorCode, String message) {
        return new AppError(errorCode.code(), message, errorCode.status());
    }

    public ErrorResponse toErrorResponse() {
        return new ErrorResponse(code, message, null);
    }

    public <T> ResponseEntity<ApiResponse<T>> toResponse() {
        return ResponseEntity.status(status).body(ApiResponse.error(toErrorResponse()));
    }
}
