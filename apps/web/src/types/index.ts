export type TitleType = 'MOVIE' | 'SERIES';
export type PublicationState = 'DRAFT' | 'PUBLISHED' | 'UNPUBLISHED';

export interface Genre {
  id: string;
  name: string;
  slug: string;
}

/**
 * Mirrors the Content-Service TitleResponse. Movie runtime is not part of the catalog model, so there is no duration here.
 * bannerUrl is the wide artwork (the API name; the UI calls it the backdrop).
 */
export interface Title {
  id: string;
  type: TitleType;
  title: string;
  synopsis: string;
  releaseYear: number;
  maturityRating: string;
  posterUrl: string;
  bannerUrl: string | null;
  publicationState: PublicationState;
  genres?: Genre[];
  seasons?: Season[];
  /** Present on single-title reads only. */
  cast?: CastMember[];
}

export interface CastMember {
  personId: string;
  name: string;
  avatarUrl?: string;
  role: 'ACTOR' | 'DIRECTOR' | 'WRITER' | 'PRODUCER';
  characterName?: string;
}

export interface Season {
  id: string;
  titleId: string;
  seasonNumber: number;
  name: string | null;
  overview: string | null;
  episodes?: Episode[];
}

export interface Episode {
  id: string;
  seasonId: string;
  episodeNumber: number;
  name: string;
  overview: string | null;
  /** Seconds, when the episode has been encoded. */
  durationSeconds: number | null;
  thumbnailUrl: string | null;
  publicationState: PublicationState;
  mediaReady: boolean;
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

export type UploadStatus = 'PENDING' | 'UPLOADED' | 'FAILED';
export type EncodingStatus = 'QUEUED' | 'PROCESSING' | 'READY' | 'FAILED';

export interface UploadIntentResponse {
  id: string;
  contentId: string;
  targetType: 'MOVIE' | 'EPISODE';
  filename: string;
  contentType: string;
  fileSizeBytes: number;
  s3Bucket: string;
  s3Key: string;
  status: UploadStatus;
  uploadUrl: string;
  createdAt: string;
  expiresAt: string;
}

export interface HlsRendition {
  id: string;
  resolution: string;
  bitrate: number;
  width: number;
  height: number;
  playlistKey: string;
}

export interface EncodingJob {
  id: string;
  uploadIntentId: string;
  contentId: string;
  rawS3Bucket: string;
  rawS3Key: string;
  outputS3Bucket: string;
  outputS3Prefix: string;
  masterPlaylistKey?: string;
  status: EncodingStatus;
  durationSeconds?: number;
  errorMessage?: string;
  retryCount: number;
  createdAt: string;
  updatedAt: string;
  renditions?: HlsRendition[];
}

export interface SearchCard {
  id: string;
  title: string;
  type: TitleType;
  synopsis: string;
  releaseYear: number;
  maturityRating: string;
  genres: string[];
  castMembers: string[];
  directors: string[];
  posterUrl: string;
}

export interface SearchResultPage {
  content: SearchCard[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

