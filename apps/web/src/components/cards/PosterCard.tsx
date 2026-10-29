'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Check, ChevronDown, Play, Plus, ThumbsUp } from 'lucide-react';
import { MaturityBadge } from '@/components/common';
import { ApiError, redirectToLogin, toggleWatchlist, checkWatchlist } from '@/lib/api';
import { cn } from '@/lib/utils';
import type { Title } from '@/types';

export function PosterCard({
  title,
  className,
  highlight,
  rank,
}: {
  title: Title;
  className?: string;
  highlight?: string;
  rank?: number;
}) {
  const router = useRouter();
  const [inList, setInList] = useState(false);
  const [isToggling, setIsToggling] = useState(false);
  const [liked, setLiked] = useState(false);

  useEffect(() => {
    let active = true;
    checkWatchlist(title.id).then((val) => {
      if (active) setInList(val);
    });
    return () => {
      active = false;
    };
  }, [title.id]);

  const handleToggleList = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (isToggling) return;
    setIsToggling(true);
    try {
      const res = await toggleWatchlist(title.id, inList);
      setInList(res);
    } catch (err) {
      // State is only updated on success, so there is nothing to roll back.
      if (err instanceof ApiError && err.isUnauthorized) redirectToLogin();
    } finally {
      setIsToggling(false);
    }
  };

  const handlePlay = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    router.push(`/watch/${title.id}`);
  };

  const matchPercent = 94 + (title.id.charCodeAt(0) % 6);
  const genres = title.tags?.length ? title.tags : ['Sci-Fi', 'Action'];

  return (
    <div className={cn('group/card relative', className)}>
      <Link
        href={`/title/${title.id}`}
        className="block overflow-hidden rounded-xl bg-surface shadow-card transition-all duration-300 group-hover/card:scale-[1.05] group-hover/card: focus-visible:scale-[1.05]"
        aria-label={title.title}
      >
        <div className="relative aspect-[2/3] w-full">
          <img
            src={title.posterUrl}
            alt={title.title}
            loading="lazy"
            className="h-full w-full object-cover transition-transform duration-500 group-hover/card:scale-105"
          />
          <div className="absolute inset-0 bg-fade-bottom opacity-80" />
          <p className="absolute inset-x-3 bottom-3 line-clamp-2 font-display text-sm font-semibold leading-tight text-foreground drop-shadow-md">
            {highlight ? highlightText(title.title, highlight) : title.title}
          </p>
        </div>
      </Link>

      {/* Hover Mini Panel on desktop */}
      <div className="pointer-events-none absolute inset-x-0 top-full z-30 -mt-2 hidden scale-95 rounded-b-xl bg-surface-elevated p-3 opacity-0 shadow-card transition-all duration-300 group-hover/card:pointer-events-auto group-hover/card:scale-[1.05] group-hover/card:opacity-100 md:block border border-border/80 border-t-0">
        <div className="flex items-center gap-1.5">
          <button
            aria-label={`Play ${title.title}`}
            onClick={handlePlay}
            className="grid h-8 w-8 place-items-center rounded-full bg-foreground text-background hover:bg-foreground/80 transition-colors cursor-pointer"
          >
            <Play className="h-4 w-4 fill-current ml-0.5" />
          </button>
          <button
            aria-label={inList ? 'Remove from My List' : 'Add to My List'}
            onClick={handleToggleList}
            disabled={isToggling}
            className={cn(
              'grid h-8 w-8 place-items-center rounded-full border border-border hover:border-foreground transition-colors cursor-pointer',
              inList && 'border-primary bg-primary/15 text-primary'
            )}
          >
            {inList ? <Check className="h-4 w-4 text-primary" /> : <Plus className="h-4 w-4 text-foreground" />}
          </button>
          <button
            aria-label="Like"
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              setLiked(!liked);
            }}
            className={cn(
              'grid h-8 w-8 place-items-center rounded-full border border-border hover:border-foreground transition-colors cursor-pointer',
              liked && 'border-primary bg-primary/20 text-primary-violet'
            )}
          >
            <ThumbsUp className="h-3.5 w-3.5" />
          </button>
          <Link
            href={`/title/${title.id}`}
            aria-label="More details"
            className="ml-auto grid h-8 w-8 place-items-center rounded-full border border-border hover:border-foreground transition-colors"
          >
            <ChevronDown className="h-4 w-4 text-foreground" />
          </Link>
        </div>

        <div className="mt-2.5 flex flex-wrap items-center gap-2 text-xs">
          <span className="font-semibold text-success">{matchPercent}% match</span>
          <MaturityBadge rating={title.maturityRating || 'PG-13'} />
          <span className="text-muted-foreground text-[11px]">
            {title.type === 'SERIES'
              ? `${title.seasons?.length ?? 1} Season${(title.seasons?.length ?? 1) > 1 ? 's' : ''}`
              : `${title.durationMinutes || 120}m`}
          </span>
        </div>

        <p className="mt-1.5 truncate text-[11px] text-muted-foreground">
          {genres.join(' • ')}
        </p>
      </div>
    </div>
  );
}

function highlightText(text: string, term: string) {
  const i = text.toLowerCase().indexOf(term.toLowerCase());
  if (i < 0 || !term) return text;
  return (
    <>
      {text.slice(0, i)}
      <mark className="rounded bg-primary/30 px-0.5 text-foreground">{text.slice(i, i + term.length)}</mark>
      {text.slice(i + term.length)}
    </>
  );
}
