package com.vyroflix.common.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/**
 * Global exception handler for all Spring MVC services.
 *
 * <p>Translates exceptions into a consistent {@link ErrorResponse} body.
 * Stack traces are never exposed in production — only the machine-readable
 * code and human-readable message are returned.</p>
 *
 * <p>Per AGENTS.md: "Do not expose stack traces in production."</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------ //
    // VyroFlix domain exceptions                                          //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException ex, HttpServletRequest request) {

        ErrorCode errorCode = ex.getErrorCode();
        log.warn("API error [{}]: {} (path={})",
                errorCode.getCode(), ex.getMessage(), request.getRequestURI());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(errorCode.getHttpStatus().value())
                .code(errorCode.getCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .correlationId(correlationId(request))
                .details(ex.getDetails())
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    // ------------------------------------------------------------------ //
    // Jakarta Validation errors                                           //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        log.warn("Validation failed (path={}): {}", request.getRequestURI(), fieldErrors);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .code(ErrorCode.VALIDATION_FAILED.getCode())
                .message("Request validation failed")
                .path(request.getRequestURI())
                .correlationId(correlationId(request))
                .details(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    // ------------------------------------------------------------------ //
    // Type mismatch (e.g. bad UUID in path)                               //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String detail = "Parameter '%s' must be of type %s"
                .formatted(ex.getName(), ex.getRequiredType() != null
                        ? ex.getRequiredType().getSimpleName() : "unknown");

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .code(ErrorCode.BAD_REQUEST.getCode())
                .message(detail)
                .path(request.getRequestURI())
                .correlationId(correlationId(request))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ------------------------------------------------------------------ //
    // 404 — no resource found                                             //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoResourceFoundException ex, HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .code(ErrorCode.NOT_FOUND.getCode())
                .message("Resource not found")
                .path(request.getRequestURI())
                .correlationId(correlationId(request))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ------------------------------------------------------------------ //
    // Catch-all                                                           //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error (path={}): {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code(ErrorCode.INTERNAL_ERROR.getCode())
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .correlationId(correlationId(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ------------------------------------------------------------------ //
    // Helpers                                                             //
    // ------------------------------------------------------------------ //

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute("correlationId");
        return attr != null ? attr.toString() : null;
    }
}
