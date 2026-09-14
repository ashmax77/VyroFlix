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
        SpringApplication.run(PlatformApplication.class, args);
    }
}
