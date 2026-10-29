'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/common';
import { useAuth } from '@/lib/auth-context';

// Concurrent-stream limits per plan, as enforced by Streaming-Service. Plans are set by an administrator for now.
const PLANS = [
  { name: 'Free', streams: 1, note: 'Watch on one screen at a time.' },
  { name: 'Basic', streams: 2, note: 'Two screens at once.' },
  { name: 'Premium', streams: 4, note: 'Up to four screens at once.' },
] as const;

export default function LandingPage() {
  const router = useRouter();
  const { isAuthed, loading } = useAuth();

  useEffect(() => {
    if (!loading && isAuthed) router.replace('/browse');
  }, [isAuthed, loading, router]);

  if (loading || isAuthed) {
    return <Spinner label="Opening VyroFlix…" />;
  }

  return (
    <div>
      <section className="mx-auto grid max-w-container gap-10 px-4 pb-16 pt-12 sm:px-6 md:pt-20 lg:grid-cols-[1.1fr_1fr] lg:items-end lg:px-10">
        <div>
          <h1 className="font-heading text-5xl font-semibold leading-[1.02] tracking-tight sm:text-7xl">
            Films and series that pick up where you left off.
          </h1>
          <p className="mt-6 max-w-xl text-lg leading-relaxed text-muted-foreground">
            Sign in to keep a list, resume anything on any screen, and get recommendations from what you have watched.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Button size="lg" asChild>
              <Link href="/register">Create an account</Link>
            </Button>
            <Button size="lg" variant="secondary" asChild>
              <Link href="/browse">Browse as guest</Link>
            </Button>
          </div>
        </div>

        <dl className="grid grid-cols-1 gap-6 border-t border-border pt-6 sm:grid-cols-3 lg:grid-cols-1 lg:border-l lg:border-t-0 lg:pl-10 lg:pt-0">
          {PLANS.map((plan) => (
            <div key={plan.name}>
              <dt className="text-sm text-subtle">{plan.name}</dt>
              <dd className="mt-1 font-heading text-3xl text-foreground">
                {plan.streams} {plan.streams === 1 ? 'screen' : 'screens'}
              </dd>
              <dd className="mt-1 text-sm text-muted-foreground">{plan.note}</dd>
            </div>
          ))}
        </dl>
      </section>
    </div>
  );
}
