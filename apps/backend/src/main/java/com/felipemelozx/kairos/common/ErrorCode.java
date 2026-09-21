package com.felipemelozx.kairos.common;

import org.springframework.http.HttpStatus;

/**
 * Central catalog of expected application errors.
 * Each code carries its corresponding HttpStatus,
 * so controllers need no if/else status mapping.
 */
public enum ErrorCode {
    EMAIL_EXISTS("EMAIL_EXISTS", "Email already registered", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("UNAUTHORIZED", "Not authenticated", HttpStatus.UNAUTHORIZED),
    NOT_FOUND("NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),
    INVALID_PROJECT_NAME("INVALID_PROJECT_NAME", "Project name must be between 1 and 100 characters", HttpStatus.BAD_REQUEST),
    INVALID_COLOR("INVALID_COLOR", "Color must be a valid hex color (#RRGGBB)", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;

    ErrorCode(String code, String defaultMessage, HttpStatus status) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }
}
