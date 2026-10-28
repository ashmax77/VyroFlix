package com.vyroflix.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtSecurityConfigTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    WebMvcAutoConfiguration.class,
                    SecurityAutoConfiguration.class,
                    JwtSecurityConfig.class,
                    ServletWebServerFactoryAutoConfiguration.class
            ))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    @DisplayName("Security beans are auto-configured in servlet context")
    void securityBeansAutoConfigured() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityFilterChain.class);
            assertThat(context).hasSingleBean(SupabaseJwtConverter.class);
            assertThat(context).hasSingleBean(AuthenticationEntryPoint.class);
            assertThat(context).hasSingleBean(AccessDeniedHandler.class);
        });
    }

    @Test
    @DisplayName("Security configuration binds when JwtDecoder bean is provided")
    void securityConfigWithJwtDecoder() {
        JwtDecoder mockDecoder = mock(JwtDecoder.class);

        contextRunner
                .withBean(JwtDecoder.class, () -> mockDecoder)
                .run(context -> {
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    assertThat(context).hasSingleBean(JwtDecoder.class);
                });
    }

    @Test
    @DisplayName("CORS only allows configured origins and never a bare wildcard")
    void corsUsesConfiguredOrigins() {
        contextRunner
                .withPropertyValues("vyroflix.security.cors-allowed-origins=https://app.example.com,https://*.vercel.app")
                .run(context -> {
                    CorsConfigurationSource source = context.getBean("corsConfigurationSource", CorsConfigurationSource.class);
                    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/catalog/home");
                    ServletRequestPathUtils.parseAndCache(request);
                    CorsConfiguration cors = source.getCorsConfiguration(request);

                    assertThat(cors).isNotNull();
                    assertThat(cors.checkOrigin("https://app.example.com")).isEqualTo("https://app.example.com");
                    assertThat(cors.checkOrigin("https://preview-1.vercel.app")).isEqualTo("https://preview-1.vercel.app");
                    assertThat(cors.checkOrigin("https://evil.example.org")).isNull();
                });
    }

    @Test
    @DisplayName("Security auto-configuration is skipped when disabled by property")
    void securityDisabledByProperty() {
        contextRunner
                .withPropertyValues("vyroflix.security.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JwtSecurityConfig.class);
                    assertThat(context).doesNotHaveBean("securityFilterChain");
                });
    }
}
