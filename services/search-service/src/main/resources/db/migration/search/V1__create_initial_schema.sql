-- ============================================================
-- VyroFlix Search Service — Initial Schema Migration
-- Schema: search
-- ============================================================

-- Denormalized Search Documents
CREATE TABLE IF NOT EXISTS search_documents (
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

-- Full-text GIN index for high-performance searching
CREATE INDEX IF NOT EXISTS idx_search_vector ON search_documents USING GIN(search_vector);
CREATE INDEX IF NOT EXISTS idx_search_type ON search_documents(type);
CREATE INDEX IF NOT EXISTS idx_search_year ON search_documents(release_year);

-- Function to automatically regenerate search_vector on insert/update
CREATE OR REPLACE FUNCTION update_search_vector() RETURNS trigger AS $$
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

CREATE OR REPLACE TRIGGER trg_search_documents_vector
BEFORE INSERT OR UPDATE ON search_documents
FOR EACH ROW EXECUTE FUNCTION update_search_vector();
