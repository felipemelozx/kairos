package com.felipemelozx.kairos.common;

/**
 * Central catalog of expected application errors.
 * Carries only code + default message. HTTP status mapping
 * lives in the controller layer (see HttpErrorMapper), so this
 * contract stays presentation-agnostic.
 */
public enum ErrorCode {
    EMAIL_EXISTS("EMAIL_EXISTS", "Email already registered"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid email or password"),
    UNAUTHORIZED("UNAUTHORIZED", "Not authenticated"),
    NOT_FOUND("NOT_FOUND", "Resource not found"),
    INVALID_PROJECT_NAME("INVALID_PROJECT_NAME", "Project name must be between 1 and 100 characters"),
    INVALID_COLOR("INVALID_COLOR", "Color must be a valid hex color (#RRGGBB)"),
    INVALID_TIME_BLOCK_TITLE("INVALID_TIME_BLOCK_TITLE", "Time block title must be between 1 and 200 characters"),
    INVALID_TIME_RANGE("INVALID_TIME_RANGE", "End datetime must be after start datetime");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
