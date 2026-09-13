package com.vyroflix.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * VyroFlix API Gateway Application.
 *
 * <p>Acts as the single entry point for all client requests (/api/v1/**).
 * Handles routing, JWT authentication validation (Supabase), rate limiting,
 * CORS, request correlation, and error translation.</p>
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
