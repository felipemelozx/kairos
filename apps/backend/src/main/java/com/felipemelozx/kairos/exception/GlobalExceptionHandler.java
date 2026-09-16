package com.felipemelozx.kairos.exception;

import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        HttpStatus status;
        if ("EMAIL_EXISTS".equals(ex.getCode())) {
            status = HttpStatus.CONFLICT;
        } else if ("NOT_FOUND".equals(ex.getCode())) {
            status = HttpStatus.NOT_FOUND;
        } else if ("UNAUTHORIZED".equals(ex.getCode()) || "INVALID_CREDENTIALS".equals(ex.getCode())) {
            status = HttpStatus.UNAUTHORIZED;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(
                ApiResponse.error(new ErrorResponse(ex.getCode(), ex.getMessage(), null)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
            String message = error.getDefaultMessage();
            details.put(field, message);
        });
        return ResponseEntity.badRequest().body(
                ApiResponse.error(new ErrorResponse("VALIDATION_ERROR",
                        "Request validation failed", details)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(new ErrorResponse("INTERNAL_ERROR",
                        "An unexpected error occurred", null)));
    }
}
