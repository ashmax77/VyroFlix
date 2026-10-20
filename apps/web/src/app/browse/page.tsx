'use client';

import React, { useEffect, useState } from 'react';
import HeroBillboard from '../../components/HeroBillboard';
import ContentRail from '../../components/ContentRail';
import { fetchHomeFeed, fetchContinueWatching, fetchTrendingRail, fetchPersonalRecommendations } from '../../lib/api';
import { HomeFeed, ViewingProgress, Title } from '../../types';
import { Loader2 } from 'lucide-react';

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
        <Loader2 className="w-10 h-10 text-primary animate-spin" />
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

  return (
    <div className="min-h-screen pb-20">
      {/* Cinematic Hero Billboard */}
      <HeroBillboard title={feed.featuredTitle} />

      {/* Content Rails Section */}
      <div className="relative -mt-16 sm:-mt-24 z-20 space-y-6">
        {/* Continue Watching Rail (if user has active viewing progress) */}
        {continueWatching.length > 0 && (
          <ContentRail
            title="Continue Watching for You"
            progressItems={continueWatching}
            onWatchlistChanged={loadData}
          />
        )}

        {/* Dynamic Trending Rail from Recommendation-Service */}
        {trending.length > 0 && (
          <ContentRail
            title="Trending on VyroFlix"
            items={trending}
            onWatchlistChanged={loadData}
          />
        )}

        {/* Personalized Recommendations Rail */}
        {recommended.length > 0 && (
          <ContentRail
            title="Recommended for You"
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
