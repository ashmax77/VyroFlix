-- ============================================================
-- VyroFlix Content Service — V2 Seed Catalog Migration
-- Schema: content
-- ============================================================

-- 1. Seed Genres
INSERT INTO genres (id, name, slug) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Sci-Fi', 'sci-fi'),
    ('a0000000-0000-0000-0000-000000000002', 'Action', 'action'),
    ('a0000000-0000-0000-0000-000000000003', 'Drama', 'drama'),
    ('a0000000-0000-0000-0000-000000000004', 'Comedy', 'comedy'),
    ('a0000000-0000-0000-0000-000000000005', 'Thriller', 'thriller'),
    ('a0000000-0000-0000-0000-000000000006', 'Horror', 'horror'),
    ('a0000000-0000-0000-0000-000000000007', 'Documentary', 'documentary'),
    ('a0000000-0000-0000-0000-000000000008', 'Fantasy', 'fantasy')
ON CONFLICT (id) DO NOTHING;

-- 2. Seed Movies
INSERT INTO titles (id, title, type, synopsis, release_year, maturity_rating, thumbnail_url, poster_url, banner_url, publication_state) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'Cosmic Odyssey', 'MOVIE',
     'A lone astronaut embarks on an interstellar expedition to find habitable worlds beyond the edge of known spacetime.',
     2024, 'PG-13',
     'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600',
     'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800',
     'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1920',
     'PUBLISHED'),

    ('b0000000-0000-0000-0000-000000000002', 'Shadow Protocol', 'MOVIE',
     'An elite operative goes rogue when a covert surveillance syndicate targets his family in the neon-lit heart of Tokyo.',
     2023, 'R',
     'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600',
     'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800',
     'https://images.unsplash.com/photo-1514565131-fce0801e5785?w=1920',
     'PUBLISHED'),

    ('b0000000-0000-0000-0000-000000000003', 'Cyberpunk 2099', 'MOVIE',
     'In a dystopian megacity run by artificial intelligence oligarchs, a cybernetic mercenary uncovers an insurrection conspiracy.',
     2025, 'R',
     'https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600',
     'https://images.unsplash.com/photo-1563089145-599997674d42?w=800',
     'https://images.unsplash.com/photo-1518770660439-4636190af475?w=1920',
     'PUBLISHED'),

    ('b0000000-0000-0000-0000-000000000004', 'The Midnight Haunting', 'MOVIE',
     'An ancient Victorian mansion holds centuries of forgotten spirits that awaken on the anniversary of an unsolved tragedy.',
     2024, 'R',
     'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600',
     'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800',
     'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1920',
     'PUBLISHED'),

    ('b0000000-0000-0000-0000-000000000005', 'Quantum Paradox', 'MOVIE',
     'A brilliant theoretical physicist accidentally splits reality into branching timelines with catastrophic consequences.',
     2024, 'PG-13',
     'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600',
     'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=800',
     'https://images.unsplash.com/photo-1518770660439-4636190af475?w=1920',
     'PUBLISHED'),

    ('b0000000-0000-0000-0000-000000000006', 'Deep Blue Abyss', 'MOVIE',
     'An awe-inspiring voyage into the Mariana Trench unveiling bioluminescent life and mysterious ocean depths.',
     2023, 'G',
     'https://images.unsplash.com/photo-1682687220063-4742bd7fd538?w=600',
     'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=800',
     'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1920',
     'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

-- 3. Seed Series
INSERT INTO titles (id, title, type, synopsis, release_year, maturity_rating, thumbnail_url, poster_url, banner_url, publication_state) VALUES
    ('b0000000-0000-0000-0000-000000000007', 'The Last Detective', 'SERIES',
     'A cynical homicide detective in London solves complex criminal conspiracies while grappling with personal grief.',
     2023, 'TV-MA',
     'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=600',
     'https://images.unsplash.com/photo-1500485035595-cbe6f645feb1?w=800',
     'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=1920',
     'PUBLISHED'),

    ('b0000000-0000-0000-0000-000000000008', 'Kingdom of Echoes', 'SERIES',
     'Nine mythical realms clash in a struggle for supreme magical hegemony across enchanted landscapes.',
     2024, 'TV-14',
     'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600',
     'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800',
     'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1920',
     'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

-- 4. Title <-> Genre mappings
INSERT INTO title_genres (title_id, genre_id) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000005'),
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000006'),
    ('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000007'),
    ('b0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000003'),
    ('b0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000005'),
    ('b0000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000008')
ON CONFLICT DO NOTHING;

-- 5. Seed Seasons & Episodes for Series
INSERT INTO seasons (id, title_id, season_number, name, overview) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000007', 1, 'Season 1', 'The mystery unfolds in London foggy streets.'),
    ('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000008', 1, 'Season 1', 'The awakening of the mythical stones.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO episodes (id, season_id, episode_number, name, overview, duration_seconds, thumbnail_url, publication_state) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 1, 'Pilot: Fog & Shadows', 'A strange discovery along the Thames pulls Detective Vance back into active duty.', 3120, 'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=600', 'PUBLISHED'),
    ('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 2, 'The Cipher Key', 'Decrypting a clandestine syndicate journal leads to unexpected betrayal.', 2940, 'https://images.unsplash.com/photo-1500485035595-cbe6f645feb1?w=600', 'PUBLISHED'),
    ('d0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002', 1, 'The Gathering of Realms', 'Emissaries from each kingdom convene under the solar eclipse.', 3400, 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600', 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

-- 6. Media Asset References (READY status with HLS master playlist paths)
INSERT INTO media_asset_references (id, target_id, asset_type, status, master_playlist_url, duration_seconds) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'MAIN_VIDEO', 'READY', 'hls/titles/cosmic-odyssey/master.m3u8', 7200),
    ('e0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 'MAIN_VIDEO', 'READY', 'hls/titles/shadow-protocol/master.m3u8', 6840),
    ('e0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 'MAIN_VIDEO', 'READY', 'hls/titles/cyberpunk-2099/master.m3u8', 8100),
    ('e0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', 'MAIN_VIDEO', 'READY', 'hls/titles/midnight-haunting/master.m3u8', 5940),
    ('e0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000005', 'MAIN_VIDEO', 'READY', 'hls/titles/quantum-paradox/master.m3u8', 7420),
    ('e0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000006', 'MAIN_VIDEO', 'READY', 'hls/titles/deep-blue-abyss/master.m3u8', 5400),
    ('e0000000-0000-0000-0000-000000000007', 'd0000000-0000-0000-0000-000000000001', 'MAIN_VIDEO', 'READY', 'hls/episodes/last-detective-s1e1/master.m3u8', 3120),
    ('e0000000-0000-0000-0000-000000000008', 'd0000000-0000-0000-0000-000000000002', 'MAIN_VIDEO', 'READY', 'hls/episodes/last-detective-s1e2/master.m3u8', 2940),
    ('e0000000-0000-0000-0000-000000000009', 'd0000000-0000-0000-0000-000000000003', 'MAIN_VIDEO', 'READY', 'hls/episodes/kingdom-echoes-s1e1/master.m3u8', 3400)
ON CONFLICT (id) DO NOTHING;
