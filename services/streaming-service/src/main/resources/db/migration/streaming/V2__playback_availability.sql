-- ============================================================
-- VyroFlix Streaming Service — Playback availability & key read models
-- Schema: streaming
--
-- Streaming never reads the content database. These tables are fed by
-- catalog.* and video.encoded.v1 events so playback can be authorised locally.
-- ============================================================

-- Publication state per playable id. Titles have parent_id NULL; episodes point at their series.
CREATE TABLE IF NOT EXISTS title_availability (
    content_id UUID PRIMARY KEY,
    parent_id UUID,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_title_availability_parent ON title_availability(parent_id);

-- Master playlist object key per encoded asset (source of truth; Redis is a cache in front of it).
CREATE TABLE IF NOT EXISTS master_playlist_keys (
    content_id UUID PRIMARY KEY,
    playlist_key VARCHAR(500) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Fast lookup of a user's open sessions for concurrency limits.
CREATE INDEX IF NOT EXISTS idx_playback_open_sessions ON playback_sessions(user_id) WHERE ended_at IS NULL;
