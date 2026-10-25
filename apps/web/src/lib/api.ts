import { HomeFeed, Title, ViewingProgress, WatchlistEntry, PlaybackSession, Genre, UploadIntentResponse, EncodingJob, EncodingStatus, SearchResultPage, SearchCard } from '../types';

const API_BASE = '/api/v1';

// Seed fallback data matching V2__seed_catalog.sql
export const MOCK_TITLES: Title[] = [
  {
    id: 'a0000000-0000-0000-0000-000000000001',
    type: 'MOVIE',
    title: 'Cosmic Odyssey: Beyond the Veil',
    synopsis: 'A crew of deep-space cartographers breaches a luminous anomaly at the edge of known space, discovering ancient megastructures that defy spacetime physics.',
    releaseYear: 2024,
    maturityRating: 'PG-13',
    durationMinutes: 142,
    posterUrl: 'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80',
    backdropUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1600&auto=format&fit=crop&q=80',
    publicationState: 'PUBLISHED',
    tags: ['Sci-Fi', 'Space', 'Adventure', 'Mystery'],
  },
  {
    id: 'a0000000-0000-0000-0000-000000000002',
    type: 'MOVIE',
    title: 'Cybernetic Shadows',
    synopsis: 'In a neon-drenched metropolis ruled by neural network corporations, a memory broker is framed for assassinating the city architect.',
    releaseYear: 2023,
    maturityRating: 'R',
    durationMinutes: 118,
    posterUrl: 'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=600&auto=format&fit=crop&q=80',
    backdropUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&auto=format&fit=crop&q=80',
    publicationState: 'PUBLISHED',
    tags: ['Cyberpunk', 'Action', 'Thriller'],
  },
  {
    id: 'a0000000-0000-0000-0000-000000000003',
    type: 'MOVIE',
    title: 'Echoes of the Deep',
    synopsis: 'An oceanic expedition at the bottom of the Mariana Trench discovers bioluminescent ruins and an intelligent species hidden beneath the mantle.',
    releaseYear: 2023,
    maturityRating: 'PG-13',
    durationMinutes: 126,
    posterUrl: 'https://images.unsplash.com/photo-1682687220063-4742bd7fd538?w=600&auto=format&fit=crop&q=80',
    backdropUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&auto=format&fit=crop&q=80',
    publicationState: 'PUBLISHED',
    tags: ['Mystery', 'Drama', 'Adventure'],
  },
  {
    id: 'a0000000-0000-0000-0000-000000000004',
    type: 'SERIES',
    title: 'Chronicles of Neo-Kyoto',
    synopsis: 'A generational saga tracing three families fighting for control of synthetic biology and orbital elevators across three centuries.',
    releaseYear: 2024,
    maturityRating: 'TV-MA',
    durationMinutes: 55,
    posterUrl: 'https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80',
    backdropUrl: 'https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=1600&auto=format&fit=crop&q=80',
    publicationState: 'PUBLISHED',
    tags: ['Sci-Fi', 'Drama', 'Binge Series'],
    seasons: [
      {
        id: 's0000000-0000-0000-0000-000000000001',
        titleId: 'a0000000-0000-0000-0000-000000000004',
        seasonNumber: 1,
        title: 'Season 1: Awakening',
        episodes: [
          {
            id: 'e0000000-0000-0000-0000-000000000001',
            seasonId: 's0000000-0000-0000-0000-000000000001',
            episodeNumber: 1,
            title: 'Protocol Zero',
            synopsis: 'The orbital tether suffers a catastrophic harmonic vibration.',
            durationMinutes: 58,
          },
          {
            id: 'e0000000-0000-0000-0000-000000000002',
            seasonId: 's0000000-0000-0000-0000-000000000001',
            episodeNumber: 2,
            title: 'Synthetic Dawn',
            synopsis: 'Rival factions race to secure the AI neural core from the wreckage.',
            durationMinutes: 52,
          },
        ],
      },
    ],
  },
  {
    id: 'a0000000-0000-0000-0000-000000000005',
    type: 'MOVIE',
    title: 'The Silent Sentinel',
    synopsis: 'A lone sniper stationed on an atmospheric defense platform faces psychological warfare during a planet-wide communications blackout.',
    releaseYear: 2022,
    maturityRating: 'R',
    durationMinutes: 105,
    posterUrl: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80',
    backdropUrl: 'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1600&auto=format&fit=crop&q=80',
    publicationState: 'PUBLISHED',
    tags: ['Action', 'Thriller', 'Military'],
  },
];

export const MOCK_GENRES: Genre[] = [
  { id: 'g1', name: 'Action', slug: 'action' },
  { id: 'g2', name: 'Sci-Fi', slug: 'sci-fi' },
  { id: 'g3', name: 'Drama', slug: 'drama' },
  { id: 'g4', name: 'Thriller', slug: 'thriller' },
  { id: 'g5', name: 'Comedy', slug: 'comedy' },
  { id: 'g6', name: 'Documentary', slug: 'documentary' },
];

export async function fetchHomeFeed(): Promise<HomeFeed> {
  try {
    const res = await fetch(`${API_BASE}/catalog/home`, { cache: 'no-store' });
    if (res.ok) {
      return await res.json();
    }
  } catch {
    // fallback gracefully
  }

  return {
    featuredTitle: MOCK_TITLES[0],
    rails: [
      {
        id: 'trending-movies',
        title: 'Trending Movies',
        type: 'TRENDING_MOVIES',
        items: MOCK_TITLES.filter((t) => t.type === 'MOVIE'),
      },
      {
        id: 'binge-series',
        title: 'Popular Series',
        type: 'BINGE_SERIES',
        items: MOCK_TITLES.filter((t) => t.type === 'SERIES'),
      },
      {
        id: 'scifi-favorites',
        title: 'Sci-Fi & Cyberpunk Hits',
        type: 'GENRE',
        items: MOCK_TITLES,
      },
    ],
  };
}

export async function fetchTitle(id: string): Promise<Title | null> {
  try {
    const res = await fetch(`${API_BASE}/catalog/titles/${id}`);
    if (res.ok) {
      return await res.json();
    }
  } catch {
    // fallback
  }
  return MOCK_TITLES.find((t) => t.id === id) || MOCK_TITLES[0];
}

export async function createPlaybackSession(
  contentId: string,
  targetType: 'MOVIE' | 'EPISODE' = 'MOVIE'
): Promise<PlaybackSession> {
  const userId = getStoredUserId();
  try {
    const res = await fetch(`${API_BASE}/streaming/sessions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': userId,
      },
      body: JSON.stringify({
        contentId,
        targetType,
        deviceId: 'web-browser',
      }),
    });
    if (res.ok) {
      return await res.json();
    }
  } catch {
    // fallback
  }

  // Fallback demo stream for local testing without minio/CDN
  return {
    sessionId: 'session-demo-' + Date.now(),
    manifestUrl: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8',
    expiresAt: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
    resumePositionSeconds: 0,
  };
}

export async function saveProgress(data: {
  contentId: string;
  targetType: 'MOVIE' | 'EPISODE';
  positionSeconds: number;
  durationSeconds: number;
}): Promise<ViewingProgress | null> {
  const userId = getStoredUserId();
  try {
    const res = await fetch(`${API_BASE}/history/progress`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': userId,
      },
      body: JSON.stringify(data),
    });
    if (res.ok) {
      return await res.json();
    }
  } catch {
    // fallback
  }
  return null;
}

export async function fetchContinueWatching(): Promise<ViewingProgress[]> {
  const userId = getStoredUserId();
  try {
    const res = await fetch(`${API_BASE}/history/continue-watching`, {
      headers: { 'X-User-Id': userId },
    });
    if (res.ok) {
      const data: ViewingProgress[] = await res.json();
      return data.map((item) => ({
        ...item,
        title: MOCK_TITLES.find((t) => t.id === item.contentId) || MOCK_TITLES[0],
      }));
    }
  } catch {
    // fallback
  }

  return [
    {
      id: 'p1',
      userId,
      contentId: MOCK_TITLES[0].id,
      targetType: 'MOVIE',
      positionSeconds: 2420,
      durationSeconds: 8520,
      progressPercent: 28.4,
      completed: false,
      lastWatchedAt: new Date().toISOString(),
      title: MOCK_TITLES[0],
    },
    {
      id: 'p2',
      userId,
      contentId: MOCK_TITLES[1].id,
      targetType: 'MOVIE',
      positionSeconds: 4100,
      durationSeconds: 7080,
      progressPercent: 57.9,
      completed: false,
      lastWatchedAt: new Date(Date.now() - 3600000).toISOString(),
      title: MOCK_TITLES[1],
    },
  ];
}

export async function fetchWatchlist(): Promise<WatchlistEntry[]> {
  const userId = getStoredUserId();
  try {
    const res = await fetch(`${API_BASE}/history/watchlist`, {
      headers: { 'X-User-Id': userId },
    });
    if (res.ok) {
      const data: WatchlistEntry[] = await res.json();
      return data.map((entry) => ({
        ...entry,
        title: MOCK_TITLES.find((t) => t.id === entry.titleId),
      }));
    }
  } catch {
    // fallback
  }

  return [
    {
      id: 'w1',
      userId,
      titleId: MOCK_TITLES[0].id,
      createdAt: new Date().toISOString(),
      title: MOCK_TITLES[0],
    },
    {
      id: 'w2',
      userId,
      titleId: MOCK_TITLES[2].id,
      createdAt: new Date().toISOString(),
      title: MOCK_TITLES[2],
    },
  ];
}

export async function toggleWatchlist(titleId: string, inWatchlist: boolean): Promise<boolean> {
  const userId = getStoredUserId();
  try {
    if (inWatchlist) {
      await fetch(`${API_BASE}/history/watchlist/${titleId}`, {
        method: 'DELETE',
        headers: { 'X-User-Id': userId },
      });
      return false;
    } else {
      await fetch(`${API_BASE}/history/watchlist`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Id': userId,
        },
        body: JSON.stringify({ titleId }),
      });
      return true;
    }
  } catch {
    return !inWatchlist;
  }
}

export async function checkWatchlist(titleId: string): Promise<boolean> {
  const userId = getStoredUserId();
  try {
    const res = await fetch(`${API_BASE}/history/watchlist/${titleId}/check`, {
      headers: { 'X-User-Id': userId },
    });
    if (res.ok) {
      const data = await res.json();
      return data.inWatchlist;
    }
  } catch {
    // fallback
  }
  return false;
}

let cachedAuthToken: string | null = null;

export function setStoredAuthToken(token: string | null) {
  cachedAuthToken = token;
  if (typeof window !== 'undefined') {
    if (token) {
      localStorage.setItem('vyroflix_auth_token', token);
    } else {
      localStorage.removeItem('vyroflix_auth_token');
    }
  }
}

export function getStoredAuthToken(): string | null {
  if (cachedAuthToken) return cachedAuthToken;
  if (typeof window !== 'undefined') {
    return localStorage.getItem('vyroflix_auth_token');
  }
  return null;
}

export function setStoredUserId(id: string | null) {
  if (typeof window !== 'undefined') {
    if (id) {
      localStorage.setItem('vyroflix_user_id', id);
    } else {
      localStorage.removeItem('vyroflix_user_id');
    }
  }
}

export function getStoredUserId(): string {
  if (typeof window !== 'undefined') {
    let id = localStorage.getItem('vyroflix_user_id');
    if (!id) {
      id = '00000000-0000-0000-0000-000000000001';
      localStorage.setItem('vyroflix_user_id', id);
    }
    return id;
  }
  return '00000000-0000-0000-0000-000000000001';
}

export function getAuthHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    'X-User-Id': getStoredUserId(),
  };
  const token = getStoredAuthToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

/**
 * Creates an upload intent for a movie or episode asset.
 */
export async function createUploadIntent(data: {
  contentId: string;
  targetType: 'MOVIE' | 'EPISODE';
  filename: string;
  contentType: string;
  fileSizeBytes: number;
}): Promise<UploadIntentResponse> {
  const userId = getStoredUserId();
  const res = await fetch(`${API_BASE}/videos/upload-intents`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': userId,
    },
    body: JSON.stringify(data),
  });

  if (!res.ok) {
    throw new Error(`Failed to create upload intent: ${res.statusText}`);
  }

  return res.json();
}

/**
 * Uploads raw media file DIRECTLY to S3 / Cloudflare R2 / MinIO pre-signed URL.
 * Video bytes NEVER pass through Spring Boot, Render, Vercel, or the API Gateway.
 */
export function uploadFileDirectToStorage(
  uploadUrl: string,
  file: File,
  onProgress?: (percent: number) => void
): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('PUT', uploadUrl, true);
    xhr.setRequestHeader('Content-Type', file.type || 'video/mp4');

    if (xhr.upload && onProgress) {
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
          const percent = Math.round((e.loaded / e.total) * 100);
          onProgress(percent);
        }
      };
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve();
      } else {
        reject(new Error(`Direct storage upload failed with status ${xhr.status}`));
      }
    };

    xhr.onerror = () => {
      reject(new Error('Network error during direct storage upload'));
    };

    xhr.send(file);
  });
}

/**
 * Notifies the backend that direct S3 upload has finished, verifying the object
 * and initiating background transcoding.
 */
export async function completeUploadIntent(intentId: string): Promise<UploadIntentResponse> {
  const res = await fetch(`${API_BASE}/videos/upload-intents/${intentId}/complete`, {
    method: 'PUT',
    headers: {
      'X-User-Id': getStoredUserId(),
    },
  });

  if (!res.ok) {
    throw new Error(`Failed to complete upload intent: ${res.statusText}`);
  }

  return res.json();
}

/**
 * Fetches encoding jobs from the encoding service with optional status filter.
 */
export async function getEncodingJobs(status?: EncodingStatus): Promise<EncodingJob[]> {
  try {
    const query = status ? `?status=${status}` : '';
    const res = await fetch(`${API_BASE}/encoding/jobs${query}`);
    if (res.ok) {
      const data = await res.json();
      return data.content || data;
    }
  } catch (e) {
    console.warn('Could not fetch encoding jobs, returning empty array:', e);
  }
  return [];
}

/**
 * Requests retry of a failed or stuck encoding job.
 */
export async function retryEncodingJob(jobId: string): Promise<EncodingJob> {
  const res = await fetch(`${API_BASE}/encoding/jobs/${jobId}/retry`, {
    method: 'POST',
    headers: {
      'X-User-Id': getStoredUserId(),
    },
  });

  if (!res.ok) {
    throw new Error(`Failed to retry encoding job: ${res.statusText}`);
  }

  return res.json();
}

/**
 * Performs full-text search with optional type filtering and pagination.
 */
export async function searchTitles(query?: string, type?: string, page = 0, size = 20): Promise<SearchResultPage> {
  try {
    const params = new URLSearchParams();
    if (query?.trim()) params.append('q', query.trim());
    if (type?.trim()) params.append('type', type.trim());
    params.append('page', page.toString());
    params.append('size', size.toString());

    const res = await fetch(`${API_BASE}/search?${params.toString()}`);
    if (res.ok) {
      return await res.json();
    }
  } catch (e) {
    console.warn('Search service call failed, falling back to local dataset:', e);
  }

  // Graceful fallback over mock titles
  let filtered = MOCK_TITLES;
  if (type) {
    filtered = filtered.filter(t => t.type === type.toUpperCase());
  }
  if (query?.trim()) {
    const q = query.trim().toLowerCase();
    filtered = filtered.filter(t => 
      t.title.toLowerCase().includes(q) ||
      t.synopsis.toLowerCase().includes(q) ||
      t.tags?.some(tag => tag.toLowerCase().includes(q))
    );
  }

  const start = page * size;
  const paged = filtered.slice(start, start + size);
  return {
    content: paged.map(t => ({
      id: t.id,
      title: t.title,
      type: t.type,
      synopsis: t.synopsis,
      releaseYear: t.releaseYear,
      maturityRating: t.maturityRating,
      genres: t.tags || [],
      castMembers: [],
      directors: [],
      posterUrl: t.posterUrl,
    })),
    page,
    size,
    totalElements: filtered.length,
    totalPages: Math.max(1, Math.ceil(filtered.length / size)),
    hasNext: start + size < filtered.length,
  };
}

/**
 * Fetches instant prefix suggestions for the search auto-complete dropdown.
 */
export async function getSearchSuggestions(query: string, limit = 5): Promise<string[]> {
  if (!query?.trim()) return [];
  try {
    const res = await fetch(`${API_BASE}/search/suggestions?q=${encodeURIComponent(query.trim())}&limit=${limit}`);
    if (res.ok) {
      return await res.json();
    }
  } catch (e) {
    console.warn('Search suggestions failed, falling back to local titles:', e);
  }

  const q = query.trim().toLowerCase();
  return MOCK_TITLES
    .filter(t => t.title.toLowerCase().includes(q))
    .slice(0, limit)
    .map(t => t.title);
}

/**
 * Fetches dynamic trending rail from recommendation-service with fail-open fallback.
 */
export async function fetchTrendingRail(limit = 10): Promise<Title[]> {
  try {
    const res = await fetch(`${API_BASE}/recommendations/trending?limit=${limit}`);
    if (res.ok) {
      const data = await res.json();
      if (data.items && data.items.length > 0) {
        return data.items.map((item: any) => ({
          id: item.titleId,
          type: item.type || 'MOVIE',
          title: item.title,
          synopsis: item.reason || '',
          releaseYear: item.releaseYear || 2024,
          maturityRating: item.maturityRating || 'PG-13',
          durationMinutes: 120,
          posterUrl: item.posterUrl || MOCK_TITLES[0].posterUrl,
          backdropUrl: item.posterUrl || MOCK_TITLES[0].backdropUrl,
          publicationState: 'PUBLISHED',
          tags: item.genres || [],
        }));
      }
    }
  } catch (e) {
    console.warn('Trending rail fetch failed, falling back to mock dataset:', e);
  }
  return MOCK_TITLES.filter(t => t.type === 'MOVIE');
}

/**
 * Fetches personalized recommendations for the current user with fail-open fallback.
 */
export async function fetchPersonalRecommendations(limit = 10): Promise<Title[]> {
  try {
    const res = await fetch(`${API_BASE}/recommendations/personal?limit=${limit}`, {
      headers: {
        'X-User-Id': getStoredUserId(),
      },
    });
    if (res.ok) {
      const data = await res.json();
      if (data.items && data.items.length > 0) {
        return data.items.map((item: any) => ({
          id: item.titleId,
          type: item.type || 'MOVIE',
          title: item.title,
          synopsis: item.reason || '',
          releaseYear: item.releaseYear || 2024,
          maturityRating: item.maturityRating || 'PG-13',
          durationMinutes: 120,
          posterUrl: item.posterUrl || MOCK_TITLES[0].posterUrl,
          backdropUrl: item.posterUrl || MOCK_TITLES[0].backdropUrl,
          publicationState: 'PUBLISHED',
          tags: item.genres || [],
        }));
      }
    }
  } catch (e) {
    console.warn('Personal recommendations fetch failed, falling back to mock dataset:', e);
  }
  return MOCK_TITLES;
}


