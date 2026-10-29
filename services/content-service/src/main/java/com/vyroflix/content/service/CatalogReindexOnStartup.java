package com.vyroflix.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Announces the published catalog once the application is up. Titles seeded by migrations never produce catalog
 * events, so search and the other event-fed read models stay empty until this runs. Enable with
 * {@code vyroflix.catalog.reindex-on-startup=true}; a failure here is logged and never stops the application.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vyroflix.catalog.reindex-on-startup", havingValue = "true")
public class CatalogReindexOnStartup {

    private final AdminCatalogService catalogService;

    @EventListener(ApplicationReadyEvent.class)
    public void reindex() {
        try {
            int count = catalogService.republishPublishedTitles();
            log.info("Announced {} published title(s) for read-model catch-up", count);
        } catch (RuntimeException e) {
            log.error("Catalog reindex on startup failed: {}", e.getMessage(), e);
        }
    }
}
