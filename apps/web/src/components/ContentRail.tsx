'use client';

import React, { useRef, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import TitleCard from './TitleCard';
import { Title, ViewingProgress } from '../types';

interface ContentRailProps {
  title: string;
  items?: Title[];
  progressItems?: ViewingProgress[];
  onWatchlistChanged?: () => void;
}

export default function ContentRail({
  title,
  items,
  progressItems,
  onWatchlistChanged,
}: ContentRailProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const [showLeftArrow, setShowLeftArrow] = useState(false);
  const [showRightArrow, setShowRightArrow] = useState(true);

  const handleScroll = () => {
    if (scrollRef.current) {
      const { scrollLeft, scrollWidth, clientWidth } = scrollRef.current;
      setShowLeftArrow(scrollLeft > 20);
      setShowRightArrow(scrollLeft < scrollWidth - clientWidth - 20);
    }
  };

  const scroll = (direction: 'left' | 'right') => {
    if (scrollRef.current) {
      const scrollAmount = scrollRef.current.clientWidth * 0.75;
      scrollRef.current.scrollBy({
        left: direction === 'left' ? -scrollAmount : scrollAmount,
        behavior: 'smooth',
      });
    }
  };

  const hasItems = (items && items.length > 0) || (progressItems && progressItems.length > 0);
  if (!hasItems) {
    return null;
  }

  return (
    <div className="relative group/rail py-4 my-2">
      {/* Rail Header */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mb-3 flex items-center justify-between">
        <h3 className="font-heading font-bold text-lg sm:text-xl text-white tracking-wide">
          {title}
        </h3>
      </div>

      {/* Scrollable Track Container */}
      <div className="relative">
        {/* Left Scroll Arrow */}
        {showLeftArrow && (
          <button
            onClick={() => scroll('left')}
            className="absolute left-0 top-0 bottom-0 z-30 w-12 bg-gradient-to-r from-background to-transparent flex items-center justify-center opacity-0 group-hover/rail:opacity-100 transition-opacity"
            aria-label="Scroll left"
          >
            <div className="w-9 h-9 rounded-full bg-surface-elevated/80 border border-border flex items-center justify-center text-white hover:scale-110 transition-transform">
              <ChevronLeft className="w-5 h-5" />
            </div>
          </button>
        )}

        {/* Cards Row */}
        <div
          ref={scrollRef}
          onScroll={handleScroll}
          className="flex space-x-4 overflow-x-auto no-scrollbar scroll-smooth px-4 sm:px-6 lg:px-8 py-2"
        >
          {progressItems
            ? progressItems.map((prog) => (
                <TitleCard
                  key={prog.id}
                  title={prog.title || {
                    id: prog.contentId,
                    type: prog.targetType === 'EPISODE' ? 'SERIES' : 'MOVIE',
                    title: 'Untitled Video',
                    synopsis: '',
                    releaseYear: 2024,
                    maturityRating: 'PG-13',
                    durationMinutes: Math.round(prog.durationSeconds / 60),
                    posterUrl: 'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80',
                    backdropUrl: '',
                    publicationState: 'PUBLISHED',
                  }}
                  progress={prog}
                  onWatchlistChanged={onWatchlistChanged}
                />
              ))
            : items?.map((item) => (
                <TitleCard
                  key={item.id}
                  title={item}
                  onWatchlistChanged={onWatchlistChanged}
                />
              ))}
        </div>

        {/* Right Scroll Arrow */}
        {showRightArrow && (
          <button
            onClick={() => scroll('right')}
            className="absolute right-0 top-0 bottom-0 z-30 w-12 bg-gradient-to-l from-background to-transparent flex items-center justify-center opacity-0 group-hover/rail:opacity-100 transition-opacity"
            aria-label="Scroll right"
          >
            <div className="w-9 h-9 rounded-full bg-surface-elevated/80 border border-border flex items-center justify-center text-white hover:scale-110 transition-transform">
              <ChevronRight className="w-5 h-5" />
            </div>
          </button>
        )}
      </div>
    </div>
  );
}
