package com.vyroflix.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
    private final SpringApplication application = new SpringApplication();

    @Test
    @DisplayName("Normalizes Supabase URI format with credentials to valid JDBC URL")
    void normalizesSupabaseUriWithCredentials() {
        ConfigurableEnvironment env = new StandardEnvironment();
        System.setProperty("SPRING_DATASOURCE_URL", "postgresql://postgres:mysecretpass@db.sxyrwfjwdxsbiuqmvvac.supabase.co:5432/postgres");

        try {
            processor.postProcessEnvironment(env, application);

            assertThat(env.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require");
            assertThat(env.getProperty("spring.datasource.username"))
                    .isEqualTo("postgres.sxyrwfjwdxsbiuqmvvac");
            assertThat(env.getProperty("spring.datasource.password"))
                    .isEqualTo("mysecretpass");
        } finally {
            System.clearProperty("SPRING_DATASOURCE_URL");
        }
    }

    @Test
    @DisplayName("Leaves standard JDBC URL intact")
    void leavesStandardJdbcUrlIntact() {
        ConfigurableEnvironment env = new StandardEnvironment();
        System.setProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/vyroflix");

        try {
            processor.postProcessEnvironment(env, application);

            assertThat(env.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:postgresql://localhost:5432/vyroflix");
        } finally {
            System.clearProperty("SPRING_DATASOURCE_URL");
        }
    }

    @Test
    @DisplayName("Handles postgres:// scheme and session pooler port")
    void handlesPostgresSchemeWithPooler() {
        ConfigurableEnvironment env = new StandardEnvironment();
        System.setProperty("SPRING_DATASOURCE_URL", "postgres://postgres.ref:secret@aws-0-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require");

        try {
            processor.postProcessEnvironment(env, application);

            assertThat(env.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require");
            assertThat(env.getProperty("spring.datasource.username"))
                    .isEqualTo("postgres.ref");
            assertThat(env.getProperty("spring.datasource.password"))
                    .isEqualTo("secret");
        } finally {
            System.clearProperty("SPRING_DATASOURCE_URL");
        }
    }

    @Test
    @DisplayName("Self-heals ap-southeast-1 pooler host to ap-south-1")
    void selfHealsApSoutheast1Pooler() {
        ConfigurableEnvironment env = new StandardEnvironment();
        System.setProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require");
        System.setProperty("SPRING_DATASOURCE_USERNAME", "postgres");

        try {
            processor.postProcessEnvironment(env, application);

            assertThat(env.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require");
            assertThat(env.getProperty("spring.datasource.username"))
                    .isEqualTo("postgres.sxyrwfjwdxsbiuqmvvac");
        } finally {
            System.clearProperty("SPRING_DATASOURCE_URL");
            System.clearProperty("SPRING_DATASOURCE_USERNAME");
        }
    }

    @Test
    @DisplayName("Clears raw token from spring.data.redis.url and assigns to password")
    void clearsRawTokenFromRedisUrl() {
        ConfigurableEnvironment env = new StandardEnvironment();
        System.setProperty("UPSTASH_REDIS_URL", "d241f478f47502f87b41155e8706c3a7");

        try {
            processor.postProcessEnvironment(env, application);

            assertThat(env.getProperty("spring.data.redis.url")).isEqualTo("redis://localhost:6379");
            assertThat(env.getProperty("spring.data.redis.password")).isEqualTo("d241f478f47502f87b41155e8706c3a7");
        } finally {
            System.clearProperty("UPSTASH_REDIS_URL");
        }
    }

    @Test
    @DisplayName("Constructs valid rediss URL from UPSTASH_REDIS_REST_URL and raw token")
    void constructsRedissUrlFromRestUrlAndToken() {
        ConfigurableEnvironment env = new StandardEnvironment();
        System.setProperty("UPSTASH_REDIS_URL", "d241f478f47502f87b41155e8706c3a7");
        System.setProperty("UPSTASH_REDIS_REST_URL", "https://humorous-gorilla-12345.upstash.io");

        try {
            processor.postProcessEnvironment(env, application);

            assertThat(env.getProperty("spring.data.redis.url"))
                    .isEqualTo("rediss://default:d241f478f47502f87b41155e8706c3a7@humorous-gorilla-12345.upstash.io:6379");
        } finally {
            System.clearProperty("UPSTASH_REDIS_URL");
            System.clearProperty("UPSTASH_REDIS_REST_URL");
        }
    }
}
