'use client';

import React, { useRef, useState } from 'react';
import Link from 'next/link';
import { ChevronLeft, ChevronRight, Play } from 'lucide-react';
import { PosterCard } from '@/components/cards/PosterCard';
import { cn } from '@/lib/utils';
import type { Title, ViewingProgress } from '@/types';

interface ContentRailProps {
  title: string;
  items?: Title[];
  progressItems?: ViewingProgress[];
  isTop10?: boolean;
  seeAllHref?: string;
  onWatchlistChanged?: () => void;
}

export default function ContentRail({
  title,
  items,
  progressItems,
  isTop10 = false,
  seeAllHref,
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
    <section className="group/rail relative py-5 my-2" aria-label={title}>
      {/* Rail Header */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mb-3 flex items-baseline justify-between">
        <h2 className="font-heading font-bold text-xl sm:text-2xl text-foreground tracking-wide">
          {title}
        </h2>
        {seeAllHref && (
          <Link
            href={seeAllHref}
            className="text-xs sm:text-sm font-medium text-cyan opacity-0 transition-opacity group-hover/rail:opacity-100 focus-visible:opacity-100 hover:underline"
          >
            Explore all ›
          </Link>
        )}
      </div>

      {/* Scrollable Track Container */}
      <div className="relative">
        {/* Left Scroll Arrow */}
        <button
          onClick={() => scroll('left')}
          aria-label="Scroll left"
          className={cn(
            'absolute left-0 top-0 bottom-0 z-40 hidden md:grid place-items-center w-12 bg-gradient-to-r from-background via-background/80 to-transparent transition-opacity cursor-pointer',
            showLeftArrow ? 'opacity-0 group-hover/rail:opacity-100' : 'pointer-events-none opacity-0'
          )}
        >
          <div className="w-9 h-9 rounded-full bg-surface-elevated/90 border border-border flex items-center justify-center text-foreground hover:scale-110 hover:border-cyan transition-transform">
            <ChevronLeft className="w-5 h-5" />
          </div>
        </button>

        {/* Cards Row */}
        <div
          ref={scrollRef}
          onScroll={handleScroll}
          className="no-scrollbar flex snap-x snap-mandatory gap-4 overflow-x-auto scroll-smooth px-4 sm:px-6 lg:px-8 pb-12 pt-2 -mb-8"
        >
          {progressItems ? (
            progressItems.map((prog) => {
              const displayTitle: Title = prog.title || {
                id: prog.contentId,
                type: prog.targetType === 'EPISODE' ? 'SERIES' : 'MOVIE',
                title: 'Continue Watching',
                synopsis: '',
                releaseYear: 2024,
                maturityRating: 'PG-13',
                durationMinutes: Math.round(prog.durationSeconds / 60),
                posterUrl: 'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80',
                backdropUrl: '',
                publicationState: 'PUBLISHED',
              };
              const remainingMinutes = Math.max(1, Math.round((prog.durationSeconds - prog.positionSeconds) / 60));

              return (
                <div key={prog.id} className="relative w-40 sm:w-48 shrink-0 snap-start group/prog">
                  <PosterCard title={displayTitle} className="w-full" />
                  <div className="absolute inset-x-0 bottom-0 z-20 overflow-hidden rounded-b-xl bg-black/70 backdrop-blur-xs px-2.5 py-1">
                    <div className="flex items-center justify-between text-[10px] text-muted-foreground mb-1">
                      <span>{remainingMinutes}m left</span>
                      <span className="font-semibold text-cyan">{Math.round(prog.progressPercent)}%</span>
                    </div>
                    <div className="h-1 w-full bg-surface-elevated rounded-full overflow-hidden">
                      <div
                        className="h-full bg-cyan rounded-full transition-all"
                        style={{ width: `${Math.min(100, prog.progressPercent)}%` }}
                      />
                    </div>
                  </div>
                </div>
              );
            })
          ) : isTop10 ? (
            items?.map((item, i) => (
              <div key={item.id} className="flex shrink-0 snap-start items-end">
                <span
                  aria-hidden
                  className="outline-number -mr-6 select-none font-display text-[7.5rem] sm:text-[9.5rem] font-black leading-[0.8] z-10 drop-shadow-md pointer-events-none"
                >
                  {i + 1}
                </span>
                <PosterCard title={item} className="w-36 shrink-0 sm:w-44 lg:w-48" />
              </div>
            ))
          ) : (
            items?.map((item) => (
              <PosterCard
                key={item.id}
                title={item}
                className="w-36 shrink-0 snap-start sm:w-44 lg:w-48"
              />
            ))
          )}
        </div>

        {/* Right Scroll Arrow */}
        <button
          onClick={() => scroll('right')}
          aria-label="Scroll right"
          className={cn(
            'absolute right-0 top-0 bottom-0 z-40 hidden md:grid place-items-center w-12 bg-gradient-to-l from-background via-background/80 to-transparent transition-opacity cursor-pointer',
            showRightArrow ? 'opacity-0 group-hover/rail:opacity-100' : 'pointer-events-none opacity-0'
          )}
        >
          <div className="w-9 h-9 rounded-full bg-surface-elevated/90 border border-border flex items-center justify-center text-foreground hover:scale-110 hover:border-cyan transition-transform">
            <ChevronRight className="w-5 h-5" />
          </div>
        </button>
      </div>
    </section>
  );
}
