package com.vyroflix.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes used across all VyroFlix services.
 *
 * <p>Each code maps to an HTTP status so the {@link GlobalExceptionHandler}
 * can produce a consistent response without per-service logic.</p>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // --- Generic ---
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED("VALIDATION_FAILED", HttpStatus.UNPROCESSABLE_ENTITY),
    NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT),

    // --- Auth / Security ---
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN),
    TOKEN_EXPIRED("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED),

    // --- Content ---
    TITLE_NOT_FOUND("TITLE_NOT_FOUND", HttpStatus.NOT_FOUND),
    TITLE_NOT_PUBLISHABLE("TITLE_NOT_PUBLISHABLE", HttpStatus.UNPROCESSABLE_ENTITY),
    ASSET_NOT_READY("ASSET_NOT_READY", HttpStatus.UNPROCESSABLE_ENTITY),

    // --- Upload / Encoding ---
    UPLOAD_INTENT_NOT_FOUND("UPLOAD_INTENT_NOT_FOUND", HttpStatus.NOT_FOUND),
    UPLOAD_FAILED("UPLOAD_FAILED", HttpStatus.INTERNAL_SERVER_ERROR),
    ENCODING_FAILED("ENCODING_FAILED", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- Playback ---
    PLAYBACK_NOT_AUTHORIZED("PLAYBACK_NOT_AUTHORIZED", HttpStatus.FORBIDDEN),
    SESSION_NOT_FOUND("SESSION_NOT_FOUND", HttpStatus.NOT_FOUND),
    SESSION_EXPIRED("SESSION_EXPIRED", HttpStatus.GONE),

    // --- Watchlist / History ---
    WATCHLIST_DUPLICATE("WATCHLIST_DUPLICATE", HttpStatus.CONFLICT),
    WATCHLIST_ENTRY_NOT_FOUND("WATCHLIST_ENTRY_NOT_FOUND", HttpStatus.NOT_FOUND);

    /** Machine-readable code sent in the error response body. */
    private final String code;

    /** HTTP status to return for this error. */
    private final HttpStatus httpStatus;
}
