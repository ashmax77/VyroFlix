'use client';

import React, { useEffect, useState } from 'react';
import HeroBillboard from '@/components/HeroBillboard';
import ContentRail from '@/components/ContentRail';
import { Spinner } from '@/components/common';
import { fetchHomeFeed, fetchContinueWatching, fetchTrendingRail, fetchPersonalRecommendations } from '@/lib/api';
import type { HomeFeed, ViewingProgress, Title } from '@/types';

export default function BrowsePage() {
  const [feed, setFeed] = useState<HomeFeed | null>(null);
  const [continueWatching, setContinueWatching] = useState<ViewingProgress[]>([]);
  const [trending, setTrending] = useState<Title[]>([]);
  const [recommended, setRecommended] = useState<Title[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    try {
      const [feedData, progressData, trendingData, recData] = await Promise.all([
        fetchHomeFeed(),
        fetchContinueWatching(),
        fetchTrendingRail(10),
        fetchPersonalRecommendations(10),
      ]);
      setFeed(feedData);
      setContinueWatching(progressData);
      setTrending(trendingData);
      setRecommended(recData);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Spinner label="Loading catalog & recommendations..." />
      </div>
    );
  }

  if (!feed || !feed.featuredTitle) {
    return (
      <div className="min-h-screen flex items-center justify-center text-text-secondary">
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
