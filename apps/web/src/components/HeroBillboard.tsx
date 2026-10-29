'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { Check, Info, Play, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/common';
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
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (titleList.length <= 1) return;
    const id = setInterval(() => setIndex((i) => (i + 1) % titleList.length), ROTATE_MS);
    return () => clearInterval(id);
  }, [titleList.length]);

  if (loading || titleList.length === 0) {
    return (
      <div className="relative h-[56vh] min-h-[380px] sm:h-[68vh] sm:min-h-[460px] lg:h-[78vh] lg:min-h-[520px]">
        <Skeleton className="absolute inset-0 rounded-none" />
      </div>
    );
  }

  const active = titleList[Math.min(index, titleList.length - 1)];
  const inList = has(active.id);

  const onToggleList = async () => {
    if (busy || !ready) return;
    setBusy(true);
    try {
      await toggle(active.id);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section
      className="relative h-[56vh] min-h-[380px] overflow-hidden sm:h-[68vh] sm:min-h-[460px] lg:h-[78vh] lg:min-h-[540px]"
      aria-roledescription="carousel"
      aria-label="Featured titles"
    >
      {/* Background Backdrops with crossfade transition */}
      {titleList.map((t, k) => {
        const bgUrl = t.bannerUrl || t.posterUrl;
        return (
          <div
            key={t.id}
            className={cn(
              'absolute inset-0 h-full w-full transition-opacity duration-1000 ease-in-out',
              k === index ? 'opacity-100 z-0' : 'opacity-0 -z-10'
            )}
          >
            {bgUrl && (
              <Image
                src={bgUrl}
                alt=""
                fill
                priority={k === 0}
                sizes="100vw"
                className="object-cover object-center"
              />
            )}
          </div>
        );
      })}

      {/* Cinematic Gradient Overlays */}
      <div className="absolute inset-0 bg-fade-left z-10" />
      <div className="absolute inset-0 bg-fade-bottom z-10" />
      <div className="absolute inset-0 bg-background/20 z-10 pointer-events-none" />

      {/* Hero Content Stage */}
      <div className="relative z-20 mx-auto flex h-full max-w-[1800px] flex-col justify-end px-4 pb-14 sm:px-8 sm:pb-20 lg:px-12 lg:pb-28">
        <div key={active.id} className="animate-page-in max-w-2xl">
          <h1 className="text-3xl font-extrabold tracking-tight sm:text-5xl lg:text-7xl drop-shadow-lg text-foreground">
            {active.title}
          </h1>

          <p className="mt-3 line-clamp-2 max-w-lg text-sm text-foreground/90 sm:mt-5 sm:line-clamp-3 sm:text-lg drop-shadow">
            {active.synopsis}
          </p>

          <div className="mt-6 flex flex-wrap items-center gap-3">
            <Button
              variant="light"
              size="lg"
              className="h-11 px-7 sm:h-12 sm:px-8 text-base font-bold shadow-lg"
              onClick={() => router.push(`/watch/${active.id}`)}
            >
              <Play className="fill-current mr-1 h-5 w-5" /> Play
            </Button>

            <Button
              variant="glass"
              size="lg"
              className="h-11 px-7 sm:h-12 sm:px-8 text-base shadow-glow"
              asChild
            >
              <Link href={`/title/${active.id}`}>
                <Info className="mr-1 h-5 w-5" /> More Info
              </Link>
            </Button>

            {ready && (
              <Button
                variant="glass"
                size="lg"
                onClick={onToggleList}
                disabled={busy}
                className="h-11 px-5 sm:h-12 sm:px-6"
                aria-label={inList ? 'Remove from My List' : 'Add to My List'}
              >
                {inList ? <Check className="h-5 w-5 text-cyan" /> : <Plus className="h-5 w-5" />}
                <span className="hidden sm:inline ml-1">{inList ? 'In List' : 'My List'}</span>
              </Button>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
