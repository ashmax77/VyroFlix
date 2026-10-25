package com.vyroflix.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes database connection URLs before Spring Boot / HikariCP initializes.
 *
 * <p>Handles common cloud provider formats such as URI-style connection strings
 * (e.g. {@code postgresql://user:pass@host:port/db} from Supabase / Render) and
 * converts them into strictly valid JDBC URLs ({@code jdbc:postgresql://host:port/db?sslmode=require}),
 * automatically extracting username and password into Spring Datasource properties.</p>
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SPRING_DATASOURCE_URL = "spring.datasource.url";
    private static final String SPRING_DATASOURCE_USERNAME = "spring.datasource.username";
    private static final String SPRING_DATASOURCE_PASSWORD = "spring.datasource.password";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String rawUrl = environment.getProperty(SPRING_DATASOURCE_URL);
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        }

        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }

        Map<String, Object> overrides = new HashMap<>();

        // If URL starts with postgresql:// or postgres:// without jdbc:
        String workUrl = rawUrl.trim();
        boolean hadJdbcPrefix = workUrl.startsWith("jdbc:");
        String uriString = hadJdbcPrefix ? workUrl.substring(5) : workUrl;

        if (uriString.startsWith("postgres://")) {
            uriString = "postgresql://" + uriString.substring("postgres://".length());
        }

        if (uriString.startsWith("postgresql://")) {
            try {
                URI uri = URI.create(uriString);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                if (path == null || path.isBlank() || path.equals("/")) {
                    path = "/postgres";
                }

                String query = uri.getQuery();
                String userInfo = uri.getUserInfo();

                // Extract credentials from URI authority if present
                if (userInfo != null && !userInfo.isBlank()) {
                    String[] creds = userInfo.split(":", 2);
                    String existingUser = environment.getProperty(SPRING_DATASOURCE_USERNAME);
                    String existingPass = environment.getProperty(SPRING_DATASOURCE_PASSWORD);

                    if ((existingUser == null || existingUser.isBlank() || existingUser.equals("postgres")) && creds.length > 0) {
                        overrides.put(SPRING_DATASOURCE_USERNAME, creds[0]);
                    }
                    if ((existingPass == null || existingPass.isBlank()) && creds.length > 1) {
                        overrides.put(SPRING_DATASOURCE_PASSWORD, creds[1]);
                    }
                }

                // IPv4 Pooler auto-redirect for Supabase (direct host db.<ref>.supabase.co is IPv6-only)
                if (host != null && host.startsWith("db.") && host.endsWith(".supabase.co")) {
                    String projectRef = host.substring(3, host.indexOf(".supabase.co"));
                    String poolerHost = System.getenv("SUPABASE_POOLER_HOST");
                    if (poolerHost == null || poolerHost.isBlank()) {
                        poolerHost = "aws-0-ap-southeast-1.pooler.supabase.com";
                    }
                    host = poolerHost;
                    String currentUser = (String) overrides.getOrDefault(SPRING_DATASOURCE_USERNAME,
                            environment.getProperty(SPRING_DATASOURCE_USERNAME));
                    if (currentUser == null || currentUser.isBlank() || currentUser.equals("postgres")) {
                        overrides.put(SPRING_DATASOURCE_USERNAME, "postgres." + projectRef);
                    }
                    log.info("Redirected IPv6-only Supabase direct host to IPv4 pooler: {} (user: postgres.{})", host, projectRef);
                }

                // Construct clean JDBC URL
                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append(path);

                boolean hasQuery = query != null && !query.isBlank();
                if (hasQuery) {
                    jdbcUrl.append("?").append(query);
                }

                // Ensure sslmode=require for non-local hosts
                if (host != null && !host.equals("localhost") && !host.equals("127.0.0.1") && !host.equals("postgres")) {
                    if (!jdbcUrl.toString().contains("sslmode=")) {
                        jdbcUrl.append(hasQuery ? "&" : "?").append("sslmode=require");
                    }
                }

                String finalUrl = jdbcUrl.toString();
                overrides.put(SPRING_DATASOURCE_URL, finalUrl);
                log.info("Normalized datasource URL to valid JDBC format: jdbc:postgresql://{}:{}{}", host, port, path);

            } catch (Exception e) {
                log.warn("Could not parse URI '{}' as a PostgreSQL URI: {}", rawUrl, e.getMessage());
                if (!workUrl.startsWith("jdbc:")) {
                    overrides.put(SPRING_DATASOURCE_URL, "jdbc:" + workUrl);
                }
            }
        } else if (!workUrl.startsWith("jdbc:")) {
            overrides.put(SPRING_DATASOURCE_URL, "jdbc:" + workUrl);
        }

        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("databaseUrlNormalizer", overrides));
        }
    }
}
