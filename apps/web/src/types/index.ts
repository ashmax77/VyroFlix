export type TitleType = 'MOVIE' | 'SERIES';
export type PublicationState = 'DRAFT' | 'PUBLISHED' | 'UNPUBLISHED';

export interface Genre {
  id: string;
  name: string;
  slug: string;
}

export interface Title {
  id: string;
  type: TitleType;
  title: string;
  synopsis: string;
  releaseYear: number;
  maturityRating: string;
  durationMinutes: number;
  posterUrl: string;
  backdropUrl: string;
  publicationState: PublicationState;
  genres?: Genre[];
  tags?: string[];
  seasons?: Season[];
}

export interface Season {
  id: string;
  titleId: string;
  seasonNumber: number;
  title: string;
  episodes?: Episode[];
}

export interface Episode {
  id: string;
  seasonId: string;
  episodeNumber: number;
  title: string;
  synopsis: string;
  durationMinutes: number;
  thumbnailUrl?: string;
}

export interface CatalogRail {
  id: string;
  title: string;
  type: string;
  items: Title[];
}

export interface HomeFeed {
  featuredTitle: Title | null;
  rails: CatalogRail[];
}

export interface ViewingProgress {
  id: string;
  userId: string;
  contentId: string;
  targetType: 'MOVIE' | 'EPISODE';
  positionSeconds: number;
  durationSeconds: number;
  progressPercent: number;
  completed: boolean;
  lastWatchedAt: string;
  title?: Title;
}

export interface WatchlistEntry {
  id: string;
  userId: string;
  titleId: string;
  createdAt: string;
  title?: Title;
}

export interface PlaybackSession {
  sessionId: string;
  manifestUrl: string;
  expiresAt: string;
  resumePositionSeconds: number;
}
