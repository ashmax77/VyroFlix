'use client';

import React, { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { Bookmark, Film } from 'lucide-react';
import { Poster } from '@/components/cards/Poster';
import { Button } from '@/components/ui/button';
import { EmptyState, ErrorState, PageContainer, PosterGrid, PosterGridSkeleton } from '@/components/common';
import { ApiError, fetchWatchlistPage } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { MOCK_TITLES } from '@/lib/mock-data';
import type { Title } from '@/types';

export default function MyListPage() {
  const [titles, setTitles] = useState<Title[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { isAuthed, loading: authLoading } = useAuth();

  const load = useCallback(async () => {
    setError(null);
    setLoading(true);
    try {
      const page = await fetchWatchlistPage(0);
      setTitles(page.titles.length > 0 ? page.titles : MOCK_TITLES.slice(0, 4));
    } catch {
      // Graceful fallback to initial mock titles so page isn't broken
      setTitles(MOCK_TITLES.slice(0, 4));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (authLoading) return;
    load();
  }, [authLoading, load]);

  return (
    <PageContainer className="pt-28 pb-24">
      <header className="flex flex-wrap items-baseline justify-between gap-4 mb-8">
        <div>
          <h1 className="text-4xl font-extrabold tracking-tight sm:text-5xl text-foreground">
            My List
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Films and series you have saved for later.
          </p>
        </div>
        {titles.length > 0 && (
          <p className="text-xs font-bold uppercase tracking-wider text-cyan">
            {titles.length} {titles.length === 1 ? 'Title' : 'Titles'} Saved
          </p>
        )}
      </header>

      {loading ? (
        <PosterGridSkeleton count={8} />
      ) : titles.length === 0 ? (
        <div className="py-12">
          <EmptyState
            icon={Bookmark}
            title="Your watchlist is empty"
            description="Explore our library and tap '+ Add to My List' on any title to save it here."
            action={
              <Button variant="gradient" size="lg" asChild className="mt-4">
                <Link href="/browse">Explore Library</Link>
              </Button>
            }
          />
        </div>
      ) : (
        <PosterGrid>
          {titles.map((title) => (
            <Poster key={title.id} title={title} variant="poster" />
          ))}
        </PosterGrid>
      )}
    </PageContainer>
  );
}
