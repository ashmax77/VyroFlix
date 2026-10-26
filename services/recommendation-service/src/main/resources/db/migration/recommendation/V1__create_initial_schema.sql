-- ============================================================
-- VyroFlix Recommendation Service — Initial Schema Migration
-- Schema: recommendation
-- ============================================================

-- Trending Scores for instant trending rail calculation
CREATE TABLE IF NOT EXISTS trending_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title_id UUID NOT NULL UNIQUE,
    view_count INT NOT NULL DEFAULT 0,
    completion_count INT NOT NULL DEFAULT 0,
    score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trending_score_desc ON trending_scores(score DESC);

-- Recommendation Cache for personalized user rails
CREATE TABLE IF NOT EXISTS recommendation_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    rail_type VARCHAR(50) NOT NULL, -- BECAUSE_YOU_WATCHED, TOP_PICKS, GENRE_POPULAR
    title_ids JSONB NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_rail UNIQUE (user_id, rail_type)
);

CREATE INDEX IF NOT EXISTS idx_rec_cache_user_expires ON recommendation_cache(user_id, expires_at);
