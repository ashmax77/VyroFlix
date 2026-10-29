package com.vyroflix.platform.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
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
            // Shared outbox table, read by the one relay for every domain (see db/migration/public in common).
            "public",
            "identity",
            "content",
            "video",
            "encoding",
            "streaming",
            "history",
            "search",
            "recommendation"
    };

    /**
     * Applies each domain's migrations in its own schema, in version order. No repair and no out-of-order
     * execution: a changed or missing migration fails the boot loudly instead of being papered over.
     * baselineOnMigrate only matters for a schema whose history table does not exist yet.
     */
    @Bean
    public FlywayMigrationStrategy multiSchemaFlywayMigrationStrategy(ObjectProvider<DataSource> dataSourceProvider) {
        return defaultFlyway -> {
            DataSource dataSource = dataSourceProvider.getIfAvailable();
            if (dataSource == null) {
                log.warn("[VyroFlix] No DataSource available; skipping schema migrations");
                return;
            }
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
                            .baselineVersion("0")
                            .outOfOrder(false)
                            .load();
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
