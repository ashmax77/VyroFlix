package com.vyroflix.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * VyroFlix Platform Application.
 *
 * <p>Aggregates all Spring MVC domain services (identity, content, video,
 * streaming, history, search, and recommendation) into a single deployable
 * application to minimize resource usage in low-cost hosting environments (e.g. Render Free).</p>
 *
 * <p>Encoding-service remains separate because FFmpeg is CPU- and disk-intensive.</p>
 */
@SpringBootApplication
@ComponentScan(basePackages = {
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
}
