'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { Check, Info, Pause, Play, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/common';
import { ApiError } from '@/lib/api';
import { useWatchlist } from '@/lib/watchlist-context';
import { cn } from '@/lib/utils';
import type { Title } from '@/types';

interface HeroBillboardProps {
  title?: Title;
  titles?: Title[];
  loading?: boolean;
}

const ROTATE_MS = 8000;

export default function HeroBillboard({ title, titles, loading }: HeroBillboardProps) {
  const router = useRouter();
  const { has, toggle, ready } = useWatchlist();
  const titleList: Title[] = titles?.length ? titles : title ? [title] : [];
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (paused || titleList.length <= 1) return;
    const id = setInterval(() => setIndex((i) => (i + 1) % titleList.length), ROTATE_MS);
    return () => clearInterval(id);
  }, [paused, titleList.length]);

  if (loading || titleList.length === 0) {
    return (
      <div className="h-[min(72vh,720px)] min-h-[420px] w-full">
        <Skeleton className="h-full w-full rounded-none" />
      </div>
    );
  }

  const active = titleList[Math.min(index, titleList.length - 1)];
  const inList = has(active.id);
  const runtime = active.type === 'SERIES'
    ? `${active.seasons?.length || 1} season${(active.seasons?.length || 1) > 1 ? 's' : ''}`
    : null;
  const facts = [active.releaseYear || null, active.maturityRating || null, runtime].filter(Boolean) as string[];

  const onToggleList = async () => {
    if (busy) return;
    setBusy(true);
    setMessage(null);
    try {
      await toggle(active.id);
    } catch (err) {
      if (err instanceof ApiError && err.isUnauthorized) setMessage('Sign in to use My list.');
      else setMessage("Couldn't update My list.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section aria-roledescription="carousel" aria-label="Featured" className="mx-auto max-w-container px-4 pb-6 pt-6 sm:px-6 lg:px-10 lg:pt-10">
      <div className="grid gap-8 lg:grid-cols-[1fr_1.1fr] lg:items-end lg:gap-14">
        <div key={active.id} className="animate-page-in order-2 flex flex-col lg:order-1">
          <h1 className="text-[3.5rem] leading-[0.95] text-foreground sm:text-7xl lg:text-[6.5rem]">
            {active.title}
          </h1>

          {facts.length > 0 && (
            <p className="mt-6 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted-foreground">
              {facts.map((f, i) => (
                <React.Fragment key={i}>
                  {i > 0 && <span className="h-3 w-px bg-border" aria-hidden />}
                  <span>{f}</span>
                </React.Fragment>
              ))}
            </p>
          )}

          {active.synopsis && (
            <p className="mt-5 max-w-md line-clamp-3 text-base leading-relaxed text-foreground/80 sm:text-lg">{active.synopsis}</p>
          )}

          <div className="mt-8 flex flex-wrap items-center gap-3">
            <Button size="lg" onClick={() => router.push(`/watch/${active.id}`)} className="gap-2">
              <Play className="h-4 w-4 fill-current" aria-hidden /> Play
            </Button>
            <Button
              variant="secondary"
              size="lg"
              onClick={onToggleList}
              disabled={busy || !ready}
              aria-label={inList ? `Remove ${active.title} from My list` : `Add ${active.title} to My list`}
              className="gap-2"
            >
              {inList ? <Check className="h-4 w-4 text-primary" aria-hidden /> : <Plus className="h-4 w-4" aria-hidden />}
              <span>{inList ? 'In My list' : 'My list'}</span>
            </Button>
            <Button variant="ghost" size="lg" asChild className="gap-2">
              <Link href={`/title/${active.id}`}>
                <Info className="h-4 w-4" aria-hidden /> More info
              </Link>
            </Button>
          </div>
          {message && <p role="alert" className="mt-3 text-sm text-danger">{message}</p>}

          {titleList.length > 1 && (
            <div className="mt-10 flex items-center gap-3">
              <button
                type="button"
                onClick={() => setPaused((p) => !p)}
                aria-label={paused ? 'Resume rotation' : 'Pause rotation'}
                className="grid h-8 w-8 place-items-center rounded-card border border-border text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              >
                {paused ? <Play className="ml-0.5 h-3.5 w-3.5" aria-hidden /> : <Pause className="h-3.5 w-3.5" aria-hidden />}
              </button>
              <div className="flex items-center gap-2" role="group" aria-label="Choose a featured title">
                {titleList.map((t, k) => (
                  <button
                    key={t.id}
                    type="button"
                    onClick={() => setIndex(k)}
                    aria-label={`Show ${t.title}`}
                    aria-current={k === index}
                    className={cn('h-1 rounded-full transition-all', k === index ? 'w-10 bg-primary' : 'w-5 bg-foreground/25 hover:bg-foreground/50')}
                  />
                ))}
              </div>
            </div>
          )}
        </div>

        {/* The backdrop sits in a frame, like a picture on a projection screen, not behind the copy. */}
        <div className="relative order-1 aspect-[16/9] w-full overflow-hidden rounded-sm bg-screen lg:order-2">
          {titleList.map((t, k) =>
            t.bannerUrl || t.posterUrl ? (
              <div
                key={t.id}
                aria-hidden={k !== index}
                className={cn('absolute inset-0 transition-opacity duration-1000', k === index ? 'opacity-100' : 'pointer-events-none opacity-0')}
              >
                <Image
                  src={t.bannerUrl || t.posterUrl}
                  alt=""
                  fill
                  priority={k === 0}
                  sizes="(min-width: 1024px) 50vw, 100vw"
                  className="object-cover"
                />
              </div>
            ) : null,
          )}
          <div className="absolute inset-0 ring-1 ring-inset ring-border" aria-hidden />
        </div>
      </div>
    </section>
  );
}
