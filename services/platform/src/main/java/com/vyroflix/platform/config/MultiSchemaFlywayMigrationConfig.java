package com.vyroflix.platform.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Executes Flyway migrations per-schema across all microservice domains
 * within the combined platform deployment.
 * Prevents version collision between multiple V1__ migrations across services.
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class MultiSchemaFlywayMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(MultiSchemaFlywayMigrationConfig.class);

    private static final String[] SCHEMAS = {
            "identity",
            "content",
            "video",
            "encoding",
            "streaming",
            "history",
            "search",
            "recommendation"
    };

    @Bean
    public FlywayMigrationStrategy multiSchemaFlywayMigrationStrategy(DataSource dataSource) {
        return defaultFlyway -> {
            for (String schema : SCHEMAS) {
                try {
                    log.info("[VyroFlix] Executing Flyway migration for schema: {}", schema);
                    Flyway flyway = Flyway.configure()
                            .dataSource(dataSource)
                            .schemas(schema)
                            .defaultSchema(schema)
                            .table("flyway_schema_history")
                            .locations("classpath:db/migration/" + schema)
                            .baselineOnMigrate(true)
                            .outOfOrder(true)
                            .load();
                    flyway.repair();
                    flyway.migrate();
                    log.info("[VyroFlix] Completed Flyway migration for schema: {}", schema);
                } catch (Exception e) {
                    log.error("[VyroFlix] Flyway migration failed for schema {}: {}", schema, e.getMessage(), e);
                    throw e;
                }
            }
        };
    }
}
