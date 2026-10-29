'use client';

import React, { useCallback, useEffect, useState } from 'react';
import HeroBillboard from '@/components/HeroBillboard';
import ContentRail from '@/components/ContentRail';
import { EmptyState, ErrorState, Skeleton } from '@/components/common';
import { Film } from 'lucide-react';
import {
  ApiError,
  fetchHomeFeed,
  fetchContinueWatching,
  fetchTrendingRail,
  fetchPersonalRecommendations,
  fetchCollections,
  type CollectionRail,
} from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { SLOW_LOAD_MESSAGE, useSlowHint } from '@/lib/hooks';
import type { HomeFeed, ViewingProgress, Title } from '@/types';

/** Collection titles are lighter than full titles; fill the fields the card needs. */
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

export default function BrowsePage() {
  const [feed, setFeed] = useState<HomeFeed | null>(null);
  const [continueWatching, setContinueWatching] = useState<ViewingProgress[]>([]);
  const [trending, setTrending] = useState<Title[]>([]);
  const [recommended, setRecommended] = useState<Title[]>([]);
  const [collections, setCollections] = useState<CollectionRail[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { isAuthed, loading: authLoading } = useAuth();
  const slow = useSlowHint(loading);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);

    // Every request starts at once, so a cold backend costs one wait rather than one per section.
    // Each section appears as soon as its own response arrives; a slow rail never holds up the hero.
    const feedRequest = fetchHomeFeed();
    const continueRequest = isAuthed
      ? fetchContinueWatching().catch((e: unknown) => {
          if (!(e instanceof ApiError && e.isUnauthorized)) console.warn('Continue Watching unavailable:', e);
          return [] as ViewingProgress[];
        })
      : Promise.resolve([] as ViewingProgress[]);
    const trendingRequest = fetchTrendingRail(10).catch(() => [] as Title[]);
    const recommendedRequest = isAuthed
      ? fetchPersonalRecommendations(10).catch(() => [] as Title[])
      : Promise.resolve([] as Title[]);
    const collectionsRequest = fetchCollections().catch(() => [] as CollectionRail[]);

    continueRequest.then(setContinueWatching);
    trendingRequest.then(setTrending);
    recommendedRequest.then(setRecommended);
    collectionsRequest.then(setCollections);

    try {
      // The catalog feed is the one request the page cannot render without.
      setFeed(await feedRequest);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Couldn't load the catalog. Try again.");
    } finally {
      setLoading(false);
    }
  }, [isAuthed]);

  useEffect(() => {
    if (authLoading) return;
    loadData();
  }, [authLoading, loadData]);

  if (loading) {
    return (
      <div aria-busy="true">
        <div className="h-[min(72vh,720px)] min-h-[420px] w-full">
          <Skeleton className="h-full w-full rounded-none" />
        </div>
        <div className="mx-auto max-w-container px-4 py-10 sm:px-6 lg:px-10">
          <p className="text-sm text-muted-foreground">{slow ? SLOW_LOAD_MESSAGE : 'Loading the catalog…'}</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="mx-auto max-w-container px-4 py-24 sm:px-6 lg:px-10">
        <ErrorState message={error} onRetry={loadData} />
      </div>
    );
  }

  if (!feed?.featuredTitle) {
    return (
      <div className="mx-auto max-w-container px-4 py-24 sm:px-6 lg:px-10">
        <EmptyState icon={Film} title="Nothing to watch yet" description="Titles appear here once they are published." />
      </div>
    );
  }

  const featuredRotation = [
    feed.featuredTitle,
    ...trending.filter((t) => t.id !== feed.featuredTitle?.id).slice(0, 3),
  ];

  return (
    <div className="pb-8">
      <HeroBillboard titles={featuredRotation} />

      <div className="mt-2 space-y-2">
        {continueWatching.length > 0 && <ContentRail title="Continue watching" progressItems={continueWatching} />}
        {trending.length > 0 && <ContentRail title="Top 10 today" items={trending} isTop10 />}
        {recommended.length > 0 && <ContentRail title="Recommended for you" items={recommended} />}
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
