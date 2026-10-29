'use client';

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useParams, useSearchParams } from 'next/navigation';
import { ArrowLeft } from 'lucide-react';
import VideoPlayer from '@/components/VideoPlayer';
import { Button } from '@/components/ui/button';
import { EmptyState, ErrorState, Spinner } from '@/components/common';
import { ApiError, fetchTitle } from '@/lib/api';
import { SLOW_LOAD_MESSAGE, useSlowHint } from '@/lib/hooks';
import type { Title } from '@/types';

/** Full-bleed stage for the player: no site chrome, one way back. */
function Stage({ children }: { children: React.ReactNode }) {
  return <div className="flex h-screen w-full flex-col items-center justify-center gap-4 bg-black px-6 text-center">{children}</div>;
}

export default function WatchPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const titleId = params.id as string;
  const episodeId = searchParams.get('episode');

  const [title, setTitle] = useState<Title | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const slow = useSlowHint(loading);

  const load = useCallback(() => {
    if (!titleId) return;
    setError(null);
    setLoading(true);
    fetchTitle(titleId)
      .then(setTitle)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Couldn't load this title. Try again."))
      .finally(() => setLoading(false));
  }, [titleId]);

  useEffect(load, [load]);

  // Series: play the requested episode; fall back to the first one. Movies play the title itself.
  const episode = useMemo(() => {
    if (!title || title.type !== 'SERIES') return null;
    const episodes = (title.seasons ?? []).flatMap((season) => season.episodes ?? []);
    return episodes.find((ep) => ep.id === episodeId) ?? episodes[0] ?? null;
  }, [title, episodeId]);

  if (loading) {
    return (
      <Stage>
        <Spinner label={slow ? SLOW_LOAD_MESSAGE : 'Loading the player…'} />
      </Stage>
    );
  }

  if (error) {
    return (
      <Stage>
        <div className="w-full max-w-lg">
          <ErrorState message={error} onRetry={load} />
        </div>
      </Stage>
    );
  }

  if (!title) {
    return (
      <Stage>
        <EmptyState
          title="This title isn't available"
          description="It may have been unpublished or the link is out of date."
          action={
            <Button variant="secondary" asChild>
              <Link href="/browse">Back to browse</Link>
            </Button>
          }
        />
      </Stage>
    );
  }

  if (title.type === 'SERIES' && !episode) {
    return (
      <Stage>
        <EmptyState
          title="No episodes yet"
          description="This series has no published episodes to play."
          action={
            <Button variant="secondary" asChild>
              <Link href={`/title/${title.id}`}>Back to details</Link>
            </Button>
          }
        />
      </Stage>
    );
  }

  return (
    <div className="relative">
      <Link
        href={`/title/${title.id}`}
        className="absolute left-4 top-4 z-50 inline-flex items-center gap-2 rounded-card bg-black/60 px-3 py-2 text-sm text-foreground backdrop-blur-sm hover:bg-black/80"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden /> Back to {title.title}
      </Link>
      <VideoPlayer
        key={episode?.id ?? title.id}
        title={title}
        contentId={episode?.id ?? title.id}
        targetType={episode ? 'EPISODE' : 'MOVIE'}
        episodeTitle={episode?.name}
      />
    </div>
  );
}
