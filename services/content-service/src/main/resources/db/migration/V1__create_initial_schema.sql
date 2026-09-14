-- ============================================================
-- VyroFlix Content Service — Initial Schema Migration
-- Schema: content
-- ============================================================

-- Core Titles (Movies and Series)
CREATE TABLE IF NOT EXISTS titles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL, -- MOVIE, SERIES
    synopsis TEXT,
    release_year INT,
    maturity_rating VARCHAR(10), -- PG, PG-13, R, TV-MA, etc.
    thumbnail_url VARCHAR(500),
    poster_url VARCHAR(500),
    banner_url VARCHAR(500),
    publication_state VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, UNPUBLISHED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_titles_state_type ON titles(publication_state, type);
CREATE INDEX IF NOT EXISTS idx_titles_release_year ON titles(release_year);

-- Seasons (for Series)
CREATE TABLE IF NOT EXISTS seasons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title_id UUID NOT NULL REFERENCES titles(id) ON DELETE CASCADE,
    season_number INT NOT NULL,
    name VARCHAR(255),
    overview TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_title_season UNIQUE (title_id, season_number)
);

CREATE INDEX IF NOT EXISTS idx_seasons_title ON seasons(title_id);

-- Episodes (for Seasons)
CREATE TABLE IF NOT EXISTS episodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    season_id UUID NOT NULL REFERENCES seasons(id) ON DELETE CASCADE,
    episode_number INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    overview TEXT,
    duration_seconds INT,
    thumbnail_url VARCHAR(500),
    publication_state VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_season_episode UNIQUE (season_id, episode_number)
);

CREATE INDEX IF NOT EXISTS idx_episodes_season ON episodes(season_id);

-- Genres Catalog
CREATE TABLE IF NOT EXISTS genres (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE
);

-- Title <-> Genre Association
CREATE TABLE IF NOT EXISTS title_genres (
    title_id UUID NOT NULL REFERENCES titles(id) ON DELETE CASCADE,
    genre_id UUID NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (title_id, genre_id)
);

-- People (Cast and Crew)
CREATE TABLE IF NOT EXISTS persons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500)
);

-- Title Credits
CREATE TABLE IF NOT EXISTS title_credits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title_id UUID NOT NULL REFERENCES titles(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL, -- ACTOR, DIRECTOR, WRITER, PRODUCER
    character_name VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_credits_title ON title_credits(title_id);
CREATE INDEX IF NOT EXISTS idx_credits_person ON title_credits(person_id);

-- Media Asset References (Links Content entities to Encoded Video Assets)
CREATE TABLE IF NOT EXISTS media_asset_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_id UUID NOT NULL, -- title_id for movie, episode_id for series
    asset_type VARCHAR(20) NOT NULL, -- MAIN_VIDEO, TRAILER
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, READY, FAILED
    master_playlist_url VARCHAR(500),
    duration_seconds INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_media_target ON media_asset_references(target_id, asset_type);

-- Curated Collections / Rails (e.g. "Trending Now", "Action Blockbusters")
CREATE TABLE IF NOT EXISTS collections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    display_order INT NOT NULL DEFAULT 0
);

-- Collection <-> Title Association
CREATE TABLE IF NOT EXISTS collection_titles (
    collection_id UUID NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    title_id UUID NOT NULL REFERENCES titles(id) ON DELETE CASCADE,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (collection_id, title_id)
);

-- Transactional Outbox for Catalog publication events
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

CREATE INDEX IF NOT EXISTS idx_content_outbox_status ON outbox_events(status, created_at);
