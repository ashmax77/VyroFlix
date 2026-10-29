'use client';

import React, { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useParams, useSearchParams } from 'next/navigation';
import VideoPlayer from '@/components/VideoPlayer';
import { ErrorState, Spinner } from '@/components/common';
import { ApiError, fetchTitle } from '@/lib/api';
import { SLOW_LOAD_MESSAGE, useSlowHint } from '@/lib/hooks';
import { Title } from '@/types';

export default function WatchPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const titleId = params.id as string;
  const episodeId = searchParams.get('episode');

  const [title, setTitle] = useState<Title | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const slow = useSlowHint(loading);

  const load = () => {
    if (!titleId) return;
    setError(null);
    setLoading(true);
    fetchTitle(titleId)
      .then(setTitle)
      .catch((e) => setError(e instanceof ApiError ? e.message : 'Could not load this title.'))
      .finally(() => setLoading(false));
  };

  useEffect(load, [titleId]);

  // Series: play the requested episode; fall back to the first one. Movies play the title itself.
  const episode = useMemo(() => {
    if (!title || title.type !== 'SERIES') return null;
    const episodes = (title.seasons ?? []).flatMap((season) => season.episodes ?? []);
    return episodes.find((ep) => ep.id === episodeId) ?? episodes[0] ?? null;
  }, [title, episodeId]);

  if (loading) {
    return (
      <div className="w-full h-screen bg-black flex items-center justify-center">
        <Spinner label={slow ? SLOW_LOAD_MESSAGE : undefined} />
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full h-screen bg-black flex items-center justify-center">
        <ErrorState message={error} onRetry={load} />
      </div>
    );
  }

  if (!title) {
    return (
      <div className="w-full h-screen bg-black flex flex-col items-center justify-center gap-4 text-muted-foreground">
        <p>Title not found.</p>
        <Link href="/browse" className="px-6 py-2.5 rounded-full bg-white text-black font-semibold">
          Return to Browse
        </Link>
      </div>
    );
  }

  if (title.type === 'SERIES' && !episode) {
    return (
      <div className="w-full h-screen bg-black flex flex-col items-center justify-center gap-4 text-muted-foreground">
        <p>No episodes are available for this series yet.</p>
        <Link href={`/title/${title.id}`} className="px-6 py-2.5 rounded-full bg-white text-black font-semibold">
          Back to details
        </Link>
      </div>
    );
  }

  return (
    <VideoPlayer
      key={episode?.id ?? title.id}
      title={title}
      contentId={episode?.id ?? title.id}
      targetType={episode ? 'EPISODE' : 'MOVIE'}
      episodeTitle={episode?.title}
    />
  );
}
