package com.vyroflix.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SupabaseJwtConverterTest {

    private final SupabaseJwtConverter converter = new SupabaseJwtConverter();

    @Test
    @DisplayName("Convert JWT with app_metadata admin role grants ROLE_ADMIN and ROLE_USER")
    void convertAdminRoleFromAppMetadata() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = createJwt(
                userId.toString(),
                "admin@vyroflix.com",
                Map.of("role", "ADMIN"),
                Map.of(),
                "authenticated");

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertNotNull(token);
        assertEquals(userId.toString(), token.getName());

        Set<String> authorities = getAuthorityNames(token);
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("ROLE_USER"));

        assertTrue(token.getDetails() instanceof UserPrincipal);
        UserPrincipal principal = (UserPrincipal) token.getDetails();
        assertEquals(userId, principal.getUserId());
        assertEquals("admin@vyroflix.com", principal.getEmail());
        assertEquals("ADMIN", principal.getRole());
        assertTrue(principal.isAdmin());
        assertTrue(principal.isUser());
    }

    @Test
    @DisplayName("Convert JWT with user_metadata admin role grants ROLE_ADMIN")
    void convertAdminRoleFromUserMetadata() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = createJwt(
                userId.toString(),
                "manager@vyroflix.com",
                Map.of(),
                Map.of("role", "admin"),
                "authenticated");

        AbstractAuthenticationToken token = converter.convert(jwt);

        Set<String> authorities = getAuthorityNames(token);
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Convert standard Supabase user token with role=authenticated grants ROLE_USER")
    void convertStandardAuthenticatedRole() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = createJwt(
                userId.toString(),
                "viewer@vyroflix.com",
                Map.of(),
                Map.of(),
                "authenticated");

        AbstractAuthenticationToken token = converter.convert(jwt);

        Set<String> authorities = getAuthorityNames(token);
        assertFalse(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("ROLE_USER"));

        UserPrincipal principal = (UserPrincipal) token.getDetails();
        assertFalse(principal.isAdmin());
        assertTrue(principal.isUser());
    }

    @Test
    @DisplayName("Convert JWT with list of roles in app_metadata")
    void convertMultipleRolesInAppMetadata() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = createJwt(
                userId.toString(),
                "vip@vyroflix.com",
                Map.of("roles", List.of("PREMIUM", "VIP")),
                Map.of(),
                "authenticated");

        AbstractAuthenticationToken token = converter.convert(jwt);

        Set<String> authorities = getAuthorityNames(token);
        assertTrue(authorities.contains("ROLE_PREMIUM"));
        assertTrue(authorities.contains("ROLE_VIP"));
        assertTrue(authorities.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Handles invalid UUID gracefully without exception")
    void handlesInvalidUuidSubject() {
        Jwt jwt = createJwt(
                "not-a-valid-uuid",
                "test@vyroflix.com",
                Map.of(),
                Map.of(),
                "authenticated");

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertNotNull(token);
        UserPrincipal principal = (UserPrincipal) token.getDetails();
        assertNull(principal.getUserId());
        assertEquals("test@vyroflix.com", principal.getName());
    }

    @Test
    @DisplayName("UserPrincipal toString does not leak claims")
    void userPrincipalToStringExcludesClaims() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder()
                .userId(userId)
                .email("secure@vyroflix.com")
                .role("USER")
                .claims(Map.of("sensitive_token", "secret123"))
                .build();

        String str = principal.toString();
        assertFalse(str.contains("secret123"));
        assertTrue(str.contains(userId.toString()));
    }

    private static Jwt createJwt(
            String sub,
            String email,
            Map<String, Object> appMetadata,
            Map<String, Object> userMetadata,
            String role) {

        Map<String, Object> headers = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", sub);
        claims.put("email", email);
        claims.put("role", role);
        claims.put("aud", "authenticated");
        claims.put("iss", "https://test.supabase.co/auth/v1");
        claims.put("app_metadata", appMetadata);
        claims.put("user_metadata", userMetadata);

        return new Jwt(
                "mock-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims);
    }

    private static Set<String> getAuthorityNames(AbstractAuthenticationToken token) {
        Set<String> names = new HashSet<>();
        for (GrantedAuthority a : token.getAuthorities()) {
            names.add(a.getAuthority());
        }
        return names;
    }
}
