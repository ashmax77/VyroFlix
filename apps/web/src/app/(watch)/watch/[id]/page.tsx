'use client';

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useParams, useSearchParams } from 'next/navigation';
import { ArrowLeft } from 'lucide-react';
import VideoPlayer from '@/components/VideoPlayer';
import { Button } from '@/components/ui/button';
import { EmptyState, ErrorState, Spinner } from '@/components/common';
import { ApiError, fetchTitle } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
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
  // The player opens a playback session as soon as it mounts, so it must not mount before the session is restored.
  const { loading: authLoading } = useAuth();

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

  const allEpisodes = useMemo(() => {
    if (!title || title.type !== 'SERIES') return [];
    return (title.seasons ?? []).flatMap((season) => season.episodes ?? []);
  }, [title]);

  const episode = useMemo(() => {
    if (!title || title.type !== 'SERIES') return null;
    return allEpisodes.find((ep) => ep.id === episodeId) ?? allEpisodes[0] ?? null;
  }, [title, episodeId, allEpisodes]);

  const nextEpisode = useMemo(() => {
    if (!episode) return undefined;
    const idx = allEpisodes.findIndex((e) => e.id === episode.id);
    return idx >= 0 ? allEpisodes[idx + 1] : undefined;
  }, [episode, allEpisodes]);

  if (loading || authLoading) {
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
            <Button variant="gradient" asChild>
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
      <VideoPlayer
        key={episode?.id ?? title.id}
        title={title}
        contentId={episode?.id ?? title.id}
        targetType={episode ? 'EPISODE' : 'MOVIE'}
        episodeTitle={episode?.name}
        nextEpisodeId={nextEpisode?.id}
      />
    </div>
  );
}
