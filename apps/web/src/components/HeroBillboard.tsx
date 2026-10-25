'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Play, Plus, Check, Info, Pause, PlayCircle } from 'lucide-react';
import { MaturityBadge, Skeleton } from '@/components/common';
import { Button } from '@/components/ui/button';
import { toggleWatchlist, checkWatchlist } from '@/lib/api';
import { cn } from '@/lib/utils';
import type { Title } from '@/types';

interface HeroBillboardProps {
  title?: Title;
  titles?: Title[];
  loading?: boolean;
}

export default function HeroBillboard({ title, titles, loading }: HeroBillboardProps) {
  const router = useRouter();
  const titleList: Title[] = titles?.length ? titles : title ? [title] : [];
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const [inList, setInList] = useState(false);
  const [isToggling, setIsToggling] = useState(false);

  useEffect(() => {
    if (paused || titleList.length <= 1) return;
    const interval = setInterval(() => {
      setIndex((curr) => (curr + 1) % titleList.length);
    }, 8000);
    return () => clearInterval(interval);
  }, [paused, titleList.length]);

  const activeTitle = titleList[index];

  useEffect(() => {
    if (!activeTitle) return;
    let mounted = true;
    checkWatchlist(activeTitle.id).then((val) => {
      if (mounted) setInList(val);
    });
    return () => {
      mounted = false;
    };
  }, [activeTitle]);

  const handleToggleList = async () => {
    if (!activeTitle || isToggling) return;
    setIsToggling(true);
    try {
      const updated = await toggleWatchlist(activeTitle.id, inList);
      setInList(updated);
    } finally {
      setIsToggling(false);
    }
  };

  if (loading || !activeTitle) {
    return (
      <div className="relative h-[78vh] min-h-[520px]">
        <Skeleton className="absolute inset-0 rounded-none" />
      </div>
    );
  }

  const matchPercent = 95 + (activeTitle.id.charCodeAt(0) % 5);

  return (
    <section
      className="relative h-[82vh] min-h-[560px] overflow-hidden select-none"
      aria-roledescription="carousel"
      aria-label="Featured titles"
    >
      {/* Background Backdrops crossfade */}
      {titleList.map((t, k) => (
        <img
          key={t.id}
          src={t.backdropUrl || t.posterUrl}
          alt={t.title}
          className={cn(
            'absolute inset-0 h-full w-full object-cover transition-opacity duration-1000',
            k === index ? 'opacity-100' : 'opacity-0 pointer-events-none'
          )}
        />
      ))}

      {/* Atmospheric Overlays */}
      <div className="absolute inset-0 bg-fade-left" />
      <div className="absolute inset-0 bg-fade-bottom" />

      {/* Content */}
      <div className="relative mx-auto flex h-full max-w-7xl flex-col justify-end px-4 pb-20 sm:px-6 lg:px-8 lg:pb-28 z-10" key={activeTitle.id}>
        <div className="animate-page-in max-w-2xl">
          <p className="mb-3 flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.25em] text-cyan">
            <PlayCircle className="h-4 w-4" /> VyroFlix {activeTitle.type === 'SERIES' ? 'Series' : 'Film'}
          </p>

          <h1 className="text-4xl sm:text-6xl lg:text-7xl font-extrabold leading-none tracking-tight text-foreground drop-shadow-lg">
            {activeTitle.title}
          </h1>

          <div className="mt-4 flex flex-wrap items-center gap-3 text-sm text-text-secondary">
            <span className="font-semibold text-success">{matchPercent}% match</span>
            <MaturityBadge rating={activeTitle.maturityRating || 'PG-13'} />
            <span>{activeTitle.releaseYear}</span>
            <span>
              {activeTitle.type === 'SERIES'
                ? `${activeTitle.seasons?.length || 1} Season${(activeTitle.seasons?.length || 1) > 1 ? 's' : ''}`
                : `${activeTitle.durationMinutes || 120}m`}
            </span>
            {activeTitle.tags?.slice(0, 3).map((tag) => (
              <span key={tag} className="rounded-full bg-foreground/10 px-2.5 py-0.5 text-xs text-foreground">
                {tag}
              </span>
            ))}
          </div>

          <p className="mt-4 line-clamp-3 max-w-xl text-base text-foreground/85 sm:text-lg leading-relaxed">
            {activeTitle.synopsis}
          </p>

          <div className="mt-7 flex flex-wrap items-center gap-3">
            <Button
              variant="light"
              size="lg"
              onClick={() => router.push(`/watch/${activeTitle.id}`)}
              className="gap-2.5 font-bold shadow-lg cursor-pointer"
            >
              <Play className="h-5 w-5 fill-current ml-0.5" /> Play
            </Button>

            <Button
              variant="glass"
              size="lg"
              onClick={handleToggleList}
              disabled={isToggling}
              className="gap-2 cursor-pointer"
            >
              {inList ? <Check className="text-cyan h-5 w-5" /> : <Plus className="h-5 w-5" />}
              <span>{inList ? 'In My List' : 'My List'}</span>
            </Button>

            <Button variant="glass" size="lg" asChild className="gap-2 cursor-pointer">
              <Link href={`/title/${activeTitle.id}`}>
                <Info className="h-5 w-5" />
                <span>More info</span>
              </Link>
            </Button>
          </div>
        </div>

        {/* Rotation Controls and Pill Indicators */}
        {titleList.length > 1 && (
          <div className="mt-8 flex items-center gap-2.5">
            <button
              aria-label={paused ? 'Resume rotation' : 'Pause rotation'}
              onClick={() => setPaused((p) => !p)}
              className="grid h-8 w-8 place-items-center rounded-full glass hover:bg-surface-elevated transition-colors text-foreground cursor-pointer"
            >
              {paused ? <Play className="h-3.5 w-3.5 ml-0.5" /> : <Pause className="h-3.5 w-3.5" />}
            </button>
            {titleList.map((t, k) => (
              <button
                key={t.id}
                aria-label={`Show ${t.title}`}
                aria-current={k === index}
                onClick={() => setIndex(k)}
                className={cn(
                  'h-1.5 rounded-full transition-all cursor-pointer',
                  k === index ? 'w-8 bg-cyan' : 'w-3 bg-foreground/30 hover:bg-foreground/60'
                )}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
