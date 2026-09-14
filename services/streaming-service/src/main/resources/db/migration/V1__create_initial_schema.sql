-- ============================================================
-- VyroFlix Streaming Service — Initial Schema Migration
-- Schema: streaming
-- ============================================================

-- Playback Sessions for auditing, concurrent stream limits, and tracking
CREATE TABLE IF NOT EXISTS playback_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    content_id UUID NOT NULL,
    target_type VARCHAR(20) NOT NULL, -- MOVIE, EPISODE
    device_id VARCHAR(255),
    ip_address VARCHAR(45),
    master_playlist_url TEXT NOT NULL,
    resume_position_seconds INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_playback_user ON playback_sessions(user_id, expires_at);
CREATE INDEX IF NOT EXISTS idx_playback_content ON playback_sessions(content_id);

-- Transactional Outbox for playback.started.v1 / playback.ended.v1 events
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

CREATE INDEX IF NOT EXISTS idx_streaming_outbox_status ON outbox_events(status, created_at);
