package com.vyroflix.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error response body returned by all VyroFlix APIs.
 *
 * <p>Follows the spirit of RFC 7807 Problem Details with VyroFlix-specific
 * fields. The {@code code} field is a machine-readable error code; the
 * {@code message} field is a human-readable description.</p>
 *
 * <p>Per AGENTS.md: "Return RFC 7807 Problem Details or the repository's
 * documented error envelope consistently."</p>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {

    /** ISO 8601 timestamp of the error (UTC). */
    private final Instant timestamp;

    /** HTTP status code. */
    private final int status;

    /** Machine-readable error code, e.g. {@code "TITLE_NOT_FOUND"}. */
    private final String code;

    /** Human-readable error message. */
    private final String message;

    /** Request path that triggered the error. */
    private final String path;

    /** Correlation ID for tracing this request across services. */
    private final String correlationId;

    /** Optional list of field-level validation errors or additional details. */
    private final List<String> details;
}
