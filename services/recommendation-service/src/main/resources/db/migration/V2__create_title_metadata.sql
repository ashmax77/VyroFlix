-- ============================================================
-- VyroFlix Recommendation Service — Title Metadata Migration
-- Schema: recommendation
-- ============================================================

CREATE TABLE IF NOT EXISTS title_metadata (
    title_id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'MOVIE',
    genres TEXT[] DEFAULT '{}',
    poster_url VARCHAR(500),
    release_year INT,
    maturity_rating VARCHAR(10),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_title_metadata_type ON title_metadata(type);
