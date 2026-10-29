'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { ChevronLeft, ChevronRight, Play } from 'lucide-react';
import { Poster } from '@/components/cards/Poster';
import { cn } from '@/lib/utils';
import type { Title, ViewingProgress } from '@/types';

interface ContentRailProps {
  title: string;
  items?: Title[];
  progressItems?: ViewingProgress[];
  isTop10?: boolean;
  seeAllHref?: string;
}

function placeholderTitle(prog: ViewingProgress): Title {
  return {
    id: prog.contentId,
    type: prog.targetType === 'EPISODE' ? 'SERIES' : 'MOVIE',
    title: 'Resume stream',
    synopsis: '',
    releaseYear: 2024,
    maturityRating: 'PG-13',
    posterUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&q=80',
    bannerUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&q=85',
    publicationState: 'PUBLISHED',
  };
}

export default function ContentRail({ title, items, progressItems, isTop10 = false, seeAllHref }: ContentRailProps) {
  const trackRef = useRef<HTMLDivElement>(null);
  const [canPrev, setCanPrev] = useState(false);
  const [canNext, setCanNext] = useState(false);

  const updateEdges = useCallback(() => {
    const el = trackRef.current;
    if (!el) return;
    setCanPrev(el.scrollLeft > 6);
    setCanNext(el.scrollLeft < el.scrollWidth - el.clientWidth - 6);
  }, []);

  useEffect(() => {
    updateEdges();
    window.addEventListener('resize', updateEdges);
    return () => window.removeEventListener('resize', updateEdges);
  }, [updateEdges, items, progressItems]);

  const scroll = (dir: 1 | -1) => {
    const el = trackRef.current;
    if (!el) return;
    el.scrollBy({ left: dir * el.clientWidth * 0.85, behavior: 'smooth' });
  };

  const count = progressItems?.length ?? items?.length ?? 0;
  if (count === 0) return null;

  return (
    <section className="group/rail relative py-6" aria-label={title}>
      {/* Header */}
      <div className="mx-auto flex max-w-[1800px] items-center justify-between gap-4 px-4 sm:px-8 lg:px-12 mb-3">
        <h2 className="text-lg font-bold tracking-tight text-foreground sm:text-xl lg:text-2xl">
          {title}
        </h2>
        {seeAllHref && (
          <Link
            href={seeAllHref}
            className="flex items-center text-xs font-semibold text-cyan hover:underline transition-colors"
          >
            Explore all <ChevronRight className="h-3.5 w-3.5 ml-0.5" />
          </Link>
        )}
      </div>

      {/* Rail Container */}
      <div className="relative mx-auto max-w-[1800px]">
        {/* Left Arrow */}
        <button
          type="button"
          onClick={() => scroll(-1)}
          disabled={!canPrev}
          aria-label={`Scroll ${title} left`}
          className={cn(
            'absolute left-0 top-0 bottom-4 z-40 hidden md:grid place-items-center w-12 bg-background/70 backdrop-blur-md text-foreground transition-all duration-300 opacity-0 group-hover/rail:opacity-100 hover:bg-background/90',
            !canPrev && 'pointer-events-none !opacity-0'
          )}
        >
          <ChevronLeft className="h-7 w-7" />
        </button>

        {/* Scrollable Track */}
        <div
          ref={trackRef}
          onScroll={updateEdges}
          tabIndex={0}
          aria-label={`${title}, horizontal list`}
          className="no-scrollbar flex snap-x snap-mandatory gap-3 overflow-x-auto scroll-px-4 px-4 pb-4 pt-1 sm:gap-4 sm:scroll-px-8 sm:px-8 lg:scroll-px-12 lg:px-12"
        >
          {progressItems ? (
            progressItems.map((prog) => {
              const remaining = Math.max(1, Math.round((prog.durationSeconds - prog.positionSeconds) / 60));
              const t = prog.title ?? placeholderTitle(prog);
              return (
                <div key={prog.id} className="w-[70vw] min-w-[220px] max-w-[320px] shrink-0 snap-start sm:w-72 lg:w-80">
                  <Poster
                    title={t}
                    variant="art"
                    progress={{ percent: prog.progressPercent, minutesLeft: remaining }}
                  />
                </div>
              );
            })
          ) : isTop10 ? (
            items?.map((item, i) => (
              <div key={item.id} className="flex shrink-0 snap-start items-end">
                <span
                  aria-hidden
                  className="outline-number mb-6 -mr-3 sm:-mr-4 select-none font-display text-[5.5rem] font-black leading-none sm:text-[7.5rem] lg:text-[9.5rem]"
                >
                  {i + 1}
                </span>
                <div className="w-[45vw] min-w-[150px] max-w-[240px] shrink-0 sm:w-56 lg:w-64">
                  <Poster title={item} variant="art" />
                </div>
              </div>
            ))
          ) : (
            items?.map((item) => (
              <div key={item.id} className="w-32 shrink-0 snap-start sm:w-40 lg:w-44">
                <Poster title={item} variant="poster" />
              </div>
            ))
          )}
        </div>

        {/* Right Arrow */}
        <button
          type="button"
          onClick={() => scroll(1)}
          disabled={!canNext}
          aria-label={`Scroll ${title} right`}
          className={cn(
            'absolute right-0 top-0 bottom-4 z-40 hidden md:grid place-items-center w-12 bg-background/70 backdrop-blur-md text-foreground transition-all duration-300 opacity-0 group-hover/rail:opacity-100 hover:bg-background/90',
            !canNext && 'pointer-events-none !opacity-0'
          )}
        >
          <ChevronRight className="h-7 w-7" />
        </button>
      </div>
    </section>
  );
}
