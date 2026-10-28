-- ============================================================
-- Idempotency ledger for Kafka consumers (at-least-once delivery).
-- A row is inserted in the same transaction as the side effect, so a
-- redelivered event (same event_id) is detected and skipped.
-- ============================================================
CREATE TABLE IF NOT EXISTS processed_events (
    event_id     VARCHAR(100) NOT NULL,
    consumer     VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, consumer)
);

CREATE INDEX IF NOT EXISTS idx_processed_events_processed_at ON processed_events(processed_at);
