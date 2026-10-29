package com.vyroflix.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("A closed client connection is not reported as a server error and writes no body")
    void clientAbortIsSilentWithoutBody() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/catalog/home");
        IOException aborted = new IOException("An established connection was aborted by the software in your host machine");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(aborted, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("Client-gone detection looks through wrapped causes")
    void detectsWrappedClientAbort() {
        IOException broken = new IOException("Broken pipe");
        RuntimeException wrapped = new RuntimeException("write failed", broken);

        assertTrue(GlobalExceptionHandler.isClientGone(wrapped));
    }

    @Test
    @DisplayName("Ordinary failures still return a Problem body")
    void ordinaryFailureKeepsBody() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/catalog/home");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new IllegalStateException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertFalse(GlobalExceptionHandler.isClientGone(new IllegalStateException("boom")));
    }
}
