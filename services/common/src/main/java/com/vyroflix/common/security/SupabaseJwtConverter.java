package com.vyroflix.common.security;

import com.vyroflix.common.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.*;

/**
 * Converts a Supabase-issued {@link Jwt} into a Spring Security
 * {@link JwtAuthenticationToken}.
 *
 * <p>
 * Extracts user ID (from {@code sub}), email, and RBAC roles from Supabase's
 * {@code app_metadata}, {@code user_metadata}, or top-level {@code role} claim.
 * </p>
 */
@Slf4j
public class SupabaseJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String CLAIM_APP_METADATA = "app_metadata";
    private static final String CLAIM_USER_METADATA = "user_metadata";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_EMAIL = "email";
    private static final String AUTHENTICATED_ROLE = "authenticated";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UserPrincipal principal = extractPrincipal(jwt);
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities, principal.getName());
        token.setDetails(principal);
        return token;
    }

    /**
     * Extracts a {@link UserPrincipal} from the validated JWT claims.
     */
    public static UserPrincipal extractPrincipal(Jwt jwt) {
        UUID userId = extractUserId(jwt);
        String email = jwt.getClaimAsString(CLAIM_EMAIL);
        String primaryRole = resolvePrimaryRole(jwt);
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        return UserPrincipal.builder()
                .userId(userId)
                .email(email)
                .role(primaryRole)
                .authorities(authorities)
                .claims(jwt.getClaims())
                .build();
    }

    /**
     * Extracts Spring Security {@link GrantedAuthority} collection from JWT claims.
     */
    public static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        Set<String> roles = extractRoles(jwt);

        for (String role : roles) {
            String upper = role.toUpperCase().trim();
            if (!upper.startsWith(ROLE_PREFIX)) {
                authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + upper));
            } else {
                authorities.add(new SimpleGrantedAuthority(upper));
            }

            // ADMIN role implicitly inherits USER authority
            if (Constants.ROLE_ADMIN.equalsIgnoreCase(upper)
                    || (ROLE_PREFIX + Constants.ROLE_ADMIN).equalsIgnoreCase(upper)) {
                authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + Constants.ROLE_USER));
            }
        }

        // Every authenticated Supabase token has at least ROLE_USER
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + Constants.ROLE_USER));
        }

        return authorities;
    }

    /**
     * Extracts user UUID from JWT subject claim.
     */
    public static UUID extractUserId(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            log.warn("JWT sub claim [{}] is not a valid UUID", sub);
            return null;
        }
    }

    private static Set<String> extractRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();

        // 1. Check app_metadata for role or roles
        Map<String, Object> appMetadata = jwt.getClaimAsMap(CLAIM_APP_METADATA);
        if (appMetadata != null) {
            Object roleObj = appMetadata.get(CLAIM_ROLE);
            if (roleObj instanceof String roleStr && !roleStr.isBlank()) {
                roles.add(roleStr);
            }
            Object rolesObj = appMetadata.get(CLAIM_ROLES);
            if (rolesObj instanceof Collection<?> roleCol) {
                for (Object r : roleCol) {
                    if (r instanceof String s && !s.isBlank()) {
                        roles.add(s);
                    }
                }
            }
        }

        // 2. Check user_metadata for role
        Map<String, Object> userMetadata = jwt.getClaimAsMap(CLAIM_USER_METADATA);
        if (userMetadata != null) {
            Object roleObj = userMetadata.get(CLAIM_ROLE);
            if (roleObj instanceof String roleStr && !roleStr.isBlank()) {
                roles.add(roleStr);
            }
        }

        // 3. Check top-level role claim (Supabase default: "authenticated")
        String topLevelRole = jwt.getClaimAsString(CLAIM_ROLE);
        if (topLevelRole != null && !topLevelRole.isBlank()) {
            if (AUTHENTICATED_ROLE.equalsIgnoreCase(topLevelRole)) {
                roles.add(Constants.ROLE_USER);
            } else {
                roles.add(topLevelRole);
            }
        }

        if (roles.isEmpty()) {
            roles.add(Constants.ROLE_USER);
        }

        return roles;
    }

    private static String resolvePrimaryRole(Jwt jwt) {
        Set<String> roles = extractRoles(jwt);
        for (String r : roles) {
            if (Constants.ROLE_ADMIN.equalsIgnoreCase(r)) {
                return Constants.ROLE_ADMIN;
            }
        }
        return Constants.ROLE_USER;
    }
}
