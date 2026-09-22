package com.felipemelozx.kairos.common;

/**
 * Expected business-rule failure. Pure domain DTO (code + message),
 * with no knowledge of HTTP — presentation mapping lives in the
 * controller layer (see HttpErrorMapper).
 */
public record AppError(String code, String message) {

    public static AppError of(ErrorCode errorCode) {
        return new AppError(errorCode.code(), errorCode.defaultMessage());
    }

    public static AppError of(ErrorCode errorCode, String message) {
        return new AppError(errorCode.code(), message);
    }
}
