-- ============================================================
-- VyroFlix Search Service — Pin the search_path of the trigger function
-- Schema: search
-- Idempotent: ALTER FUNCTION can be re-applied safely.
-- An empty search_path stops a role-controlled schema from shadowing the
-- functions the body calls (to_tsvector, setweight, coalesce, array_to_string
-- all resolve from pg_catalog, which is always searched first).
-- ============================================================

ALTER FUNCTION search.update_search_vector() SET search_path = '';
