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
    <section aria-roledescription="carousel" aria-label="Featured" className="relative h-[min(72vh,720px)] min-h-[420px] w-full overflow-hidden bg-background">
      {titleList.map((t, k) => (
        <div
          key={t.id}
          aria-hidden={k !== index}
          className={cn('absolute inset-0 transition-opacity duration-1000', k === index ? 'opacity-100' : 'pointer-events-none opacity-0')}
        >
          {(t.bannerUrl || t.posterUrl) && (
            <Image
              src={t.bannerUrl || t.posterUrl}
              alt=""
              fill
              priority={k === 0}
              sizes="100vw"
              className="object-cover object-[center_20%]"
            />
          )}
        </div>
      ))}

      {/* One scrim, for legibility of the copy only. */}
      <div className="absolute inset-0 bg-gradient-to-r from-background via-background/70 to-background/10" aria-hidden />
      <div className="absolute inset-0 bg-gradient-to-t from-background via-transparent to-transparent" aria-hidden />

      <div key={active.id} className="animate-page-in relative mx-auto flex h-full max-w-container flex-col justify-end px-4 pb-14 sm:px-6 lg:px-10">
        <div className="max-w-xl">
          <h1 className="font-heading text-4xl font-semibold leading-[1.02] tracking-tight text-foreground sm:text-6xl">
            {active.title}
          </h1>

          {facts.length > 0 && (
            <p className="mt-4 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
              {facts.map((f, i) => (
                <React.Fragment key={i}>
                  {i > 0 && <span aria-hidden>·</span>}
                  <span>{f}</span>
                </React.Fragment>
              ))}
            </p>
          )}

          {active.synopsis && (
            <p className="mt-4 line-clamp-3 text-base leading-relaxed text-foreground/85 sm:text-lg">{active.synopsis}</p>
          )}

          <div className="mt-7 flex flex-wrap items-center gap-3">
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
        </div>

        {titleList.length > 1 && (
          <div className="mt-10 flex items-center gap-3">
            <button
              type="button"
              onClick={() => setPaused((p) => !p)}
              aria-label={paused ? 'Resume rotation' : 'Pause rotation'}
              className="grid h-8 w-8 place-items-center rounded-card border border-border text-muted-foreground hover:text-foreground"
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
                  className={cn('h-1 rounded-full transition-all', k === index ? 'w-8 bg-primary' : 'w-4 bg-foreground/30 hover:bg-foreground/60')}
                />
              ))}
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
