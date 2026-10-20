'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { Bookmark, Play, Trash2, Loader2 } from 'lucide-react';
import { fetchWatchlist, toggleWatchlist } from '../../lib/api';
import { WatchlistEntry } from '../../types';

export default function MyListPage() {
  const [watchlist, setWatchlist] = useState<WatchlistEntry[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    try {
      const data = await fetchWatchlist();
      setWatchlist(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleRemove = async (titleId: string) => {
    await toggleWatchlist(titleId, true);
    setWatchlist((prev) => prev.filter((item) => item.titleId !== titleId));
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="w-10 h-10 text-primary animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-20 min-h-screen">
      <div className="flex items-center space-x-3 mb-8">
        <Bookmark className="w-7 h-7 text-primary-gradient" />
        <h1 className="font-heading font-extrabold text-3xl sm:text-4xl text-white">
          My List
        </h1>
      </div>

      {watchlist.length === 0 ? (
        <div className="glass-panel p-12 rounded-card border border-border text-center max-w-lg mx-auto mt-16">
          <Bookmark className="w-12 h-12 text-text-muted mx-auto mb-4" />
          <h3 className="font-heading font-bold text-lg text-white mb-2">Your list is empty</h3>
          <p className="text-text-secondary text-sm mb-6">
            Explore our catalog of movies and series to add titles you want to watch later.
          </p>
          <Link
            href="/browse"
            className="inline-block px-6 py-3 gradient-accent text-white font-bold rounded-full text-sm shadow-lg hover:scale-105 transition-transform"
          >
            Browse Catalog
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6">
          {watchlist.map((entry) => {
            const title = entry.title;
            if (!title) return null;

            return (
              <div
                key={entry.id}
                className="group relative aspect-[2/3] rounded-card overflow-hidden bg-surface-elevated border border-border transition-all duration-300 hover:scale-105 hover:shadow-2xl hover:shadow-purple-900/30"
              >
                <img
                  src={title.posterUrl}
                  alt={title.title}
                  className="w-full h-full object-cover"
                />

                <div className="absolute inset-0 bg-gradient-to-t from-background via-background/70 to-transparent opacity-0 group-hover:opacity-100 transition-opacity p-4 flex flex-col justify-end">
                  <h4 className="font-heading font-bold text-sm text-white line-clamp-1 mb-1">
                    {title.title}
                  </h4>
                  <div className="flex items-center space-x-2 text-[10px] text-text-secondary mb-3">
                    <span>{title.releaseYear}</span>
                    <span>•</span>
                    <span>{title.durationMinutes}m</span>
                  </div>

                  <div className="flex items-center space-x-2">
                    <Link
                      href={`/watch/${title.id}`}
                      className="flex-1 flex items-center justify-center space-x-1 py-1.5 bg-white text-black hover:bg-gray-200 rounded-full font-bold text-xs transition-colors"
                    >
                      <Play className="w-3.5 h-3.5 fill-black" />
                      <span>Play</span>
                    </Link>

                    <button
                      onClick={() => handleRemove(entry.titleId)}
                      className="w-8 h-8 rounded-full border border-border bg-surface-elevated hover:bg-red-500/20 hover:border-red-500 text-white flex items-center justify-center transition-colors"
                      title="Remove from My List"
                    >
                      <Trash2 className="w-4 h-4 text-red-400" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
