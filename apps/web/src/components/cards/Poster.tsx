'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Check, ChevronDown, Play, Plus } from 'lucide-react';
import { ApiError } from '@/lib/api';
import { useWatchlist } from '@/lib/watchlist-context';
import { MaturityBadge } from '@/components/common';
import { cn } from '@/lib/utils';
import type { Title } from '@/types';

export interface PosterProgress {
  percent: number;
  minutesLeft: number;
}

export type CardVariant = 'poster' | 'art';

interface PosterProps {
  title: Title;
  className?: string;
  variant?: CardVariant;
  progress?: PosterProgress;
  rank?: number;
  highlight?: string;
  sizes?: string;
}

export function Poster({
  title,
  className,
  variant = 'poster',
  progress,
  highlight,
  sizes = '(min-width: 1024px) 240px, 180px',
}: PosterProps) {
  const router = useRouter();
  const { has, toggle, ready } = useWatchlist();
  const [imageFailed, setImageFailed] = useState(false);
  const [busy, setBusy] = useState(false);

  const inList = has(title.id);
  const seasons = title.seasons?.length ?? 1;
  const meta = title.type === 'SERIES' ? `${seasons} Season${seasons > 1 ? 's' : ''}` : `${title.releaseYear}`;
  const imageSrc = variant === 'art' && title.bannerUrl ? title.bannerUrl : title.posterUrl;

  const onPlay = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    router.push(`/watch/${title.id}`);
  };

  const onToggleList = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (busy) return;
    setBusy(true);
    try {
      await toggle(title.id);
    } catch {
      // ignore
    } finally {
      setBusy(false);
    }
  };

  const hoverPanel = (
    <div className="pointer-events-none absolute inset-x-0 top-full z-30 -mt-1 hidden scale-95 rounded-b-2xl bg-surface-elevated/95 p-3.5 opacity-0 shadow-card backdrop-blur-xl transition-all duration-300 group-hover:pointer-events-auto group-hover:scale-100 group-hover:opacity-100 md:block border border-t-0 border-border">
      <div className="flex items-center gap-2">
        <button
          type="button"
          aria-label={`Play ${title.title}`}
          onClick={onPlay}
          className="grid h-8 w-8 place-items-center rounded-full bg-white text-black hover:bg-white/90 shadow-md transition-transform active:scale-95"
        >
          <Play className="h-4 w-4 fill-current ml-0.5" />
        </button>
        {ready && (
          <button
            type="button"
            aria-label={inList ? 'Remove from My List' : 'Add to My List'}
            onClick={onToggleList}
            className="grid h-8 w-8 place-items-center rounded-full border border-border bg-surface hover:border-foreground transition-all active:scale-95"
          >
            {inList ? <Check className="h-4 w-4 text-cyan" /> : <Plus className="h-4 w-4" />}
          </button>
        )}
        <Link
          href={`/title/${title.id}`}
          aria-label="More info"
          className="ml-auto grid h-8 w-8 place-items-center rounded-full border border-border bg-surface hover:border-foreground transition-colors"
        >
          <ChevronDown className="h-4 w-4" />
        </Link>
      </div>
      <div className="mt-2.5 flex items-center gap-2 text-xs text-muted-foreground">
        <MaturityBadge rating={title.maturityRating} />
        <span className="font-medium">{meta}</span>
      </div>
    </div>
  );

  return (
    <article className={cn('group relative select-none', className)}>
      <Link
        href={`/title/${title.id}`}
        aria-label={title.title}
        className={cn(
          'block overflow-hidden rounded-2xl bg-surface shadow-card transition-all duration-300 group-hover:scale-[1.04] group-hover:shadow-glow group-focus-within:scale-[1.04]',
          variant === 'art' ? 'aspect-video' : 'aspect-[2/3]'
        )}
      >
        <div className="relative h-full w-full">
          {imageSrc && !imageFailed ? (
            <Image
              src={imageSrc}
              alt=""
              fill
              sizes={sizes}
              className="object-cover transition-transform duration-500 group-hover:scale-[1.06]"
              onError={() => setImageFailed(true)}
            />
          ) : (
            <div className="absolute inset-0 grid place-items-center p-4 text-center font-display text-lg font-bold text-muted-foreground bg-surface">
              {title.title}
            </div>
          )}

          <div className="absolute inset-0 bg-fade-bottom opacity-75" />

          {/* Title label overlay */}
          <div className="absolute inset-x-3 bottom-3 z-10">
            <p className="line-clamp-1 font-display text-sm font-bold text-foreground drop-shadow-md">
              {highlight ? highlightText(title.title, highlight) : title.title}
            </p>
            {variant === 'art' && (
              <p className="line-clamp-1 text-xs text-muted-foreground/90 font-medium">{title.synopsis}</p>
            )}
          </div>

          {/* Viewing Progress */}
          {progress && (
            <div className="absolute inset-x-0 bottom-0 z-20 h-1 bg-white/20">
              <div className="h-full bg-cyan shadow-glow" style={{ width: `${Math.min(100, progress.percent)}%` }} />
            </div>
          )}
        </div>
      </Link>

      {hoverPanel}
    </article>
  );
}

function highlightText(text: string, term: string) {
  const i = text.toLowerCase().indexOf(term.toLowerCase());
  if (i < 0 || !term) return text;
  return (
    <>
      {text.slice(0, i)}
      <mark className="rounded bg-cyan/30 px-0.5 text-foreground">{text.slice(i, i + term.length)}</mark>
      {text.slice(i + term.length)}
    </>
  );
}
