package com.vyroflix.common.error;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Base exception for all VyroFlix API errors.
 *
 * <p>Carries an {@link ErrorCode} which the {@link GlobalExceptionHandler}
 * maps to the appropriate HTTP status and RFC 7807-style response body.</p>
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<String> details;

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = Collections.emptyList();
    }

    public ApiException(ErrorCode errorCode, String message, List<String> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? details : Collections.emptyList();
    }

    public ApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = Collections.emptyList();
    }
}
