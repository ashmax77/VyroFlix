-- ============================================================
-- VyroFlix — Complete Supabase PostgreSQL Schema & Catalog Seed
-- Run this script in: https://supabase.com/dashboard/project/sxyrwfjwdxsbiuqmvvac/sql
-- ============================================================

-- 0. Required Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Dedicated Service Schemas (Preserving Microservice Boundaries)
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS content;
CREATE SCHEMA IF NOT EXISTS video;
CREATE SCHEMA IF NOT EXISTS encoding;
CREATE SCHEMA IF NOT EXISTS streaming;
CREATE SCHEMA IF NOT EXISTS history;
CREATE SCHEMA IF NOT EXISTS search;
CREATE SCHEMA IF NOT EXISTS recommendation;

-- ------------------------------------------------------------
-- 2. IDENTITY SERVICE SCHEMA
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS identity.user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES identity.users(id) ON DELETE CASCADE,
    display_name VARCHAR(100),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS identity.user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_identity_user_role UNIQUE (user_id, role)
);

CREATE TABLE IF NOT EXISTS identity.device_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    device_id VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    ip_address VARCHAR(45),
    last_active_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_identity_user_device UNIQUE (user_id, device_id)
);

CREATE TABLE IF NOT EXISTS identity.outbox_events (
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

CREATE INDEX IF NOT EXISTS idx_identity_outbox_status ON identity.outbox_events(status, created_at);
CREATE INDEX IF NOT EXISTS idx_identity_device_user ON identity.device_sessions(user_id);

-- ------------------------------------------------------------
-- 3. CONTENT SERVICE SCHEMA
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS content.titles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL, -- MOVIE, SERIES
    synopsis TEXT,
    release_year INT,
    maturity_rating VARCHAR(10),
    thumbnail_url VARCHAR(500),
    poster_url VARCHAR(500),
    banner_url VARCHAR(500),
    publication_state VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, UNPUBLISHED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_titles_state_type ON content.titles(publication_state, type);
CREATE INDEX IF NOT EXISTS idx_titles_release_year ON content.titles(release_year);

CREATE TABLE IF NOT EXISTS content.seasons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title_id UUID NOT NULL REFERENCES content.titles(id) ON DELETE CASCADE,
    season_number INT NOT NULL,
    title VARCHAR(255),
    synopsis TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_season_title_number UNIQUE (title_id, season_number)
);

CREATE INDEX IF NOT EXISTS idx_seasons_title ON content.seasons(title_id);

CREATE TABLE IF NOT EXISTS content.episodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    season_id UUID NOT NULL REFERENCES content.seasons(id) ON DELETE CASCADE,
    episode_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    synopsis TEXT,
    duration_minutes INT NOT NULL,
    video_asset_id UUID,
    thumbnail_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_episode_season_number UNIQUE (season_id, episode_number)
);

CREATE INDEX IF NOT EXISTS idx_episodes_season ON content.episodes(season_id);

CREATE TABLE IF NOT EXISTS content.genres (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS content.title_genres (
    title_id UUID NOT NULL REFERENCES content.titles(id) ON DELETE CASCADE,
    genre_id UUID NOT NULL REFERENCES content.genres(id) ON DELETE CASCADE,
    PRIMARY KEY (title_id, genre_id)
);

CREATE TABLE IF NOT EXISTS content.movie_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title_id UUID NOT NULL UNIQUE REFERENCES content.titles(id) ON DELETE CASCADE,
    duration_minutes INT NOT NULL,
    video_asset_id UUID,
    master_playlist_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS content.outbox_events (
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

CREATE INDEX IF NOT EXISTS idx_content_outbox_status ON content.outbox_events(status, created_at);

-- ------------------------------------------------------------
-- 4. VIDEO SERVICE SCHEMA
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS video.upload_intents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_id UUID NOT NULL,
    target_type VARCHAR(20) NOT NULL, -- MOVIE, EPISODE, TRAILER
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT,
    s3_bucket VARCHAR(100) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, UPLOADED, FAILED, EXPIRED
    upload_url TEXT,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_upload_intents_content ON video.upload_intents(content_id);
CREATE INDEX IF NOT EXISTS idx_upload_intents_status ON video.upload_intents(status);

CREATE TABLE IF NOT EXISTS video.outbox_events (
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

CREATE INDEX IF NOT EXISTS idx_video_outbox_status ON video.outbox_events(status, created_at);

-- ------------------------------------------------------------
-- 5. ENCODING SERVICE SCHEMA
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS encoding.encoding_jobs (
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

CREATE INDEX IF NOT EXISTS idx_encoding_jobs_status ON encoding.encoding_jobs(status);
CREATE INDEX IF NOT EXISTS idx_encoding_jobs_content ON encoding.encoding_jobs(content_id);

CREATE TABLE IF NOT EXISTS encoding.hls_renditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    encoding_job_id UUID NOT NULL REFERENCES encoding.encoding_jobs(id) ON DELETE CASCADE,
    resolution VARCHAR(20) NOT NULL, -- 1080p, 720p, 480p
    bitrate INT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    playlist_key VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_renditions_job ON encoding.hls_renditions(encoding_job_id);

CREATE TABLE IF NOT EXISTS encoding.outbox_events (
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

CREATE INDEX IF NOT EXISTS idx_encoding_outbox_status ON encoding.outbox_events(status, created_at);

-- ------------------------------------------------------------
-- 6. STREAMING SERVICE SCHEMA
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS streaming.playback_sessions (
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

CREATE INDEX IF NOT EXISTS idx_playback_user ON streaming.playback_sessions(user_id, expires_at);
CREATE INDEX IF NOT EXISTS idx_playback_content ON streaming.playback_sessions(content_id);

CREATE TABLE IF NOT EXISTS streaming.outbox_events (
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

CREATE INDEX IF NOT EXISTS idx_streaming_outbox_status ON streaming.outbox_events(status, created_at);

-- ------------------------------------------------------------
-- 7. HISTORY SERVICE SCHEMA
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS history.viewing_progress (
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
    ON history.viewing_progress(user_id, is_completed, last_watched_at DESC);

CREATE TABLE IF NOT EXISTS history.watchlist_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_watchlist_title UNIQUE (user_id, title_id)
);

CREATE INDEX IF NOT EXISTS idx_watchlist_user ON history.watchlist_entries(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS history.completion_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    content_id UUID NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_completion UNIQUE (user_id, content_id)
);

CREATE INDEX IF NOT EXISTS idx_completion_user ON history.completion_records(user_id);

CREATE TABLE IF NOT EXISTS history.outbox_events (
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

CREATE INDEX IF NOT EXISTS idx_history_outbox_status ON history.outbox_events(status, created_at);

-- ------------------------------------------------------------
-- 8. SEARCH SERVICE SCHEMA
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS search.search_documents (
    id UUID PRIMARY KEY, -- matches catalog title_id
    title VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL, -- MOVIE, SERIES
    synopsis TEXT,
    release_year INT,
    maturity_rating VARCHAR(10),
    genres TEXT[] DEFAULT '{}',
    cast_members TEXT[] DEFAULT '{}',
    directors TEXT[] DEFAULT '{}',
    poster_url VARCHAR(500),
    publication_state VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    search_vector TSVECTOR,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_search_vector ON search.search_documents USING GIN(search_vector);
CREATE INDEX IF NOT EXISTS idx_search_type ON search.search_documents(type);
CREATE INDEX IF NOT EXISTS idx_search_year ON search.search_documents(release_year);

CREATE OR REPLACE FUNCTION search.update_search_vector() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', coalesce(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(array_to_string(NEW.genres, ' '), '')), 'B') ||
        setweight(to_tsvector('english', coalesce(array_to_string(NEW.cast_members, ' '), '')), 'C') ||
        setweight(to_tsvector('english', coalesce(array_to_string(NEW.directors, ' '), '')), 'C') ||
        setweight(to_tsvector('english', coalesce(NEW.synopsis, '')), 'D');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_search_documents_vector ON search.search_documents;
CREATE TRIGGER trg_search_documents_vector
BEFORE INSERT OR UPDATE ON search.search_documents
FOR EACH ROW EXECUTE FUNCTION search.update_search_vector();

-- ------------------------------------------------------------
-- 9. RECOMMENDATION SERVICE SCHEMA
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recommendation.trending_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title_id UUID NOT NULL UNIQUE,
    view_count INT NOT NULL DEFAULT 0,
    completion_count INT NOT NULL DEFAULT 0,
    score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trending_score_desc ON recommendation.trending_scores(score DESC);

CREATE TABLE IF NOT EXISTS recommendation.title_metadata (
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

CREATE INDEX IF NOT EXISTS idx_title_metadata_type ON recommendation.title_metadata(type);

CREATE TABLE IF NOT EXISTS recommendation.recommendation_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    rail_type VARCHAR(50) NOT NULL,
    title_ids JSONB NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_rail UNIQUE (user_id, rail_type)
);

CREATE INDEX IF NOT EXISTS idx_rec_cache_user_expires ON recommendation.recommendation_cache(user_id, expires_at);

-- ------------------------------------------------------------
-- 10. SEED DATA (Content, Genres, Movies, Series, Episodes)
-- ------------------------------------------------------------
INSERT INTO content.genres (id, name, slug) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Sci-Fi', 'sci-fi'),
    ('a0000000-0000-0000-0000-000000000002', 'Action', 'action'),
    ('a0000000-0000-0000-0000-000000000003', 'Drama', 'drama'),
    ('a0000000-0000-0000-0000-000000000004', 'Comedy', 'comedy'),
    ('a0000000-0000-0000-0000-000000000005', 'Thriller', 'thriller'),
    ('a0000000-0000-0000-0000-000000000006', 'Horror', 'horror'),
    ('a0000000-0000-0000-0000-000000000007', 'Documentary', 'documentary'),
    ('a0000000-0000-0000-0000-000000000008', 'Fantasy', 'fantasy')
ON CONFLICT (id) DO NOTHING;

-- Seed Movies
INSERT INTO content.titles (id, title, type, synopsis, release_year, maturity_rating, thumbnail_url, poster_url, banner_url, publication_state) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'Cosmic Odyssey: Beyond the Veil', 'MOVIE',
     'A lone astronaut embarks on an interstellar expedition to find habitable worlds beyond the edge of known spacetime.',
     2024, 'PG-13',
     'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600',
     'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=800',
     'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1920',
     'PUBLISHED'),

    ('b0000000-0000-0000-0000-000000000002', 'Cybernetic Shadows', 'MOVIE',
     'An elite memory broker goes rogue when a covert neural network syndicate targets his family in the neon-lit heart of Tokyo.',
     2023, 'R',
     'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=600',
     'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800',
     'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1920',
     'PUBLISHED'),

    ('b0000000-0000-0000-0000-000000000003', 'Echoes of the Deep', 'MOVIE',
     'An oceanic research team deep within the Mariana Trench discovers bioluminescent ruins and an ancient intelligence.',
     2023, 'PG-13',
     'https://images.unsplash.com/photo-1682687220063-4742bd7fd538?w=600',
     'https://images.unsplash.com/photo-1682687220063-4742bd7fd538?w=800',
     'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1920',
     'PUBLISHED'),

    ('b0000000-0000-0000-0000-000000000005', 'The Silent Sentinel', 'MOVIE',
     'A lone sniper stationed on an atmospheric defense platform faces psychological warfare during a planetary blackout.',
     2022, 'R',
     'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600',
     'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800',
     'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1920',
     'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

-- Seed Movie Assets & Durations
INSERT INTO content.movie_assets (title_id, duration_minutes, master_playlist_url) VALUES
    ('b0000000-0000-0000-0000-000000000001', 142, 'movies/b0000000-0000-0000-0000-000000000001/hls/master.m3u8'),
    ('b0000000-0000-0000-0000-000000000002', 118, 'movies/b0000000-0000-0000-0000-000000000002/hls/master.m3u8'),
    ('b0000000-0000-0000-0000-000000000003', 126, 'movies/b0000000-0000-0000-0000-000000000003/hls/master.m3u8'),
    ('b0000000-0000-0000-0000-000000000005', 105, 'movies/b0000000-0000-0000-0000-000000000005/hls/master.m3u8')
ON CONFLICT (title_id) DO NOTHING;

-- Seed Series
INSERT INTO content.titles (id, title, type, synopsis, release_year, maturity_rating, thumbnail_url, poster_url, banner_url, publication_state) VALUES
    ('b0000000-0000-0000-0000-000000000004', 'Chronicles of Neo-Kyoto', 'SERIES',
     'A generational saga tracing three families fighting for control of synthetic biology and orbital elevators across centuries.',
     2024, 'TV-MA',
     'https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600',
     'https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800',
     'https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=1920',
     'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO content.seasons (id, title_id, season_number, title, synopsis) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004', 1, 'Season 1: Awakening', 'The orbital tether suffers a catastrophic harmonic vibration.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO content.episodes (id, season_id, episode_number, title, synopsis, duration_minutes) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 1, 'Protocol Zero', 'The orbital tether suffers a catastrophic harmonic vibration.', 58),
    ('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 2, 'Synthetic Dawn', 'Rival factions race to secure the AI neural core from the wreckage.', 52)
ON CONFLICT (id) DO NOTHING;

-- Seed Title Genres
INSERT INTO content.title_genres (title_id, genre_id) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000005'),
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003'),
    ('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000003'),
    ('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000005')
ON CONFLICT DO NOTHING;

-- Populate Search Documents from seeded titles
INSERT INTO search.search_documents (id, title, type, synopsis, release_year, maturity_rating, genres, poster_url) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'Cosmic Odyssey: Beyond the Veil', 'MOVIE', 'A lone astronaut embarks on an interstellar expedition to find habitable worlds beyond the edge of known spacetime.', 2024, 'PG-13', ARRAY['Sci-Fi', 'Action'], 'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=800'),
    ('b0000000-0000-0000-0000-000000000002', 'Cybernetic Shadows', 'MOVIE', 'An elite memory broker goes rogue when a covert neural network syndicate targets his family in the neon-lit heart of Tokyo.', 2023, 'R', ARRAY['Action', 'Thriller', 'Cyberpunk'], 'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800'),
    ('b0000000-0000-0000-0000-000000000003', 'Echoes of the Deep', 'MOVIE', 'An oceanic research team deep within the Mariana Trench discovers bioluminescent ruins and an ancient intelligence.', 2023, 'PG-13', ARRAY['Sci-Fi', 'Drama'], 'https://images.unsplash.com/photo-1682687220063-4742bd7fd538?w=800'),
    ('b0000000-0000-0000-0000-000000000004', 'Chronicles of Neo-Kyoto', 'SERIES', 'A generational saga tracing three families fighting for control of synthetic biology and orbital elevators across centuries.', 2024, 'TV-MA', ARRAY['Sci-Fi', 'Drama'], 'https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800'),
    ('b0000000-0000-0000-0000-000000000005', 'The Silent Sentinel', 'MOVIE', 'A lone sniper stationed on an atmospheric defense platform faces psychological warfare during a planetary blackout.', 2022, 'R', ARRAY['Action', 'Thriller'], 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800')
ON CONFLICT (id) DO NOTHING;

-- Populate Recommendation Metadata
INSERT INTO recommendation.title_metadata (title_id, title, type, genres, poster_url, release_year, maturity_rating) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'Cosmic Odyssey: Beyond the Veil', 'MOVIE', ARRAY['Sci-Fi', 'Action'], 'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=800', 2024, 'PG-13'),
    ('b0000000-0000-0000-0000-000000000002', 'Cybernetic Shadows', 'MOVIE', ARRAY['Action', 'Thriller', 'Cyberpunk'], 'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800', 2023, 'R'),
    ('b0000000-0000-0000-0000-000000000003', 'Echoes of the Deep', 'MOVIE', ARRAY['Sci-Fi', 'Drama'], 'https://images.unsplash.com/photo-1682687220063-4742bd7fd538?w=800', 2023, 'PG-13'),
    ('b0000000-0000-0000-0000-000000000004', 'Chronicles of Neo-Kyoto', 'SERIES', ARRAY['Sci-Fi', 'Drama'], 'https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800', 2024, 'TV-MA'),
    ('b0000000-0000-0000-0000-000000000005', 'The Silent Sentinel', 'MOVIE', ARRAY['Action', 'Thriller'], 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800', 2022, 'R')
ON CONFLICT (title_id) DO NOTHING;

-- Seed Trending Scores
INSERT INTO recommendation.trending_scores (title_id, view_count, completion_count, score) VALUES
    ('b0000000-0000-0000-0000-000000000001', 1250, 940, 98.5),
    ('b0000000-0000-0000-0000-000000000002', 980, 720, 89.2),
    ('b0000000-0000-0000-0000-000000000004', 850, 610, 84.1),
    ('b0000000-0000-0000-0000-000000000003', 640, 430, 76.8),
    ('b0000000-0000-0000-0000-000000000005', 510, 310, 68.4)
ON CONFLICT (title_id) DO NOTHING;
