package com.vyroflix.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
