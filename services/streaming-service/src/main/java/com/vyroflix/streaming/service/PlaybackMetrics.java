package com.vyroflix.streaming.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Prometheus counters for playback authorisation and master-key cache effectiveness.
 *
 * <p>Only low-cardinality tags are used (outcome and reason). Never tag with user, content or session ids.</p>
 */
@Component
public class PlaybackMetrics {

    static final String AUTHORIZATION = "vyroflix.playback.authorization";
    static final String KEY_LOOKUP = "vyroflix.playback.master_key.lookup";

    private final MeterRegistry registry;

    public PlaybackMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void allowed() {
        registry.counter(AUTHORIZATION, "outcome", "allowed", "reason", "none").increment();
    }

    public void denied(String reason) {
        registry.counter(AUTHORIZATION, "outcome", "denied", "reason", reason).increment();
    }

    public void keyLookup(boolean cacheHit) {
        registry.counter(KEY_LOOKUP, "result", cacheHit ? "hit" : "miss").increment();
    }
}
