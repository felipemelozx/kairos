package com.felipemelozx.kairos.common;

import java.util.function.Function;

/**
 * Explicit outcome of an operation: success (Ok) or expected failure (Err).
 * Replaces exceptions for control flow (e.g. user not found).
 *
 * <pre>
 * Result&lt;UserResponse&gt; result = authService.login(request);
 * return switch (result) {
 *     case Result.Ok&lt;UserResponse&gt; ok -&gt; ResponseEntity.ok(ApiResponse.success(ok.value()));
 *     case Result.Err&lt;UserResponse&gt; err -&gt; err.error().toResponse();
 * };
 * </pre>
 */
public sealed interface Result<T> permits Result.Ok, Result.Err {

    record Ok<T>(T value) implements Result<T> {}

    record Err<T>(AppError error) implements Result<T> {}

    static <T> Result<T> ok(T value) {
        return new Ok<>(value);
    }

    static <T> Result<T> err(AppError error) {
        return new Err<>(error);
    }

    static <T> Result<T> err(ErrorCode code) {
        return new Err<>(AppError.of(code));
    }

    static <T> Result<T> err(ErrorCode code, String message) {
        return new Err<>(AppError.of(code, message));
    }

    default boolean isOk() {
        return this instanceof Ok;
    }

    default boolean isErr() {
        return this instanceof Err;
    }

    default <R> R fold(Function<T, R> onOk, Function<AppError, R> onErr) {
        return switch (this) {
            case Ok<T> ok -> onOk.apply(ok.value());
            case Err<T> err -> onErr.apply(err.error());
        };
    }
}
