'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Poster } from '@/components/cards/Poster';
import type { Title, ViewingProgress } from '@/types';

interface ContentRailProps {
  title: string;
  items?: Title[];
  progressItems?: ViewingProgress[];
  isTop10?: boolean;
  seeAllHref?: string;
}

/** Stand-in for a progress item whose title failed to load: still shows the position, not invented metadata. */
function placeholderTitle(prog: ViewingProgress): Title {
  return {
    id: prog.contentId,
    type: prog.targetType === 'EPISODE' ? 'SERIES' : 'MOVIE',
    title: 'Untitled',
    synopsis: '',
    releaseYear: 0,
    maturityRating: '',
    posterUrl: '',
    bannerUrl: null,
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
    setCanPrev(el.scrollLeft > 4);
    setCanNext(el.scrollLeft < el.scrollWidth - el.clientWidth - 4);
  }, []);

  useEffect(() => {
    updateEdges();
    window.addEventListener('resize', updateEdges);
    return () => window.removeEventListener('resize', updateEdges);
  }, [updateEdges, items, progressItems]);

  const page = (dir: 1 | -1) => {
    const el = trackRef.current;
    if (!el) return;
    el.scrollBy({ left: dir * el.clientWidth * 0.8, behavior: 'smooth' });
  };

  const count = progressItems?.length ?? items?.length ?? 0;
  if (count === 0) return null;

  return (
    <section className="py-8" aria-labelledby={`rail-${title.replace(/\s+/g, '-').toLowerCase()}`}>
      <div className="mx-auto flex max-w-container items-baseline justify-between gap-4 px-4 sm:px-6 lg:px-10">
        <h2 id={`rail-${title.replace(/\s+/g, '-').toLowerCase()}`} className="text-xl text-foreground sm:text-2xl">
          {title}
        </h2>
        <div className="flex items-center gap-3">
          {seeAllHref && (
            <Link href={seeAllHref} className="text-sm text-muted-foreground hover:text-foreground">
              See all
            </Link>
          )}
          <div className="hidden gap-1 md:flex">
            <button
              type="button"
              onClick={() => page(-1)}
              disabled={!canPrev}
              aria-label={`Scroll ${title} left`}
              className="grid h-8 w-8 place-items-center rounded-card border border-border text-muted-foreground transition-colors hover:text-foreground disabled:opacity-30"
            >
              <ChevronLeft className="h-4 w-4" aria-hidden />
            </button>
            <button
              type="button"
              onClick={() => page(1)}
              disabled={!canNext}
              aria-label={`Scroll ${title} right`}
              className="grid h-8 w-8 place-items-center rounded-card border border-border text-muted-foreground transition-colors hover:text-foreground disabled:opacity-30"
            >
              <ChevronRight className="h-4 w-4" aria-hidden />
            </button>
          </div>
        </div>
      </div>

      <div className="mx-auto mt-4 max-w-container">
        <div
          ref={trackRef}
          onScroll={updateEdges}
          tabIndex={0}
          aria-label={`${title}, scrollable`}
          className="no-scrollbar flex snap-x snap-mandatory gap-4 overflow-x-auto scroll-pl-4 px-4 pb-2 sm:gap-5 sm:px-6 lg:px-10"
        >
          {progressItems
            ? progressItems.map((prog) => {
                const remaining = Math.max(1, Math.round((prog.durationSeconds - prog.positionSeconds) / 60));
                return (
                  <div key={prog.id} className="w-36 shrink-0 snap-start sm:w-44 lg:w-48">
                    <Poster
                      title={prog.title ?? placeholderTitle(prog)}
                      progress={{ percent: prog.progressPercent, minutesLeft: remaining }}
                    />
                  </div>
                );
              })
            : isTop10
              ? items?.map((item, i) => (
                  <div key={item.id} className="flex shrink-0 snap-start items-end gap-2">
                    <span
                      aria-hidden
                      className="outline-number w-14 select-none text-right font-heading text-[6.5rem] font-semibold leading-[0.78] sm:w-20 sm:text-[8rem]"
                    >
                      {i + 1}
                    </span>
                    <div className="w-28 sm:w-36 lg:w-44">
                      <Poster title={item} />
                    </div>
                  </div>
                ))
              : items?.map((item) => (
                  <div key={item.id} className="w-36 shrink-0 snap-start sm:w-44 lg:w-48">
                    <Poster title={item} />
                  </div>
                ))}
        </div>
      </div>
    </section>
  );
}
