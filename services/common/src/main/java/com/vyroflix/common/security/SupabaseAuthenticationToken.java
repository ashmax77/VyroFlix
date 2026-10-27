package com.vyroflix.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/**
 * Custom authentication token for Supabase-authenticated users.
 *
 * <p>Ensures that {@link #getPrincipal()} returns {@link UserPrincipal} rather than
 * the raw {@link Jwt}, allowing controllers to inject {@code @AuthenticationPrincipal UserPrincipal principal}
 * directly without null pointer or class cast issues.</p>
 */
public class SupabaseAuthenticationToken extends AbstractAuthenticationToken {

    private final Jwt jwt;
    private final UserPrincipal principal;

    public SupabaseAuthenticationToken(
            Jwt jwt,
            UserPrincipal principal,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.jwt = jwt;
        this.principal = principal;
        setAuthenticated(true);
        setDetails(principal);
    }

    @Override
    public Object getCredentials() {
        return jwt != null ? jwt.getTokenValue() : null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public Jwt getToken() {
        return jwt;
    }
}
