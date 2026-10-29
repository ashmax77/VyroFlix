'use client';

import React, { useEffect, useState } from 'react';
import HeroBillboard from '@/components/HeroBillboard';
import ContentRail from '@/components/ContentRail';
import { ErrorState, Spinner } from '@/components/common';
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
    durationMinutes: 0,
    posterUrl: item.posterUrl ?? '',
    backdropUrl: item.posterUrl ?? '',
    publicationState: 'PUBLISHED',
    tags: [],
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

  const loadData = async () => {
    setError(null);
    try {
      // The catalog is required; everything else is optional and must never block the page.
      const feedData = await fetchHomeFeed();
      setFeed(feedData);

      const [progressData, trendingData, recData] = await Promise.all([
        isAuthed
          ? fetchContinueWatching().catch((e: unknown) => {
              if (!(e instanceof ApiError && e.isUnauthorized)) console.warn('Continue Watching unavailable:', e);
              return [] as ViewingProgress[];
            })
          : Promise.resolve([] as ViewingProgress[]),
        fetchTrendingRail(10),
        isAuthed ? fetchPersonalRecommendations(10) : Promise.resolve([] as Title[]),
      ]);
      setContinueWatching(progressData);
      setTrending(trendingData);
      setRecommended(recData);
      // Collections are optional: a failure here must not hide the rest of the page.
      setCollections(await fetchCollections().catch(() => []));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Could not load the catalog.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (authLoading) return;
    setLoading(true);
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authLoading, isAuthed]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Spinner label={slow ? SLOW_LOAD_MESSAGE : 'Loading catalog & recommendations...'} />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <ErrorState
          message={error}
          onRetry={() => {
            setLoading(true);
            loadData();
          }}
        />
      </div>
    );
  }

  if (!feed || !feed.featuredTitle) {
    return (
      <div className="min-h-screen flex items-center justify-center text-muted-foreground">
        No titles available in catalog.
      </div>
    );
  }

  const featuredRotation = [
    feed.featuredTitle,
    ...trending.filter((t) => t.id !== feed.featuredTitle?.id).slice(0, 3),
  ];

  return (
    <div className="min-h-screen pb-20">
      {/* Cinematic Hero Billboard with Auto-advancing Rotation */}
      <HeroBillboard titles={featuredRotation} />

      {/* Content Rails Section with Top10 Styling & Hover Mini-Panels */}
      <div className="relative -mt-16 sm:-mt-24 z-20 space-y-6">
        {/* Continue Watching Rail */}
        {continueWatching.length > 0 && (
          <ContentRail
            title="Continue Watching"
            progressItems={continueWatching}
            onWatchlistChanged={loadData}
          />
        )}

        {/* Dynamic Trending Rail with iconic Top 10 Outline Numbering */}
        {trending.length > 0 && (
          <ContentRail
            title="Top 10 Trending Today"
            items={trending}
            isTop10={true}
            onWatchlistChanged={loadData}
          />
        )}

        {/* Personalized Recommendations Rail */}
        {recommended.length > 0 && (
          <ContentRail
            title="Recommended For You"
            items={recommended}
            onWatchlistChanged={loadData}
          />
        )}

        {/* Curated collections from the catalog */}
        {collections.map((collection) => (
          <ContentRail
            key={collection.id}
            title={collection.name}
            items={collection.titles.map(asCardTitle)}
            onWatchlistChanged={loadData}
          />
        ))}

        {/* Dynamic Aggregated Catalog Rails */}
        {feed.rails.map((rail) => (
          <ContentRail
            key={rail.id}
            title={rail.title}
            items={rail.items}
            onWatchlistChanged={loadData}
          />
        ))}
      </div>
    </div>
  );
}
