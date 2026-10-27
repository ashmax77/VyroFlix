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
        Map<String, Object> overrides = new HashMap<>();

        normalizeDatasourceUrl(environment, overrides);
        normalizeRedisUrl(environment, overrides);

        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("databaseUrlNormalizer", overrides));
        }
    }

    private void normalizeDatasourceUrl(ConfigurableEnvironment environment, Map<String, Object> overrides) {
        String rawUrl = environment.getProperty(SPRING_DATASOURCE_URL);
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        }

        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }

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
                        String region = System.getenv("SUPABASE_REGION");
                        if (region != null && !region.isBlank()) {
                            poolerHost = "aws-0-" + region.trim() + ".pooler.supabase.com";
                        } else {
                            // Default to ap-south-1 (Mumbai) where project sxyrwfjwdxsbiuqmvvac is hosted
                            poolerHost = "aws-0-ap-south-1.pooler.supabase.com";
                        }
                    }
                    host = poolerHost;
                    String currentUser = (String) overrides.getOrDefault(SPRING_DATASOURCE_USERNAME,
                            environment.getProperty(SPRING_DATASOURCE_USERNAME));
                    if (currentUser == null || currentUser.isBlank() || currentUser.equals("postgres")) {
                        overrides.put(SPRING_DATASOURCE_USERNAME, "postgres." + projectRef);
                    }
                    log.info("Redirected IPv6-only Supabase direct host to IPv4 pooler: {} (user: postgres.{})", host, projectRef);
                } else if (host != null && host.contains("aws-0-ap-southeast-1.pooler.supabase.com")) {
                    host = host.replace("aws-0-ap-southeast-1.pooler.supabase.com", "aws-0-ap-south-1.pooler.supabase.com");
                    log.info("Corrected pooler region from ap-southeast-1 to ap-south-1: {}", host);
                }

                // Ensure Supabase pooler username includes tenant project-ref (e.g. postgres.<ref>)
                if (host != null && host.contains("pooler.supabase.com")) {
                    String currentUser = (String) overrides.get(SPRING_DATASOURCE_USERNAME);
                    if (currentUser == null || currentUser.isBlank()) {
                        currentUser = environment.getProperty(SPRING_DATASOURCE_USERNAME);
                    }
                    if (currentUser == null || currentUser.isBlank()) {
                        currentUser = environment.getProperty("SPRING_DATASOURCE_USERNAME");
                    }
                    if (currentUser == null || currentUser.isBlank()) {
                        currentUser = environment.getProperty("POSTGRES_USER");
                    }
                    if (currentUser == null || currentUser.isBlank()) {
                        currentUser = "postgres";
                    }

                    if (!currentUser.contains(".")) {
                        String projectRef = resolveSupabaseProjectRef(environment);
                        overrides.put(SPRING_DATASOURCE_USERNAME, currentUser + "." + projectRef);
                        log.info("Appended Supabase tenant to username for pooler: {}.{}", currentUser, projectRef);
                    } else {
                        overrides.put(SPRING_DATASOURCE_USERNAME, currentUser);
                    }
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
    }

    private void normalizeRedisUrl(ConfigurableEnvironment environment, Map<String, Object> overrides) {
        String redisUrl = environment.getProperty("spring.data.redis.url");
        if (redisUrl == null || redisUrl.isBlank()) {
            redisUrl = environment.getProperty("UPSTASH_REDIS_URL");
        }
        if (redisUrl == null || redisUrl.isBlank()) {
            redisUrl = System.getenv("UPSTASH_REDIS_URL");
        }
        if (redisUrl == null || redisUrl.isBlank()) {
            redisUrl = System.getenv("SPRING_DATA_REDIS_URL");
        }
        if (redisUrl == null || redisUrl.isBlank()) {
            return;
        }

        redisUrl = redisUrl.trim();
        if (redisUrl.startsWith("redis://") || redisUrl.startsWith("rediss://")) {
            return;
        }

        if (redisUrl.startsWith("https://") || redisUrl.startsWith("http://")) {
            try {
                URI uri = URI.create(redisUrl);
                String scheme = redisUrl.startsWith("https://") ? "rediss://" : "redis://";
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 6379;
                String userInfo = uri.getUserInfo();
                String fixed = scheme + (userInfo != null && !userInfo.isBlank() ? userInfo + "@" : "") + host + ":" + port;
                overrides.put("spring.data.redis.url", fixed);
                log.info("Normalized HTTP Redis URL to valid Redis protocol: {}{}:{}", scheme, host, port);
                return;
            } catch (Exception ignored) {}
        }

        // If it's a raw token / password (does not contain '://')
        if (!redisUrl.contains("://")) {
            String restUrl = environment.getProperty("UPSTASH_REDIS_REST_URL");
            if (restUrl == null || restUrl.isBlank()) {
                restUrl = System.getenv("UPSTASH_REDIS_REST_URL");
            }
            if (restUrl != null && !restUrl.isBlank()) {
                try {
                    URI u = URI.create(restUrl.trim());
                    String host = u.getHost();
                    if (host != null && !host.isBlank()) {
                        String constructed = "rediss://default:" + redisUrl + "@" + host + ":6379";
                        overrides.put("spring.data.redis.url", constructed);
                        log.info("Constructed valid rediss URL from UPSTASH_REDIS_REST_URL: rediss://default:***@{}:6379", host);
                        return;
                    }
                } catch (Exception ignored) {}
            }

            String host = environment.getProperty("REDIS_HOST");
            if (host == null || host.isBlank() || host.equals("localhost")) {
                host = System.getenv("REDIS_HOST");
            }
            if (host != null && !host.isBlank() && !host.equals("localhost")) {
                String constructed = "rediss://default:" + redisUrl + "@" + host.trim() + ":6379";
                overrides.put("spring.data.redis.url", constructed);
                log.info("Constructed valid rediss URL from REDIS_HOST: rediss://default:***@{}:6379", host);
                return;
            }

            // Clear invalid URL to prevent Lettuce crash, and assign to password
            overrides.put("spring.data.redis.url", "");
            overrides.put("spring.data.redis.password", redisUrl);
            log.warn("Invalid Redis URL detected (raw token provided). Cleared spring.data.redis.url to prevent Lettuce crash. Set spring.data.redis.password.");
        }
    }

    private String resolveSupabaseProjectRef(ConfigurableEnvironment environment) {
        String ref = System.getenv("SUPABASE_PROJECT_REF");
        if (ref != null && !ref.isBlank()) {
            return ref.trim();
        }
        String url = environment.getProperty("supabase.url");
        if (url == null || url.isBlank()) {
            url = System.getenv("SUPABASE_URL");
        }
        if (url != null && url.contains(".supabase.co")) {
            try {
                java.net.URI uri = java.net.URI.create(url);
                String h = uri.getHost();
                if (h != null && h.endsWith(".supabase.co")) {
                    return h.substring(0, h.indexOf(".supabase.co"));
                }
            } catch (Exception ignored) {}
        }
        return "sxyrwfjwdxsbiuqmvvac";
    }
}
