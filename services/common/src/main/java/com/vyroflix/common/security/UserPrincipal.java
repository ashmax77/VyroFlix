package com.vyroflix.common.security;

import com.vyroflix.common.util.Constants;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the authenticated user extracted from a validated Supabase JWT.
 */
@Getter
@Builder
@ToString(exclude = "claims")
public class UserPrincipal implements Principal, Serializable {

    private final UUID userId;
    private final String email;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> claims;

    @Override
    public String getName() {
        return userId != null ? userId.toString() : (email != null ? email : "anonymous");
    }

    /**
     * Checks if the authenticated principal has the ADMIN role.
     */
    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equalsIgnoreCase(role)
                || hasAuthority("ROLE_" + Constants.ROLE_ADMIN);
    }

    /**
     * Checks if the authenticated principal has the USER role.
     */
    public boolean isUser() {
        return Constants.ROLE_USER.equalsIgnoreCase(role)
                || hasAuthority("ROLE_" + Constants.ROLE_USER)
                || isAdmin();
    }

    private boolean hasAuthority(String authority) {
        if (authorities == null) {
            return false;
        }
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(authority));
    }

    public Map<String, Object> getClaims() {
        return claims != null ? Collections.unmodifiableMap(claims) : Collections.emptyMap();
    }
}
