-- ============================================================
-- VyroFlix Encoding Service — Initial Schema Migration
-- Schema: encoding
-- ============================================================

-- Encoding Jobs for asynchronous FFmpeg transcoding
CREATE TABLE IF NOT EXISTS encoding_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upload_intent_id UUID NOT NULL UNIQUE,
    content_id UUID NOT NULL,
    raw_s3_bucket VARCHAR(100) NOT NULL,
    raw_s3_key VARCHAR(500) NOT NULL,
    output_s3_bucket VARCHAR(100) NOT NULL,
    output_s3_prefix VARCHAR(500) NOT NULL,
    master_playlist_key VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED', -- QUEUED, PROCESSING, READY, FAILED
    duration_seconds INT,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_encoding_jobs_status ON encoding_jobs(status);
CREATE INDEX IF NOT EXISTS idx_encoding_jobs_content ON encoding_jobs(content_id);

-- Individual HLS Renditions (e.g. 1080p, 720p, 480p)
CREATE TABLE IF NOT EXISTS hls_renditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    encoding_job_id UUID NOT NULL REFERENCES encoding_jobs(id) ON DELETE CASCADE,
    resolution VARCHAR(20) NOT NULL, -- 1080p, 720p, 480p
    bitrate INT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    playlist_key VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_renditions_job ON hls_renditions(encoding_job_id);

-- Transactional Outbox for video.encoded.v1 events
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

CREATE INDEX IF NOT EXISTS idx_encoding_outbox_status ON outbox_events(status, created_at);
