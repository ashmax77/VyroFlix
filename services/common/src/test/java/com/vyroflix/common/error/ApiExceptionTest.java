package com.vyroflix.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionTest {

    @Test
    @DisplayName("ApiException carries error code and message")
    void basicException() {
        ApiException ex = new ApiException(
                ErrorCode.TITLE_NOT_FOUND,
                "Title with ID abc not found");

        assertEquals(ErrorCode.TITLE_NOT_FOUND, ex.getErrorCode());
        assertEquals("Title with ID abc not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getErrorCode().getHttpStatus());
        assertTrue(ex.getDetails().isEmpty());
    }

    @Test
    @DisplayName("ApiException with details carries field-level info")
    void exceptionWithDetails() {
        List<String> details = List.of("title: must not be blank", "releaseYear: must be positive");

        ApiException ex = new ApiException(
                ErrorCode.VALIDATION_FAILED,
                "Validation failed",
                details);

        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        assertEquals(2, ex.getDetails().size());
        assertEquals("title: must not be blank", ex.getDetails().get(0));
    }

    @Test
    @DisplayName("Null details list is normalized to empty list")
    void nullDetailsNormalized() {
        ApiException ex = new ApiException(
                ErrorCode.BAD_REQUEST,
                "Bad request",
                (List<String>) null);

        assertNotNull(ex.getDetails());
        assertTrue(ex.getDetails().isEmpty());
    }
}
