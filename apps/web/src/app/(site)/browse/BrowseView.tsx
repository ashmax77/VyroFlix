'use client';

import React, { useCallback, useEffect, useState } from 'react';
import HeroBillboard from '@/components/HeroBillboard';
import ContentRail from '@/components/ContentRail';
import { EmptyState, ErrorState, Skeleton } from '@/components/common';
import { Film } from 'lucide-react';
import {
  ApiError,
  fetchCollections,
  fetchContinueWatching,
  fetchHomeFeed,
  fetchPersonalRecommendations,
  fetchTrendingRail,
  type CollectionRail,
} from '@/lib/api';
import type { BrowseCatalog } from '@/lib/catalog-server';
import { useAuth } from '@/lib/auth-context';
import { SLOW_LOAD_MESSAGE, useSlowHint } from '@/lib/hooks';
import type { HomeFeed, Title, ViewingProgress } from '@/types';

function asCardTitle(item: CollectionRail['titles'][number]): Title {
  return {
    id: item.id,
    type: item.type === 'SERIES' ? 'SERIES' : 'MOVIE',
    title: item.title,
    synopsis: '',
    releaseYear: 0,
    maturityRating: '',
    posterUrl: item.posterUrl ?? '',
    bannerUrl: null,
    publicationState: 'PUBLISHED',
  };
}

export default function BrowseView({ initial }: { initial: BrowseCatalog }) {
  const [feed, setFeed] = useState<HomeFeed | null>(initial.feed);
  const [trending, setTrending] = useState<Title[]>(initial.trending ?? []);
  const [collections, setCollections] = useState<CollectionRail[]>(initial.collections ?? []);
  const [continueWatching, setContinueWatching] = useState<ViewingProgress[]>([]);
  const [recommended, setRecommended] = useState<Title[]>([]);
  const [loading, setLoading] = useState(initial.feed === null);
  const [error, setError] = useState<string | null>(null);
  const { isAuthed, loading: authLoading } = useAuth();
  const slow = useSlowHint(loading);

  const loadMissing = useCallback(async () => {
    setError(null);
    const feedRequest = initial.feed === null ? fetchHomeFeed() : null;
    if (initial.trending === null) fetchTrendingRail(10).then(setTrending);
    if (initial.collections === null) fetchCollections().then(setCollections).catch(() => setCollections([]));

    if (isAuthed) {
      fetchContinueWatching()
        .then(setContinueWatching)
        .catch(() => setContinueWatching([]));
      fetchPersonalRecommendations(10).then(setRecommended).catch(() => setRecommended([]));
    } else {
      setContinueWatching([]);
      setRecommended([]);
    }

    if (feedRequest) {
      try {
        setFeed(await feedRequest);
      } catch (e) {
        setError(e instanceof ApiError ? e.message : "Couldn't load the catalog. Try again.");
      } finally {
        setLoading(false);
      }
    } else {
      setLoading(false);
    }
  }, [isAuthed, initial.feed, initial.trending, initial.collections]);

  useEffect(() => {
    if (authLoading) return;
    loadMissing();
  }, [authLoading, loadMissing]);

  if (loading && !feed) {
    return (
      <div aria-busy="true">
        <div className="h-[min(72vh,720px)] min-h-[420px] w-full">
          <Skeleton className="h-full w-full rounded-none" />
        </div>
        <div className="mx-auto max-w-[1800px] px-4 py-10 sm:px-8 lg:px-12">
          <p className="text-sm text-muted-foreground">{slow ? SLOW_LOAD_MESSAGE : 'Loading the catalog…'}</p>
        </div>
      </div>
    );
  }

  if (error && !feed) {
    return (
      <div className="mx-auto max-w-[1800px] px-4 py-24 sm:px-8 lg:px-12">
        <ErrorState message={error} onRetry={loadMissing} />
      </div>
    );
  }

  if (!feed?.featuredTitle) {
    return (
      <div className="mx-auto max-w-[1800px] px-4 py-24 sm:px-8 lg:px-12">
        <EmptyState icon={Film} title="Nothing to watch yet" description="Titles appear here once they are published." />
      </div>
    );
  }

  const featuredRotation = [
    feed.featuredTitle,
    ...trending.filter((t) => t.id !== feed.featuredTitle?.id).slice(0, 3),
  ];

  return (
    <div className="pb-16 md:pb-8">
      <HeroBillboard titles={featuredRotation} />

      <div className="relative z-20 -mt-6 sm:-mt-12 lg:-mt-16 space-y-4 sm:space-y-6">
        {continueWatching.length > 0 && (
          <ContentRail title="Continue Watching" progressItems={continueWatching} />
        )}
        {trending.length > 0 && (
          <ContentRail title="Trending Now" items={trending} isTop10 />
        )}
        {recommended.length > 0 && (
          <ContentRail title="Recommended for You" items={recommended} />
        )}
        {collections.map((collection) => (
          <ContentRail key={collection.id} title={collection.name} items={collection.titles.map(asCardTitle)} />
        ))}
        {feed.rails.map((rail) => (
          <ContentRail key={rail.id} title={rail.title} items={rail.items} />
        ))}
      </div>
    </div>
  );
}
