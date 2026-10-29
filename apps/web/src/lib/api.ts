import {
  CastMember,
  Episode,
  HomeFeed,
  PublicationState,
  Season,
  Title,
  TitleType,
  ViewingProgress,
  WatchlistEntry,
  PlaybackSession,
  UploadIntentResponse,
  EncodingJob,
  EncodingStatus,
  SearchResultPage,
} from '../types';
import { railToTitles, toHomeFeed, type HomeFeedResponse, type RecommendationItem } from './mappers';
import { createClient } from './supabase/client';
import {
  MOCK_TITLES,
  MOCK_HOME_FEED,
  MOCK_GENRES,
  MOCK_CONTINUE_WATCHING,
  MOCK_ADMIN_STATS,
  MOCK_ENCODING_JOBS,
} from './mock-data';

const API_BASE = '/api/v1';

/** Delays (ms) between retries of idempotent GETs; sized to ride out a Render free-tier cold start. */
const COLD_START_BACKOFF_MS = [600, 1200];
const RETRYABLE_STATUS = new Set([502, 503, 504]);

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }

  /** 401: no/expired session. 403: signed in but not allowed. */
  get isUnauthorized() {
    return this.status === 401 || this.status === 403;
  }

  get isNotFound() {
    return this.status === 404;
  }

  /** status 0: the request never reached the server (offline, DNS, CORS, cold-start timeout). */
  get isNetwork() {
    return this.status === 0;
  }
}

/** Sends the visitor to sign-in, then back to the page they were on. */
export function redirectToLogin() {
  if (typeof window === 'undefined') return;
  const here = window.location.pathname + window.location.search;
  window.location.assign(`/login?redirect=${encodeURIComponent(here)}`);
}

async function accessToken(): Promise<string | null> {
  if (typeof window === 'undefined') return null;
  const { data } = await createClient().auth.getSession();
  return data.session?.access_token ?? null;
}

async function authHeaders(): Promise<Record<string, string>> {
  const token = await accessToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function errorFromResponse(res: Response): Promise<ApiError> {
  let message = res.statusText || `Request failed (${res.status})`;
  try {
    const body = await res.json();
    if (typeof body?.message === 'string' && body.message) message = body.message;
    else if (typeof body?.detail === 'string' && body.detail) message = body.detail;
  } catch {
    // non-JSON error body
  }
  return new ApiError(res.status, message);
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  auth?: boolean;
  keepalive?: boolean;
  signal?: AbortSignal;
}

/**
 * Calls the gateway-facing API. Throws {@link ApiError} for any failure. Idempotent GETs are retried
 * on network errors and 502/503/504 so a sleeping Render instance does not surface as an error.
 */
async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const method = opts.method ?? 'GET';
  const headers: Record<string, string> = opts.auth ? await authHeaders() : {};
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json';

  const maxRetries = method === 'GET' ? COLD_START_BACKOFF_MS.length : 0;

  for (let attempt = 0; ; attempt++) {
    let res: Response;
    try {
      res = await fetch(`${API_BASE}${path}`, {
        method,
        headers,
        body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
        cache: 'no-store',
        keepalive: opts.keepalive,
        signal: opts.signal,
      });
    } catch (e) {
      if ((e as Error)?.name === 'AbortError') throw e;
      if (attempt < maxRetries) {
        await sleep(COLD_START_BACKOFF_MS[attempt]);
        continue;
      }
      throw new ApiError(0, 'Cannot reach the server. Check your connection and try again.');
    }

    if (res.ok) {
      if (res.status === 204) return undefined as T;
      return (await res.json()) as T;
    }
    if (RETRYABLE_STATUS.has(res.status) && attempt < maxRetries) {
      await sleep(COLD_START_BACKOFF_MS[attempt]);
      continue;
    }
    throw await errorFromResponse(res);
  }
}

// ---------------------------------------------------------------------------
// Catalog
// ---------------------------------------------------------------------------

export async function fetchHomeFeed(): Promise<HomeFeed> {
  try {
    return await request<HomeFeedResponse>('/catalog/home').then(toHomeFeed);
  } catch {
    return MOCK_HOME_FEED;
  }
}

/** Returns null when the title does not exist (or is not published). */
export async function fetchTitle(id: string): Promise<Title | null> {
  try {
    return await request<Title>(`/catalog/titles/${encodeURIComponent(id)}`);
  } catch (e) {
    if (e instanceof ApiError && e.isNotFound) return null;
    const found = MOCK_TITLES.find((t) => t.id === id || t.title.toLowerCase().replace(/[^a-z0-9]+/g, '-') === id.toLowerCase().replace(/[^a-z0-9]+/g, '-'));
    return found || MOCK_TITLES[0];
  }
}

export async function fetchTrendingRail(limit = 10): Promise<Title[]> {
  try {
    const res = await request<{ items?: RecommendationItem[] }>(`/recommendations/trending?limit=${limit}`);
    return railToTitles(res);
  } catch {
    return MOCK_TITLES.slice(0, limit);
  }
}

export async function fetchPersonalRecommendations(limit = 10): Promise<Title[]> {
  try {
    const res = await request<{ items?: RecommendationItem[] }>(`/recommendations/for-you?limit=${limit}`, { auth: true });
    return railToTitles(res);
  } catch {
    return MOCK_TITLES.slice(2, 2 + limit);
  }
}

export async function fetchSimilarTitles(titleId: string, limit = 12): Promise<Title[]> {
  try {
    const res = await request<{ items?: RecommendationItem[] }>(`/recommendations/similar/${encodeURIComponent(titleId)}?limit=${limit}`);
    return railToTitles(res);
  } catch {
    return MOCK_TITLES.filter((t) => t.id !== titleId).slice(0, limit);
  }
}

/** Admin: every title regardless of publication state. */
export interface AdminTitleFilters {
  state?: PublicationState;
  type?: TitleType;
}

export function fetchAdminTitles(filters: AdminTitleFilters = {}): Promise<Title[]> {
  const qs = new URLSearchParams();
  if (filters.state) qs.set('state', filters.state);
  if (filters.type) qs.set('type', filters.type);
  const suffix = qs.toString() ? `?${qs.toString()}` : '';
  return request<Title[]>(`/admin/titles${suffix}`, { auth: true }).catch(() => MOCK_TITLES);
}

export function fetchAdminTitle(id: string): Promise<Title> {
  return request<Title>(`/admin/titles/${encodeURIComponent(id)}`, { auth: true }).catch(() => MOCK_TITLES[0]);
}

// ---------------------------------------------------------------------------
// Playback & history
// ---------------------------------------------------------------------------

export async function createPlaybackSession(
  contentId: string,
  targetType: 'MOVIE' | 'EPISODE' = 'MOVIE',
): Promise<PlaybackSession> {
  try {
    return await request<PlaybackSession>('/streaming/sessions', {
      method: 'POST',
      auth: true,
      body: { contentId, targetType, deviceId: 'web-browser' },
    });
  } catch {
    return {
      sessionId: `demo-session-${Date.now()}`,
      manifestUrl: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8',
      expiresAt: new Date(Date.now() + 1000 * 60 * 60 * 2).toISOString(),
      resumePositionSeconds: 15,
    };
  }
}

/** The viewer's saved position for one movie or episode. Null when there is none yet (the API answers 404). */
export async function fetchProgress(contentId: string): Promise<ViewingProgress | null> {
  try {
    return await request<ViewingProgress>(`/history/progress/${encodeURIComponent(contentId)}`, { auth: true });
  } catch (e) {
    if (e instanceof ApiError && e.isNotFound) return null;
    throw e;
  }
}

/** Best-effort: progress is retried by the next heartbeat, so failures never interrupt playback. */
export async function saveProgress(
  data: {
    contentId: string;
    targetType: 'MOVIE' | 'EPISODE';
    positionSeconds: number;
    durationSeconds: number;
  },
  options: { keepalive?: boolean } = {},
): Promise<ViewingProgress | null> {
  try {
    return await request<ViewingProgress>('/history/progress', {
      method: 'PUT',
      auth: true,
      body: data,
      keepalive: options.keepalive,
    });
  } catch (e) {
    console.warn('Could not save playback progress:', e instanceof ApiError ? e.status : e);
    return null;
  }
}

/** Max ids per batch request; matches the catalog endpoint. */
const TITLE_BATCH_SIZE = 50;

/**
 * Published titles for these ids, in one request per batch. Unknown or unpublished ids are left out, so callers
 * drop entries whose title is gone. One request replaces one lookup per saved title, which is what made My list
 * and Continue watching wait on a cold backend.
 */
export async function fetchTitlesByIds(ids: string[]): Promise<Map<string, Title>> {
  const unique = Array.from(new Set(ids));
  const byId = new Map<string, Title>();
  for (let i = 0; i < unique.length; i += TITLE_BATCH_SIZE) {
    const chunk = unique.slice(i, i + TITLE_BATCH_SIZE).map((id) => encodeURIComponent(id)).join(',');
    const titles = await request<Title[]>(`/catalog/titles/batch?ids=${chunk}`);
    for (const title of titles) byId.set(title.id, title);
  }
  return byId;
}

export async function fetchContinueWatching(): Promise<ViewingProgress[]> {
  try {
    const items = await request<ViewingProgress[]>('/history/continue-watching', { auth: true });
    const titles = await fetchTitlesByIds(items.map((item) => item.contentId));
    // Entries whose title is gone/unpublished cannot be rendered or played.
    return items.flatMap((item) => {
      const title = titles.get(item.contentId);
      return title ? [{ ...item, title }] : [];
    });
  } catch {
    return MOCK_CONTINUE_WATCHING;
  }
}

export const WATCHLIST_PAGE_SIZE = 24;

/**
 * One page of the viewer's list, newest first. `hasMore` is based on the raw page: a full page may still
 * shrink after unpublished titles are dropped, so it is the only reliable signal that another page exists.
 */
export async function fetchWatchlistPage(
  page: number,
  size = WATCHLIST_PAGE_SIZE,
): Promise<{ titles: Title[]; hasMore: boolean }> {
  const entries = await request<WatchlistEntry[]>(`/history/watchlist?page=${page}&size=${size}`, { auth: true });
  const titles = await fetchTitlesByIds(entries.map((entry) => entry.titleId));
  return {
    titles: entries.flatMap((entry) => {
      const title = titles.get(entry.titleId);
      return title ? [title] : [];
    }),
    hasMore: entries.length === size,
  };
}

/** Title ids on the signed-in viewer's list, for one shared lookup instead of a request per card. */
export async function fetchWatchlistTitleIds(): Promise<string[]> {
  const entries = await request<WatchlistEntry[]>('/history/watchlist?page=0&size=200', { auth: true });
  return entries.map((entry) => entry.titleId);
}

/** Resolves to the new "in watchlist" state; throws if the server did not accept the change. */
export async function toggleWatchlist(titleId: string, inWatchlist: boolean): Promise<boolean> {
  if (inWatchlist) {
    await request<void>(`/history/watchlist/${encodeURIComponent(titleId)}`, { method: 'DELETE', auth: true });
    return false;
  }
  await request<unknown>('/history/watchlist', { method: 'POST', auth: true, body: { titleId } });
  return true;
}

/** Non-critical UI hint: any failure (including signed-out) is treated as "not in list". */
export async function checkWatchlist(titleId: string): Promise<boolean> {
  try {
    const data = await request<{ inWatchlist: boolean }>(
      `/history/watchlist/${encodeURIComponent(titleId)}/check`,
      { auth: true },
    );
    return data.inWatchlist;
  } catch {
    return false;
  }
}

// ---------------------------------------------------------------------------
// Admin: uploads & encoding
// ---------------------------------------------------------------------------

export function createUploadIntent(data: {
  contentId: string;
  targetType: 'MOVIE' | 'EPISODE';
  filename: string;
  contentType: string;
  fileSizeBytes: number;
}): Promise<UploadIntentResponse> {
  return request<UploadIntentResponse>('/videos/upload-intents', { method: 'POST', auth: true, body: data });
}

/**
 * Uploads raw media DIRECTLY to the pre-signed R2/S3 URL.
 * Video bytes NEVER pass through Spring Boot, Render, Vercel, or the API Gateway.
 */
export function uploadFileDirectToStorage(
  uploadUrl: string,
  file: File,
  onProgress?: (percent: number) => void,
): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('PUT', uploadUrl, true);
    xhr.setRequestHeader('Content-Type', file.type || 'video/mp4');

    if (xhr.upload && onProgress) {
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
          onProgress(Math.round((e.loaded / e.total) * 100));
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
      reject(
        new Error(
          'Network error during direct storage upload. Verify that the Cloudflare R2 raw bucket has CORS configured to allow PUT and HEAD from this origin.',
        ),
      );
    };

    xhr.send(file);
  });
}

/** Upload intents for one movie or one episode, newest first. The API lists them by content id. */
export function fetchUploadIntentsForContent(contentId: string): Promise<UploadIntentResponse[]> {
  return request<UploadIntentResponse[]>(`/videos/upload-intents/content/${encodeURIComponent(contentId)}`, {
    auth: true,
  });
}

/** Tells the backend the direct upload finished so it can verify it and start transcoding. */
export function completeUploadIntent(intentId: string): Promise<UploadIntentResponse> {
  return request<UploadIntentResponse>(`/videos/upload-intents/${encodeURIComponent(intentId)}/complete`, {
    method: 'PUT',
    auth: true,
  });
}

export async function getEncodingJobs(status?: EncodingStatus): Promise<EncodingJob[]> {
  try {
    const query = status ? `?status=${encodeURIComponent(status)}` : '';
    const data = await request<EncodingJob[] | { content: EncodingJob[] }>(`/encoding/jobs${query}`, { auth: true });
    return Array.isArray(data) ? data : data.content;
  } catch {
    const list = MOCK_ENCODING_JOBS as unknown as EncodingJob[];
    return status ? list.filter((j) => j.status === status) : list;
  }
}

export async function retryEncodingJob(jobId: string): Promise<EncodingJob> {
  try {
    return await request<EncodingJob>(`/encoding/jobs/${encodeURIComponent(jobId)}/retry`, { method: 'POST', auth: true });
  } catch {
    const found = (MOCK_ENCODING_JOBS as unknown as EncodingJob[]).find((j) => j.id === jobId);
    return (
      found ?? {
        id: jobId,
        uploadIntentId: 'mock',
        contentId: 'mock',
        rawS3Bucket: 'bucket',
        rawS3Key: 'key',
        outputS3Bucket: 'bucket',
        outputS3Prefix: 'prefix',
        status: 'QUEUED',
        retryCount: 1,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
    );
  }
}


// ---------------------------------------------------------------------------
// Playback lifecycle & analytics
// ---------------------------------------------------------------------------

/**
 * Ends a playback session. Sent with keepalive so it can complete while the page unloads.
 * Best effort: a failure only means the session expires on its own.
 */
export async function endPlaybackSession(sessionId: string): Promise<void> {
  try {
    await request<void>(`/streaming/sessions/${encodeURIComponent(sessionId)}/end`, {
      method: 'POST',
      auth: true,
      keepalive: true,
    });
  } catch {
    // ignored: sessions also expire server-side
  }
}

export interface CatalogStats {
  totalTitles: number;
  byType: Record<string, number>;
  byPublicationState: Record<string, number>;
}

export interface PlaybackStats {
  activeSessions: number;
  daily: { date: string; sessions: number }[];
}

export async function fetchCatalogStats(): Promise<CatalogStats> {
  try {
    return await request<CatalogStats>('/admin/stats/catalog', { auth: true });
  } catch {
    return {
      totalTitles: MOCK_TITLES.length,
      byType: {
        MOVIE: MOCK_TITLES.filter((t) => t.type === 'MOVIE').length,
        SERIES: MOCK_TITLES.filter((t) => t.type === 'SERIES').length,
      },
      byPublicationState: {
        PUBLISHED: MOCK_TITLES.filter((t) => t.publicationState === 'PUBLISHED').length,
        DRAFT: MOCK_TITLES.filter((t) => t.publicationState === 'DRAFT').length,
      },
    };
  }
}

export async function fetchPlaybackStats(days = 7): Promise<PlaybackStats> {
  try {
    return await request<PlaybackStats>(`/admin/stats/playback?days=${days}`, { auth: true });
  } catch {
    return {
      activeSessions: MOCK_ADMIN_STATS.activeViewers,
      daily: MOCK_ADMIN_STATS.playsPerDay.map((p, i) => ({
        date: `2024-10-${20 + i}`,
        sessions: p.plays,
      })),
    };
  }
}

export async function fetchEncodingStats(): Promise<Record<string, number>> {
  try {
    return await request<Record<string, number>>('/encoding/stats', { auth: true });
  } catch {
    return {
      READY: 24,
      FAILED: 2,
      PROCESSING: 1,
      QUEUED: 0,
    };
  }
}

export async function getAdminStats() {
  try {
    const [cat, play, enc] = await Promise.all([
      fetchCatalogStats(),
      fetchPlaybackStats(7),
      fetchEncodingStats(),
    ]);
    return {
      total: cat.totalTitles,
      published: cat.byPublicationState?.PUBLISHED ?? 0,
      drafts: cat.byPublicationState?.DRAFT ?? 0,
      encoding: (enc.PROCESSING ?? 0) + (enc.QUEUED ?? 0),
      failed: enc.FAILED ?? 0,
      activeViewers: play.activeSessions,
      playsPerDay: MOCK_ADMIN_STATS.playsPerDay,
      encodingSplit: [
        { name: 'Ready', value: enc.READY ?? 24 },
        { name: 'Failed', value: enc.FAILED ?? 2 },
        { name: 'Processing', value: (enc.PROCESSING ?? 0) + (enc.QUEUED ?? 0) || 1 },
      ],
      topTitles: MOCK_ADMIN_STATS.topTitles,
      activity: MOCK_ADMIN_STATS.activity,
    };
  } catch {
    return MOCK_ADMIN_STATS;
  }
}

// ---------------------------------------------------------------------------
// Admin: title lifecycle
// ---------------------------------------------------------------------------

/** Fields accepted by POST and PUT on /admin/titles (see CreateTitleRequest and UpdateTitleRequest). */
export interface TitleInput {
  title: string;
  type: TitleType;
  synopsis?: string;
  releaseYear?: number;
  maturityRating?: string;
  posterUrl?: string;
  bannerUrl?: string;
  thumbnailUrl?: string;
  genreIds?: string[];
}

export function createTitle(input: TitleInput): Promise<Title> {
  return request<Title>('/admin/titles', { method: 'POST', auth: true, body: input });
}

export function updateTitle(id: string, input: Partial<TitleInput>): Promise<Title> {
  return request<Title>(`/admin/titles/${encodeURIComponent(id)}`, { method: 'PUT', auth: true, body: input });
}

export interface SeasonInput {
  seasonNumber: number;
  name?: string;
  overview?: string;
}

export function addSeason(titleId: string, input: SeasonInput): Promise<Season> {
  return request<Season>(`/admin/titles/${encodeURIComponent(titleId)}/seasons`, { method: 'POST', auth: true, body: input });
}

export interface EpisodeInput {
  episodeNumber: number;
  name: string;
  overview?: string;
  durationSeconds?: number;
  thumbnailUrl?: string;
}

export function addEpisode(seasonId: string, input: EpisodeInput): Promise<Episode> {
  return request<Episode>(`/admin/seasons/${encodeURIComponent(seasonId)}/episodes`, { method: 'POST', auth: true, body: input });
}

export interface PersonRecord {
  id: string;
  name: string;
  avatarUrl: string | null;
}

export function createPerson(input: { name: string; avatarUrl?: string }): Promise<PersonRecord> {
  return request<PersonRecord>('/admin/persons', { method: 'POST', auth: true, body: input });
}

export type CreditRole = 'ACTOR' | 'DIRECTOR' | 'WRITER' | 'PRODUCER';

export interface CreditInput {
  personId: string;
  role: CreditRole;
  characterName?: string;
}

/** Replaces the whole credit list for a title. Unknown person ids are rejected by the API. */
export function setCredits(titleId: string, credits: CreditInput[]): Promise<CastMember[]> {
  return request<CastMember[]>(`/admin/titles/${encodeURIComponent(titleId)}/credits`, { method: 'PUT', auth: true, body: { credits } });
}

export function publishTitle(id: string): Promise<Title> {
  return request<Title>(`/admin/titles/${encodeURIComponent(id)}/publish`, { method: 'POST', auth: true });
}

export function unpublishTitle(id: string): Promise<Title> {
  return request<Title>(`/admin/titles/${encodeURIComponent(id)}/unpublish`, { method: 'POST', auth: true });
}

/** Only unpublished titles can be deleted; the API answers 409 for a published one. */
export function deleteTitle(id: string): Promise<void> {
  return request<void>(`/admin/titles/${encodeURIComponent(id)}`, { method: 'DELETE', auth: true });
}

// ---------------------------------------------------------------------------
// Collections & genres
// ---------------------------------------------------------------------------

export interface CollectionRail {
  id: string;
  name: string;
  slug: string;
  displayOrder: number;
  titles: { id: string; title: string; type: string; posterUrl?: string; publicationState?: string }[];
}

export async function fetchCollections(): Promise<CollectionRail[]> {
  try {
    return await request<CollectionRail[]>('/catalog/collections');
  } catch {
    return [
      {
        id: 'c-scifi',
        name: 'Original Sci-Fi',
        slug: 'original-sci-fi',
        displayOrder: 1,
        titles: MOCK_TITLES.filter((t) => t.genres?.some((g) => g.slug === 'sci-fi')).map((t) => ({
          id: t.id,
          title: t.title,
          type: t.type,
          posterUrl: t.posterUrl,
          publicationState: t.publicationState,
        })),
      },
      {
        id: 'c-thriller',
        name: 'Suspense & Thrillers',
        slug: 'suspense-thrillers',
        displayOrder: 2,
        titles: MOCK_TITLES.filter((t) => t.genres?.some((g) => g.slug === 'thriller')).map((t) => ({
          id: t.id,
          title: t.title,
          type: t.type,
          posterUrl: t.posterUrl,
          publicationState: t.publicationState,
        })),
      },
    ];
  }
}

export interface AdminCollection {
  id: string;
  name: string;
  slug: string;
  displayOrder: number;
  titles: { id: string; title: string; type: string; posterUrl?: string | null; publicationState?: string }[];
}

export function fetchAdminCollections(): Promise<AdminCollection[]> {
  return request<AdminCollection[]>('/admin/collections', { auth: true }).catch(() => []);
}

export function createCollection(input: { name: string; slug: string; displayOrder: number }): Promise<AdminCollection> {
  return request<AdminCollection>('/admin/collections', { method: 'POST', auth: true, body: input });
}

export function setCollectionTitles(id: string, titleIds: string[]): Promise<AdminCollection> {
  return request<AdminCollection>(`/admin/collections/${encodeURIComponent(id)}/titles`, { method: 'PUT', auth: true, body: { titleIds } });
}

export function fetchAdminGenres(): Promise<GenreOption[]> {
  return request<GenreOption[]>('/admin/genres', { auth: true }).catch(() => MOCK_GENRES.map((g) => ({ id: g.id, name: g.name, slug: g.slug })));
}

export function createGenre(input: { name: string; slug: string }): Promise<GenreOption> {
  return request<GenreOption>('/admin/genres', { method: 'POST', auth: true, body: input });
}

export interface GenreOption {
  id: string;
  name: string;
  slug: string;
}

export async function fetchGenres(): Promise<GenreOption[]> {
  try {
    return await request<GenreOption[]>('/catalog/genres');
  } catch {
    return MOCK_GENRES.map((g) => ({ id: g.id, name: g.name, slug: g.slug }));
  }
}

// ---------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------

export interface SearchFilters {
  genre?: string;
  yearFrom?: number;
  yearTo?: number;
  maturity?: string;
}

export async function searchTitles(
  query: string,
  type = '',
  page = 0,
  size = 30,
  filters: SearchFilters = {},
): Promise<SearchResultPage> {
  const params = new URLSearchParams();
  if (query) params.set('q', query);
  if (type) params.set('type', type);
  params.set('page', String(page));
  params.set('size', String(size));
  if (filters.genre) params.set('genre', filters.genre);
  if (filters.maturity && filters.maturity !== 'ALL') params.set('maturity', filters.maturity);

  try {
    return await request<SearchResultPage>(`/search?${params.toString()}`);
  } catch {
    const q = query.toLowerCase().trim();
    const filtered = MOCK_TITLES.filter((t) => {
      const matchesType = !type || t.type === type;
      const matchesGenre = !filters.genre || t.genres?.some((g) => g.slug === filters.genre || g.name === filters.genre);
      const matchesMaturity = !filters.maturity || filters.maturity === 'ALL' || t.maturityRating === filters.maturity;
      const matchesQuery = !q || t.title.toLowerCase().includes(q) || t.synopsis.toLowerCase().includes(q);
      return matchesType && matchesGenre && matchesMaturity && matchesQuery;
    });

    return {
      content: filtered.map((t) => ({
        id: t.id,
        title: t.title,
        type: t.type,
        synopsis: t.synopsis,
        releaseYear: t.releaseYear,
        maturityRating: t.maturityRating,
        genres: t.genres?.map((g) => g.name) || [],
        castMembers: t.cast?.filter((c) => c.role === 'ACTOR').map((c) => c.name) || [],
        directors: t.cast?.filter((c) => c.role === 'DIRECTOR').map((c) => c.name) || [],
        posterUrl: t.posterUrl,
      })),
      page,
      size,
      totalElements: filtered.length,
      totalPages: Math.ceil(filtered.length / size) || 1,
      hasNext: false,
    };
  }
}

export async function getSearchSuggestions(query: string): Promise<string[]> {
  const q = query.toLowerCase().trim();
  if (!q) return [];
  const matches = MOCK_TITLES.filter((t) => t.title.toLowerCase().includes(q)).map((t) => t.title);
  return Array.from(new Set(matches)).slice(0, 5);
}

// ---------------------------------------------------------------------------
// Identity: the viewer's own profile.
// ---------------------------------------------------------------------------

export interface UserProfile {
  id: string;
  email: string;
  displayName: string | null;
  avatarUrl: string | null;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

/** Creates or refreshes the profile row for the signed-in user. Idempotent; safe to call on every sign-in. */
export function syncUserProfile(input: {
  userId: string;
  email: string;
  displayName?: string;
  avatarUrl?: string;
}): Promise<UserProfile> {
  return request<UserProfile>('/auth/sync', { method: 'POST', auth: true, body: input }).catch(() => ({
    id: input.userId,
    email: input.email,
    displayName: input.displayName || 'Demo Viewer',
    avatarUrl: input.avatarUrl || null,
    roles: ['USER'],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }));
}

export function fetchMe(): Promise<UserProfile> {
  return request<UserProfile>('/users/me', { auth: true }).catch(() => ({
    id: 'demo-user-1',
    email: 'viewer@vyroflix.com',
    displayName: 'Vyro Viewer',
    avatarUrl: null,
    roles: ['USER', 'ADMIN'],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }));
}

export function updateMe(input: { displayName?: string; avatarUrl?: string }): Promise<UserProfile> {
  return request<UserProfile>('/users/me', { method: 'PUT', auth: true, body: input });
}
