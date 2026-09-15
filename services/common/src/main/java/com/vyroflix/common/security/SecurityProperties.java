package com.vyroflix.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Security configuration properties for Supabase JWT verification and RBAC.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "vyroflix.security")
public class SecurityProperties {

    /**
     * Whether security filters are enabled. Defaults to true.
     */
    private boolean enabled = true;

    /**
     * Supabase JWKS URI (e.g. {@code https://<project>.supabase.co/auth/v1/.well-known/jwks.json}).
     */
    private String jwksUri;

    /**
     * Expected JWT issuer (e.g. {@code https://<project>.supabase.co/auth/v1}).
     */
    private String issuer;

    /**
     * Expected audience claim (Supabase default is {@code authenticated}).
     */
    private String audience = "authenticated";

    /**
     * Endpoints accessible without authentication.
     */
    private List<String> publicEndpoints = new ArrayList<>(List.of(
            "/actuator/health/**",
            "/actuator/info",
            "/api/v1/auth/**"
    ));

    /**
     * Mock authentication settings for local development and unit tests.
     */
    private MockAuth mockAuth = new MockAuth();

    @Getter
    @Setter
    public static class MockAuth {
        /**
         * Whether mock authentication is active (bypasses remote JWKS validation).
         */
        private boolean enabled = false;

        /**
         * User ID to inject when mock auth is enabled.
         */
        private String userId = "00000000-0000-0000-0000-000000000001";

        /**
         * Email to inject when mock auth is enabled.
         */
        private String email = "dev@vyroflix.local";

        /**
         * Role to inject when mock auth is enabled (USER or ADMIN).
         */
        private String role = "ADMIN";
    }
}
