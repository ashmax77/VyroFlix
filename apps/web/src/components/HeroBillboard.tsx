'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { Play, Plus, Check, Info } from 'lucide-react';
import { Title } from '../types';
import { toggleWatchlist, checkWatchlist } from '../lib/api';

interface HeroBillboardProps {
  title: Title;
}

export default function HeroBillboard({ title }: HeroBillboardProps) {
  const [inList, setInList] = useState(false);
  const [loadingList, setLoadingList] = useState(false);

  useEffect(() => {
    let isMounted = true;
    checkWatchlist(title.id).then((res) => {
      if (isMounted) setInList(res);
    });
    return () => {
      isMounted = false;
    };
  }, [title.id]);

  const handleToggleList = async () => {
    setLoadingList(true);
    const updated = await toggleWatchlist(title.id, inList);
    setInList(updated);
    setLoadingList(false);
  };

  return (
    <div className="relative w-full h-[65vh] sm:h-[75vh] lg:h-[82vh] overflow-hidden select-none">
      {/* Backdrop Image */}
      <img
        src={title.backdropUrl || title.posterUrl}
        alt={title.title}
        className="absolute inset-0 w-full h-full object-cover object-center"
      />

      {/* Atmospheric Multi-layer Gradient Fades */}
      <div className="absolute inset-0 bg-gradient-to-r from-background via-background/70 to-transparent" />
      <div className="absolute inset-0 bg-gradient-to-t from-background via-background/20 to-transparent" />
      <div className="absolute inset-0 bg-gradient-to-b from-background/40 via-transparent to-transparent" />

      {/* Hero Content */}
      <div className="relative max-w-7xl mx-auto h-full px-4 sm:px-6 lg:px-8 flex flex-col justify-end pb-16 sm:pb-20 z-10">
        <div className="max-w-2xl">
          {/* Tagline / Badges */}
          <div className="flex items-center space-x-2.5 mb-3 text-xs sm:text-sm font-medium">
            <span className="px-2 py-0.5 rounded bg-primary/30 border border-primary text-primary-gradient font-bold tracking-wider uppercase text-[10px]">
              Featured {title.type}
            </span>
            <span className="px-1.5 py-0.5 border border-border rounded text-[11px] text-text-secondary">
              {title.maturityRating}
            </span>
            <span className="text-text-secondary">{title.releaseYear}</span>
            <span className="text-text-secondary">•</span>
            <span className="text-text-secondary">{title.durationMinutes}m</span>
          </div>

          {/* Title */}
          <h1 className="font-heading font-extrabold text-3xl sm:text-5xl lg:text-6xl text-white tracking-tight leading-tight mb-4 drop-shadow-md">
            {title.title}
          </h1>

          {/* Synopsis */}
          <p className="text-text-secondary text-sm sm:text-base line-clamp-3 mb-6 leading-relaxed max-w-xl">
            {title.synopsis}
          </p>

          {/* Action Buttons */}
          <div className="flex flex-wrap items-center gap-3">
            <Link
              href={`/watch/${title.id}`}
              className="flex items-center space-x-2 px-6 py-3 bg-white text-black hover:bg-gray-200 rounded-full font-bold text-sm sm:text-base shadow-xl transition-all transform hover:scale-105"
            >
              <Play className="w-5 h-5 fill-black" />
              <span>Play Now</span>
            </Link>

            <button
              onClick={handleToggleList}
              disabled={loadingList}
              className="flex items-center space-x-2 px-5 py-3 glass-panel text-white hover:bg-surface-elevated rounded-full font-semibold text-sm transition-all border border-border hover:border-white"
            >
              {inList ? (
                <>
                  <Check className="w-5 h-5 text-green-400" />
                  <span>In My List</span>
                </>
              ) : (
                <>
                  <Plus className="w-5 h-5" />
                  <span>Add to List</span>
                </>
              )}
            </button>

            <Link
              href={`/title/${title.id}`}
              className="flex items-center space-x-2 px-5 py-3 glass-panel text-text-secondary hover:text-white rounded-full font-medium text-sm transition-all border border-border"
            >
              <Info className="w-5 h-5" />
              <span>Details</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
