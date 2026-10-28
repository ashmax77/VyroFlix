'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { Play, Plus, Check, Info } from 'lucide-react';
import { Title, ViewingProgress } from '../types';
import { ApiError, redirectToLogin, toggleWatchlist } from '../lib/api';

interface TitleCardProps {
  title: Title;
  progress?: ViewingProgress;
  onWatchlistChanged?: () => void;
}

export default function TitleCard({ title, progress, onWatchlistChanged }: TitleCardProps) {
  const [isHovered, setIsHovered] = useState(false);
  const [inList, setInList] = useState(false);
  const [loadingList, setLoadingList] = useState(false);

  const handleToggleList = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setLoadingList(true);
    try {
      const updated = await toggleWatchlist(title.id, inList);
      setInList(updated);
      if (onWatchlistChanged) {
        onWatchlistChanged();
      }
    } catch (err) {
      if (err instanceof ApiError && err.isUnauthorized) redirectToLogin();
    } finally {
      setLoadingList(false);
    }
  };

  const progressPercent = progress ? Math.round(progress.progressPercent) : 0;
  const remainingMinutes = progress
    ? Math.max(1, Math.round((progress.durationSeconds - progress.positionSeconds) / 60))
    : 0;

  return (
    <div
      className="relative flex-none w-[170px] sm:w-[210px] md:w-[240px] aspect-[2/3] rounded-card overflow-hidden bg-surface-elevated transition-all duration-300 transform hover:scale-105 hover:z-20 hover:shadow-2xl hover:shadow-purple-900/30 group"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      {/* Poster Image */}
      <img
        src={title.posterUrl}
        alt={title.title}
        className="w-full h-full object-cover"
        loading="lazy"
      />

      {/* Static Subtle Gradient Overlay */}
      <div className="absolute inset-0 bg-gradient-to-t from-background/90 via-transparent to-transparent opacity-60 group-hover:opacity-20 transition-opacity" />

      {/* Continue Watching Progress Bar (when progress provided) */}
      {progress && (
        <div className="absolute bottom-0 left-0 right-0 z-10">
          <div className="bg-black/60 backdrop-blur-sm px-2.5 py-1 flex items-center justify-between text-[11px] text-text-secondary">
            <span>{remainingMinutes}m left</span>
            <span className="font-semibold text-accent-cyan">{progressPercent}%</span>
          </div>
          <div className="w-full h-1 bg-surface-hover">
            <div
              className="h-full bg-accent-cyan transition-all"
              style={{ width: `${progressPercent}%` }}
            />
          </div>
        </div>
      )}

      {/* Hover Mini-Card Reveal */}
      <div
        className={`absolute inset-0 bg-gradient-to-t from-background via-background/80 to-transparent p-4 flex flex-col justify-end transition-opacity duration-200 ${
          isHovered ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}
      >
        <h4 className="font-heading font-bold text-sm sm:text-base text-white line-clamp-1 mb-1">
          {title.title}
        </h4>

        {/* Metadata chips */}
        <div className="flex items-center space-x-2 text-[11px] text-text-secondary mb-3">
          <span className="px-1.5 py-0.5 border border-border rounded text-[10px] text-text-primary">
            {title.maturityRating}
          </span>
          <span>{title.releaseYear}</span>
          <span>•</span>
          <span>{title.durationMinutes}m</span>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center space-x-2">
          <Link
            href={`/watch/${title.id}`}
            className="flex-1 flex items-center justify-center space-x-1 py-1.5 bg-white text-black hover:bg-gray-200 rounded-full font-semibold text-xs transition-colors"
          >
            <Play className="w-3.5 h-3.5 fill-black" />
            <span>Play</span>
          </Link>

          <button
            onClick={handleToggleList}
            disabled={loadingList}
            className="w-8 h-8 rounded-full border border-border bg-surface-elevated/80 hover:border-white text-white flex items-center justify-center transition-colors"
            title={inList ? 'Remove from My List' : 'Add to My List'}
          >
            {inList ? <Check className="w-4 h-4 text-green-400" /> : <Plus className="w-4 h-4" />}
          </button>

          <Link
            href={`/title/${title.id}`}
            className="w-8 h-8 rounded-full border border-border bg-surface-elevated/80 hover:border-white text-white flex items-center justify-center transition-colors"
            title="More Info"
          >
            <Info className="w-4 h-4" />
          </Link>
        </div>
      </div>
    </div>
  );
}
