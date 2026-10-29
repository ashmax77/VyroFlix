'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Check, Play, Plus } from 'lucide-react';
import { ApiError } from '@/lib/api';
import { useWatchlist } from '@/lib/watchlist-context';
import { cn } from '@/lib/utils';
import type { Title } from '@/types';

export interface PosterProgress {
  /** 0–100 */
  percent: number;
  minutesLeft: number;
}

interface PosterProps {
  title: Title;
  className?: string;
  progress?: PosterProgress;
  /** Shown as a large outline numeral beside the poster in Top 10 rails. */
  rank?: number;
  /** Highlights this substring in the title, for search results. */
  highlight?: string;
  sizes?: string;
}

/**
 * Film-gate poster: a hairline frame, a perforated strip for progress, and actions that are always
 * reachable on touch and appear on hover or focus for pointer users.
 */
export function Poster({ title, className, progress, highlight, sizes = '(min-width: 1024px) 192px, 160px' }: PosterProps) {
  const router = useRouter();
  const { has, toggle, ready } = useWatchlist();
  const [imageFailed, setImageFailed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const inList = has(title.id);
  const meta = [
    title.releaseYear,
    title.type === 'SERIES' ? `${title.seasons?.length || 1} season${(title.seasons?.length || 1) > 1 ? 's' : ''}` : null,
  ].filter(Boolean);

  const onPlay = (e: React.MouseEvent) => {
    e.preventDefault();
    router.push(`/watch/${title.id}`);
  };

  const onToggleList = async (e: React.MouseEvent) => {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setActionError(null);
    try {
      await toggle(title.id);
    } catch (err) {
      setActionError(err instanceof ApiError && err.isUnauthorized ? "Sign in to use My list." : "Couldn't update My list.");
    } finally {
      setBusy(false);
    }
  };

  const actionClass =
    'grid h-9 w-9 place-items-center rounded-full bg-background/85 text-foreground backdrop-blur-sm ' +
    'transition-colors hover:bg-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary';

  return (
    <article className={cn('group relative', className)}>
      <div className="relative aspect-[2/3] overflow-hidden rounded-[2px] bg-surface ring-1 ring-inset ring-white/10">
        <Link href={`/title/${title.id}`} aria-label={title.title} className="absolute inset-0 focus-visible:outline-none">
          {title.posterUrl && !imageFailed ? (
            <Image
              src={title.posterUrl}
              alt=""
              fill
              sizes={sizes}
              className="object-cover transition-transform duration-500 group-hover:scale-[1.03]"
              onError={() => setImageFailed(true)}
            />
          ) : (
            <span className="absolute inset-0 grid place-items-center p-4 text-center font-heading text-2xl text-muted-foreground">
              {title.title}
            </span>
          )}
        </Link>

        {progress && (
          <div className="pointer-events-none absolute inset-x-0 bottom-0 bg-background/80 px-2 pb-2 pt-1.5" aria-hidden>
            {/* Perforated film strip: the sprocket holes are the track, the lit part is progress. */}
            <div
              className="relative h-2 w-full overflow-hidden rounded-[1px] bg-screen"
              style={{
                backgroundImage: 'radial-gradient(circle, rgb(var(--booth)) 1.2px, transparent 1.6px)',
                backgroundSize: '7px 100%',
                backgroundRepeat: 'repeat-x',
                backgroundPosition: 'center',
              }}
            >
              <div className="absolute inset-y-0 left-0 bg-primary" style={{ width: `${Math.min(100, progress.percent)}%` }} />
            </div>
            <p className="mt-1 text-[11px] text-muted-foreground">{progress.minutesLeft} min left</p>
          </div>
        )}

        {/* Actions: always visible on touch; pointer users get them on hover or keyboard focus. */}
        <div className="absolute right-2 top-2 flex gap-1.5 opacity-100 transition-opacity md:opacity-0 md:group-focus-within:opacity-100 md:group-hover:opacity-100">
          <button type="button" onClick={onPlay} aria-label={`Play ${title.title}`} className={actionClass}>
            <Play className="ml-0.5 h-4 w-4 fill-current" aria-hidden />
          </button>
          {ready && (
            <button
              type="button"
              onClick={onToggleList}
              disabled={busy}
              aria-label={inList ? `Remove ${title.title} from My list` : `Add ${title.title} to My list`}
              className={cn(actionClass, inList && 'text-primary')}
            >
              {inList ? <Check className="h-4 w-4" aria-hidden /> : <Plus className="h-4 w-4" aria-hidden />}
            </button>
          )}
        </div>
      </div>

      <div className="mt-2.5 min-w-0">
        <Link href={`/title/${title.id}`} className="block truncate text-sm font-medium text-foreground hover:text-primary">
          {highlight ? highlightText(title.title, highlight) : title.title}
        </Link>
        {meta.length > 0 && (
          <p className="mt-0.5 flex min-w-0 items-center gap-1.5 truncate text-xs text-subtle">
            {meta.map((m, i) => (
              <React.Fragment key={i}>
                {i > 0 && <span aria-hidden>·</span>}
                <span>{m}</span>
              </React.Fragment>
            ))}
          </p>
        )}
        {actionError && (
          <p role="alert" className="mt-1 text-xs text-danger">
            {actionError}
          </p>
        )}
      </div>
    </article>
  );
}

function highlightText(text: string, term: string) {
  const i = text.toLowerCase().indexOf(term.toLowerCase());
  if (i < 0 || !term) return text;
  return (
    <>
      {text.slice(0, i)}
      <mark className="bg-transparent font-medium text-primary">{text.slice(i, i + term.length)}</mark>
      {text.slice(i + term.length)}
    </>
  );
}
