'use client';

import React, { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { Bookmark } from 'lucide-react';
import { Poster } from '@/components/cards/Poster';
import { Button } from '@/components/ui/button';
import { EmptyState, ErrorState, PosterGridSkeleton } from '@/components/common';
import { ApiError, fetchWatchlist } from '@/lib/api';
import { SLOW_LOAD_MESSAGE, useSlowHint } from '@/lib/hooks';
import type { Title } from '@/types';

export default function MyListPage() {
  const [titles, setTitles] = useState<Title[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<{ message: string; unauthorized: boolean } | null>(null);
  const slow = useSlowHint(loading);

  const load = useCallback(async () => {
    setError(null);
    setLoading(true);
    try {
      const entries = await fetchWatchlist();
      setTitles(entries.map((e) => e.title).filter((t): t is Title => Boolean(t)));
    } catch (e) {
      const apiError = e instanceof ApiError ? e : null;
      setError({
        message: apiError?.isUnauthorized
          ? 'Your session has expired. Sign in again to see your list.'
          : "Couldn't load your list. Try again.",
        unauthorized: !!apiError?.isUnauthorized,
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) {
    return (
      <div className="mx-auto max-w-container px-4 py-10 sm:px-6 lg:px-10" aria-busy="true">
        <h1 className="mb-8 text-3xl sm:text-4xl">My list</h1>
        <PosterGridSkeleton count={10} />
        <p className="mt-6 text-sm text-muted-foreground">{slow ? SLOW_LOAD_MESSAGE : 'Loading your list…'}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="mx-auto max-w-container px-4 py-16 sm:px-6 lg:px-10">
        <ErrorState message={error.message} onRetry={error.unauthorized ? undefined : load} />
        {error.unauthorized && (
          <div className="mt-6 flex justify-center">
            <Button asChild>
              <Link href="/login?redirect=/my-list">Sign in</Link>
            </Button>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-container px-4 pb-16 sm:px-6 lg:px-10">
      <header className="flex items-baseline justify-between gap-4 pt-6">
        <h1 className="text-3xl sm:text-4xl">My list</h1>
        {titles.length > 0 && (
          <p className="text-sm text-muted-foreground">
            {titles.length} {titles.length === 1 ? 'title' : 'titles'}
          </p>
        )}
      </header>

      <div className="mt-8">
        {titles.length === 0 ? (
          <EmptyState
            icon={Bookmark}
            title="Your list is empty"
            description="Add films and series you want to watch later. They'll wait here."
            action={
              <Button variant="secondary" asChild>
                <Link href="/browse">Browse titles</Link>
              </Button>
            }
          />
        ) : (
          <ul className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
            {titles.map((title) => (
              <li key={title.id}>
                <Poster title={title} />
              </li>
            ))}
          </ul>
        )}
      </div>

    </div>
  );
}
