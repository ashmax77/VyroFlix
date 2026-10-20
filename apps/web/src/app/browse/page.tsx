'use client';

import React, { useEffect, useState } from 'react';
import HeroBillboard from '../../components/HeroBillboard';
import ContentRail from '../../components/ContentRail';
import { fetchHomeFeed, fetchContinueWatching } from '../../lib/api';
import { HomeFeed, ViewingProgress } from '../../types';
import { Loader2 } from 'lucide-react';

export default function BrowsePage() {
  const [feed, setFeed] = useState<HomeFeed | null>(null);
  const [continueWatching, setContinueWatching] = useState<ViewingProgress[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    try {
      const [feedData, progressData] = await Promise.all([
        fetchHomeFeed(),
        fetchContinueWatching(),
      ]);
      setFeed(feedData);
      setContinueWatching(progressData);
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

        {/* Dynamic Aggregated Rails */}
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
