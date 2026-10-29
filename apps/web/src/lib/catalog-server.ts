import { PHASE_PRODUCTION_BUILD } from 'next/constants';
import type { CollectionRail } from '@/lib/api';
import type { HomeFeed, Title } from '@/types';
import { railToTitles, toHomeFeed, type HomeFeedResponse, type RecommendationItem } from '@/lib/mappers';
import { MOCK_HOME_FEED, MOCK_TITLES } from './mock-data';

export const REVALIDATE_SECONDS = 300;

const GATEWAY = (process.env.GATEWAY_URL || process.env.NEXT_PUBLIC_GATEWAY_URL || 'http://localhost:8080').replace(/\/+$/, '');

export interface BrowseCatalog {
  feed: HomeFeed | null;
  trending: Title[] | null;
  collections: CollectionRail[] | null;
}

const FALLBACK_COLLECTIONS: CollectionRail[] = [
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

async function getPublic<T>(path: string): Promise<T> {
  if (!GATEWAY) throw new Error('Gateway URL is not configured');
  const res = await fetch(`${GATEWAY}/api/v1${path}`, {
    next: { revalidate: REVALIDATE_SECONDS },
    signal: AbortSignal.timeout(3000),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status} for ${path}`);
  return (await res.json()) as T;
}

export async function getBrowseCatalog(): Promise<BrowseCatalog> {
  if (process.env.NEXT_PHASE === PHASE_PRODUCTION_BUILD) {
    return { feed: MOCK_HOME_FEED, trending: MOCK_TITLES.slice(0, 10), collections: FALLBACK_COLLECTIONS };
  }
  const [feed, trending, collections] = await Promise.all([
    getPublic<HomeFeedResponse>('/catalog/home')
      .then(toHomeFeed)
      .catch(() => MOCK_HOME_FEED),
    getPublic<{ items?: RecommendationItem[] }>('/recommendations/trending?limit=10')
      .then(railToTitles)
      .catch(() => MOCK_TITLES.slice(0, 10)),
    getPublic<CollectionRail[]>('/catalog/collections')
      .catch(() => FALLBACK_COLLECTIONS),
  ]);
  return { feed, trending, collections };
}
