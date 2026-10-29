package com.vyroflix.common.security;

import com.vyroflix.common.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
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
 * {@code app_metadata} (server-controlled) or the top-level {@code role} claim.
 * </p>
 */
@Slf4j
public class SupabaseJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String CLAIM_APP_METADATA = "app_metadata";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_PLAN = "plan";
    private static final String AUTHENTICATED_ROLE = "authenticated";

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        UserPrincipal principal = extractPrincipal(jwt);
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        return new SupabaseAuthenticationToken(jwt, principal, authorities);
    }

    /**
     * Extracts a {@link UserPrincipal} from the validated JWT claims.
     */
    public static UserPrincipal extractPrincipal(Jwt jwt) {
        UUID userId = extractUserId(jwt);
        String email = jwt.getClaimAsString(CLAIM_EMAIL);
        String primaryRole = resolvePrimaryRole(jwt);
        String plan = resolvePlan(jwt);
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        return UserPrincipal.builder()
                .userId(userId)
                .email(email)
                .role(primaryRole)
                .plan(plan)
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

        // user_metadata is deliberately NOT consulted: Supabase lets users edit it themselves
        // (auth.updateUser({ data })), so honouring it would allow self-promotion to ADMIN.

        // 2. Check top-level role claim (Supabase default: "authenticated")
        String topLevelRole = jwt.getClaimAsString(CLAIM_ROLE);
        if (topLevelRole != null && !topLevelRole.isBlank()) {
            if (AUTHENTICATED_ROLE.equalsIgnoreCase(topLevelRole)) {
                roles.add(Constants.ROLE_USER);
            } else {
                roles.add(topLevelRole);
            }
        }

        // ADMIN is an allowlist, not just a claim: only the configured account may hold it.
        if (roles.stream().anyMatch(Constants.ROLE_ADMIN::equalsIgnoreCase) && !isAdminEligible(jwt)) {
            log.warn("ADMIN role on a token for a non-allowlisted account was ignored");
            roles.removeIf(Constants.ROLE_ADMIN::equalsIgnoreCase);
        }

        if (roles.isEmpty()) {
            roles.add(Constants.ROLE_USER);
        }

        return roles;
    }

    /** True when the token's email is on {@link Constants#ADMIN_EMAIL_ALLOWLIST} (case-insensitive). */
    static boolean isAdminEligible(Jwt jwt) {
        String email = jwt.getClaimAsString(CLAIM_EMAIL);
        return email != null
                && Constants.ADMIN_EMAIL_ALLOWLIST.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(email));
    }

    /**
     * Subscription plan from app_metadata.plan (server-controlled). Anything missing or unknown is FREE.
     */
    static String resolvePlan(Jwt jwt) {
        Map<String, Object> appMetadata = jwt.getClaimAsMap(CLAIM_APP_METADATA);
        Object planObj = appMetadata != null ? appMetadata.get(CLAIM_PLAN) : null;
        if (planObj instanceof String plan) {
            String upper = plan.trim().toUpperCase();
            if (Constants.PLAN_BASIC.equals(upper) || Constants.PLAN_PREMIUM.equals(upper)) {
                return upper;
            }
        }
        return Constants.PLAN_FREE;
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
