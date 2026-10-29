import type { CollectionRail } from '@/lib/api';
import type { HomeFeed, Title } from '@/types';
import { railToTitles, type RecommendationItem } from '@/lib/mappers';

/**
 * Public catalog reads for server components. Responses are cached by Next for REVALIDATE_SECONDS, so
 * visitors are served from the Vercel cache and the backend is only asked once per window. The backend
 * (Render Free) may be asleep; the first refresh after a sleep can be slow, and visitors keep getting the
 * last good copy while it runs.
 *
 * Authenticated data (Continue Watching, personal recommendations, the watchlist) never goes through here.
 */
export const REVALIDATE_SECONDS = 300;

const GATEWAY = (process.env.GATEWAY_URL || process.env.NEXT_PUBLIC_GATEWAY_URL || '').replace(/\/+$/, '');

export interface BrowseCatalog {
  /** null means "not available on the server": the browser fetches that part instead. */
  feed: HomeFeed | null;
  trending: Title[] | null;
  collections: CollectionRail[] | null;
}

async function getPublic<T>(path: string): Promise<T> {
  if (!GATEWAY) throw new Error('Gateway URL is not configured');
  const res = await fetch(`${GATEWAY}/api/v1${path}`, {
    next: { revalidate: REVALIDATE_SECONDS },
    signal: AbortSignal.timeout(60_000),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status} for ${path}`);
  return (await res.json()) as T;
}

/** Never throws: a failed part comes back as null and the page fetches it in the browser. */
export async function getBrowseCatalog(): Promise<BrowseCatalog> {
  const [feed, trending, collections] = await Promise.all([
    getPublic<HomeFeed>('/catalog/home').catch(() => null),
    getPublic<{ items?: RecommendationItem[] }>('/recommendations/trending?limit=10')
      .then(railToTitles)
      .catch(() => null),
    getPublic<CollectionRail[]>('/catalog/collections').catch(() => null),
  ]);
  return { feed, trending, collections };
}
