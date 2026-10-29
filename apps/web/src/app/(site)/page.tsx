'use client';

import React, { useEffect, useState } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/common';
import { fetchHomeFeed } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import type { Title } from '@/types';

// Concurrent-stream limits per plan, as enforced by Streaming-Service. Plans are set by an administrator for now.
const PLANS = [
  { name: 'Free', streams: 1, note: 'One screen at a time.' },
  { name: 'Basic', streams: 2, note: 'Two screens at once.' },
  { name: 'Premium', streams: 4, note: 'Four screens at once.' },
] as const;

export default function LandingPage() {
  const router = useRouter();
  const { isAuthed, loading } = useAuth();
  const [posters, setPosters] = useState<Title[]>([]);

  useEffect(() => {
    if (!loading && isAuthed) router.replace('/browse');
  }, [isAuthed, loading, router]);

  // Real posters from the catalog. If the backend is asleep the page still reads fine without them.
  useEffect(() => {
    fetchHomeFeed()
      .then((feed) => {
        const titles = [feed.featuredTitle, ...feed.rails.flatMap((rail) => rail.items)].filter(
          (t): t is Title => Boolean(t?.posterUrl),
        );
        setPosters(titles.slice(0, 3));
      })
      .catch(() => setPosters([]));
  }, []);

  if (loading || isAuthed) {
    return <Spinner label="Opening VyroFlix…" />;
  }

  return (
    <div>
      <section className="mx-auto grid max-w-container gap-12 px-4 pb-20 pt-10 sm:px-6 md:pt-16 lg:grid-cols-[1.25fr_1fr] lg:items-center lg:gap-16 lg:px-10">
        <div>
          <h1 className="text-[3.25rem] leading-[0.98] sm:text-7xl lg:text-[7.5rem]">
            Pick up where you left off.
          </h1>
          <p className="mt-8 max-w-md text-lg leading-relaxed text-muted-foreground">
            Keep a list, resume any film on any screen, and get picks from what you have watched.
          </p>
          <div className="mt-10 flex flex-wrap gap-3">
            <Button size="lg" asChild>
              <Link href="/register">Create an account</Link>
            </Button>
            <Button size="lg" variant="ghost" asChild>
              <Link href="/browse">Browse as guest</Link>
            </Button>
          </div>
        </div>

        {posters.length > 0 && (
          <div className="flex items-end justify-center gap-4 sm:gap-6" aria-hidden>
            {posters.map((title, i) => (
              <div
                key={title.id}
                className={`relative w-[30%] max-w-[200px] overflow-hidden rounded-sm bg-screen shadow-2xl shadow-black/60 ${
                  i === 1 ? '-translate-y-10' : ''
                }`}
                style={{ aspectRatio: '2 / 3' }}
              >
                <Image src={title.posterUrl} alt="" fill sizes="200px" className="object-cover" />
              </div>
            ))}
          </div>
        )}
      </section>

      <section aria-labelledby="plans-heading" className="border-y border-border">
        <div className="mx-auto grid max-w-container gap-0 px-4 sm:px-6 lg:px-10 md:grid-cols-3">
          <h2 id="plans-heading" className="sr-only">Plans</h2>
          {PLANS.map((plan, i) => (
            <div
              key={plan.name}
              className={`py-10 md:px-8 ${i > 0 ? 'border-t border-border md:border-l md:border-t-0' : ''} ${i === 0 ? 'md:pl-0' : ''}`}
            >
              <p className="font-heading text-3xl text-foreground">{plan.name}</p>
              <p className="mt-3 font-heading text-6xl text-foreground">{plan.streams}</p>
              <p className="mt-2 text-sm text-muted-foreground">{plan.note}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
