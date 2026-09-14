-- ============================================================
-- VyroFlix History Service — Initial Schema Migration
-- Schema: history
-- ============================================================

-- Viewing Progress (heartbeat upserts and Continue Watching)
CREATE TABLE IF NOT EXISTS viewing_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    content_id UUID NOT NULL,
    target_type VARCHAR(20) NOT NULL, -- MOVIE, EPISODE
    position_seconds INT NOT NULL DEFAULT 0,
    duration_seconds INT NOT NULL,
    progress_percent DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    last_watched_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_content_progress UNIQUE (user_id, content_id)
);

CREATE INDEX IF NOT EXISTS idx_progress_continue_watching 
    ON viewing_progress(user_id, is_completed, last_watched_at DESC);

-- Watchlist Entries
CREATE TABLE IF NOT EXISTS watchlist_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_watchlist_title UNIQUE (user_id, title_id)
);

CREATE INDEX IF NOT EXISTS idx_watchlist_user ON watchlist_entries(user_id, created_at DESC);

-- Completion Records (triggered at >= 90% progress threshold)
CREATE TABLE IF NOT EXISTS completion_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    content_id UUID NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_completion UNIQUE (user_id, content_id)
);

CREATE INDEX IF NOT EXISTS idx_completion_user ON completion_records(user_id);

-- Transactional Outbox for history events (history.progress.updated.v1, etc.)
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    producer VARCHAR(100) NOT NULL,
    correlation_id UUID NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_history_outbox_status ON outbox_events(status, created_at);
