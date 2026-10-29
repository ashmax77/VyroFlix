package com.vyroflix.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * VyroFlix Platform Application.
 *
 * <p>Aggregates all Spring MVC domain services (identity, content, video,
 * streaming, history, search, and recommendation) into a single deployable
 * application to minimize resource usage in low-cost hosting environments (e.g. Render Free).</p>
 *
 * <p>Encoding-service remains separate because FFmpeg is CPU- and disk-intensive.</p>
 */
// @AutoConfiguration classes (outbox, JPA-dependent beans) must not be picked up by component scanning: scanned
// configuration is processed before JPA exists, so their @ConditionalOnBean checks never match. They are still
// registered through META-INF/spring/...AutoConfiguration.imports, in the right order.
@SpringBootApplication
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = AutoConfiguration.class),
        basePackages = {
        "com.vyroflix.common",
        "com.vyroflix.platform",
        "com.vyroflix.identity",
        "com.vyroflix.content",
        "com.vyroflix.video",
        "com.vyroflix.streaming",
        "com.vyroflix.history",
        "com.vyroflix.search",
        "com.vyroflix.recommendation"
})
public class PlatformApplication {

    public static void main(String[] args) {
        sanitizeDatasourceUrl();
        sanitizeRedisUrl();
        SpringApplication.run(PlatformApplication.class, args);
    }

    private static void sanitizeDatasourceUrl() {
        String url = System.getProperty("spring.datasource.url");
        if (url == null || url.isBlank()) {
            url = System.getenv("SPRING_DATASOURCE_URL");
        }
        if (url == null || url.isBlank()) {
            url = System.getenv("DATABASE_URL");
        }
        if (url == null || url.isBlank()) {
            return;
        }

        String workUrl = url.trim();
        String uriString = workUrl.startsWith("jdbc:") ? workUrl.substring(5) : workUrl;
        if (uriString.startsWith("postgres://")) {
            uriString = "postgresql://" + uriString.substring("postgres://".length());
        }

        if (uriString.startsWith("postgresql://")) {
            try {
                java.net.URI uri = java.net.URI.create(uriString);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                if (path == null || path.isBlank() || path.equals("/")) {
                    path = "/postgres";
                }
                String query = uri.getQuery();
                String userInfo = uri.getUserInfo();

                if (userInfo != null && !userInfo.isBlank()) {
                    String[] parts = userInfo.split(":", 2);
                    if (parts.length > 0 && !parts[0].isBlank()) {
                        System.setProperty("spring.datasource.username", parts[0]);
                    }
                    if (parts.length > 1 && !parts[1].isBlank()) {
                        System.setProperty("spring.datasource.password", parts[1]);
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
                    String user = System.getProperty("spring.datasource.username");
                    if (user == null || user.isBlank() || "postgres".equals(user)) {
                        System.setProperty("spring.datasource.username", "postgres." + projectRef);
                    }
                    System.out.println("[VyroFlix] Redirected IPv6-only Supabase host to IPv4 pooler: " + host + " (user: postgres." + projectRef + ")");
                } else if (host != null && host.contains("aws-0-ap-southeast-1.pooler.supabase.com")) {
                    host = host.replace("aws-0-ap-southeast-1.pooler.supabase.com", "aws-0-ap-south-1.pooler.supabase.com");
                    System.out.println("[VyroFlix] Corrected pooler region from ap-southeast-1 to ap-south-1: " + host);
                }

                // Ensure Supabase pooler username includes tenant project-ref (e.g. postgres.<ref>)
                if (host != null && host.contains("pooler.supabase.com")) {
                    String user = System.getProperty("spring.datasource.username");
                    if (user != null && !user.isBlank() && !user.contains(".")) {
                        String projectRef = System.getenv("SUPABASE_PROJECT_REF");
                        if (projectRef == null || projectRef.isBlank()) {
                            projectRef = "sxyrwfjwdxsbiuqmvvac";
                        }
                        System.setProperty("spring.datasource.username", user + "." + projectRef);
                        System.out.println("[VyroFlix] Appended Supabase tenant to username for pooler: " + user + "." + projectRef);
                    }
                }

                StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                        .append(host).append(":").append(port).append(path);
                boolean hasQuery = query != null && !query.isBlank();
                if (hasQuery) {
                    jdbc.append("?").append(query);
                }
                if (host != null && !host.equals("localhost") && !host.equals("127.0.0.1") && !host.equals("postgres")) {
                    if (!jdbc.toString().contains("sslmode=")) {
                        jdbc.append(hasQuery ? "&" : "?").append("sslmode=require");
                    }
                }

                System.setProperty("spring.datasource.url", jdbc.toString());
                System.out.println("[VyroFlix] Normalized datasource URL to JDBC: jdbc:postgresql://" + host + ":" + port + path);
            } catch (Exception e) {
                if (!workUrl.startsWith("jdbc:")) {
                    System.setProperty("spring.datasource.url", "jdbc:" + workUrl);
                }
            }
        } else if (!workUrl.startsWith("jdbc:")) {
            System.setProperty("spring.datasource.url", "jdbc:" + workUrl);
        }
    }

    private static void sanitizeRedisUrl() {
        String redisUrl = System.getProperty("spring.data.redis.url");
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
                java.net.URI uri = java.net.URI.create(redisUrl);
                String scheme = redisUrl.startsWith("https://") ? "rediss://" : "redis://";
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 6379;
                String userInfo = uri.getUserInfo();
                String fixed = scheme + (userInfo != null && !userInfo.isBlank() ? userInfo + "@" : "") + host + ":" + port;
                System.setProperty("spring.data.redis.url", fixed);
                System.out.println("[VyroFlix] Normalized HTTP Redis URL to: " + scheme + host + ":" + port);
                return;
            } catch (Exception ignored) {}
        }

        // If it's a raw token / password (does not contain '://')
        if (!redisUrl.contains("://")) {
            String restUrl = System.getenv("UPSTASH_REDIS_REST_URL");
            if (restUrl != null && !restUrl.isBlank()) {
                try {
                    java.net.URI u = java.net.URI.create(restUrl.trim());
                    String host = u.getHost();
                    if (host != null && !host.isBlank()) {
                        String constructed = "rediss://default:" + redisUrl + "@" + host + ":6379";
                        System.setProperty("spring.data.redis.url", constructed);
                        System.out.println("[VyroFlix] Constructed valid rediss URL from UPSTASH_REDIS_REST_URL: rediss://default:***@" + host + ":6379");
                        return;
                    }
                } catch (Exception ignored) {}
            }

            String host = System.getenv("REDIS_HOST");
            if (host != null && !host.isBlank() && !host.equals("localhost")) {
                String constructed = "rediss://default:" + redisUrl + "@" + host.trim() + ":6379";
                System.setProperty("spring.data.redis.url", constructed);
                System.out.println("[VyroFlix] Constructed valid rediss URL from REDIS_HOST: rediss://default:***@" + host + ":6379");
                return;
            }

            System.setProperty("spring.data.redis.url", "redis://localhost:6379");
            System.setProperty("spring.data.redis.password", redisUrl);
            System.out.println("[VyroFlix] WARN: Invalid Redis URL detected (raw token provided). Setting fallback redis://localhost:6379 with password to prevent Lettuce scheme 'null' crash.");
        }
    }
}
