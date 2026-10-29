import type { CatalogRail, HomeFeed, Title } from '@/types';

/** The home feed as Content-Service sends it: the featured title is `featured`. */
export interface HomeFeedResponse {
  featured: Title | null;
  rails: CatalogRail[];
}

/** Maps the home feed to the web shape (`featuredTitle`). Shared by the browser and the server-side catalog read. */
export function toHomeFeed(data: HomeFeedResponse): HomeFeed {
  return { featuredTitle: data.featured ?? null, rails: data.rails ?? [] };
}

/** One item in a Recommendation-Service rail. The service knows less about a title than the catalog does. */
export interface RecommendationItem {
  titleId: string;
  type?: Title['type'];
  title: string;
  reason?: string;
  releaseYear?: number;
  maturityRating?: string;
  posterUrl?: string;
  genres?: string[];
}

/**
 * Maps a recommendation rail to titles. Shared by the browser client and the server-side catalog read,
 * so both build the same shape. Fields the rail does not carry stay empty rather than being invented.
 */
export function railToTitles(data: { items?: RecommendationItem[] }): Title[] {
  return (data.items ?? []).map((item) => ({
    id: item.titleId,
    type: item.type || 'MOVIE',
    title: item.title,
    synopsis: '',
    releaseYear: item.releaseYear ?? 0,
    maturityRating: item.maturityRating || '',
    posterUrl: item.posterUrl || '',
    bannerUrl: null,
    publicationState: 'PUBLISHED' as const,
    genres: (item.genres || []).map((name) => ({ id: name, name, slug: name })),
  }));
}
