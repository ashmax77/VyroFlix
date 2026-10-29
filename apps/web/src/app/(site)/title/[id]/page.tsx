'use client';

import React, { useCallback, useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Image from 'next/image';
import Link from 'next/link';
import { ArrowLeft, Check, Play, Plus } from 'lucide-react';
import { ApiError, fetchProgress, fetchTitle, fetchSimilarTitles } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { formatTime } from '@/lib/utils';
import { useWatchlist } from '@/lib/watchlist-context';
import { SLOW_LOAD_MESSAGE, useSlowHint } from '@/lib/hooks';
import ContentRail from '@/components/ContentRail';
import { Button } from '@/components/ui/button';
import { EmptyState, ErrorState, MaturityBadge, Skeleton } from '@/components/common';
import type { Title } from '@/types';

export default function TitleDetailsPage() {
  const params = useParams();
  const router = useRouter();
  const titleId = params.id as string;
  const { has, toggle, ready } = useWatchlist();

  const [title, setTitle] = useState<Title | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [similar, setSimilar] = useState<Title[]>([]);
  const [seasonIndex, setSeasonIndex] = useState(0);
  const [resumeAt, setResumeAt] = useState<number | null>(null);
  const { isAuthed, loading: authLoading } = useAuth();
  const [listBusy, setListBusy] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const slow = useSlowHint(loading);

  const load = useCallback(() => {
    if (!titleId) return;
    setError(null);
    setLoading(true);
    fetchTitle(titleId)
      .then((data) => {
        setTitle(data);
        setSeasonIndex(0);
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : "Couldn't load this title. Try again."))
      .finally(() => setLoading(false));
    fetchSimilarTitles(titleId, 12).then(setSimilar).catch(() => setSimilar([]));
  }, [titleId]);

  useEffect(load, [load]);

  // Resume point for signed-in viewers. Series resume per episode, from the episode list, so only movies use this.
  const loadedId = title?.id;
  const isMovie = title?.type === 'MOVIE';
  useEffect(() => {
    if (authLoading || !isAuthed || !loadedId || !isMovie) {
      setResumeAt(null);
      return;
    }
    let cancelled = false;
    fetchProgress(loadedId)
      .then((progress) => {
        if (!cancelled) setResumeAt(progress && !progress.completed && progress.positionSeconds > 0 ? progress.positionSeconds : null);
      })
      .catch(() => {
        if (!cancelled) setResumeAt(null);
      });
    return () => {
      cancelled = true;
    };
  }, [authLoading, isAuthed, loadedId, isMovie]);

  const inList = title ? has(title.id) : false;

  const onToggleList = async () => {
    if (!title || listBusy) return;
    setListBusy(true);
    setListError(null);
    try {
      await toggle(title.id);
    } catch (e) {
      setListError(
        e instanceof ApiError && e.isUnauthorized ? 'Sign in to use My list.' : "Couldn't update My list. Try again.",
      );
    } finally {
      setListBusy(false);
    }
  };

  if (loading) {
    return (
      <div aria-busy="true" className="mx-auto max-w-container px-4 py-10 sm:px-6 lg:px-10">
        <Skeleton className="h-[40vh] w-full" />
        <p className="mt-4 text-sm text-muted-foreground">{slow ? SLOW_LOAD_MESSAGE : 'Loading title…'}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="mx-auto max-w-container px-4 py-24 sm:px-6 lg:px-10">
        <ErrorState message={error} onRetry={load} />
      </div>
    );
  }

  if (!title) {
    return (
      <div className="mx-auto max-w-container px-4 py-16 sm:px-6 lg:px-10">
        <EmptyState
          title="This title isn't available"
          description="It may have been unpublished or the link is out of date."
          action={
            <Button variant="secondary" asChild>
              <Link href="/browse">Back to browse</Link>
            </Button>
          }
        />
      </div>
    );
  }

  const runtime = title.type === 'SERIES'
    ? `${title.seasons?.length || 1} season${(title.seasons?.length || 1) > 1 ? 's' : ''}`
    : null;
  const facts = [title.releaseYear || null, runtime].filter(Boolean) as string[];
  const season = title.seasons?.[seasonIndex];

  return (
    <div className="pb-10">
      {/* Backdrop sits in normal flow below the fixed navbar, so nothing is hidden under it. */}
      <div className="relative h-[min(52vh,560px)] min-h-[300px] w-full overflow-hidden bg-background">
        {(title.bannerUrl || title.posterUrl) && (
          <Image src={title.bannerUrl || title.posterUrl} alt="" fill priority sizes="100vw" className="object-cover object-[center_25%]" />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-background via-background/40 to-transparent" aria-hidden />
      </div>

      <div className="mx-auto -mt-40 grid max-w-container grid-cols-1 gap-10 px-4 sm:px-6 md:grid-cols-[minmax(0,220px)_minmax(0,1fr)] lg:px-10">
        <div className="relative hidden md:block">
          <div className="relative aspect-[2/3] overflow-hidden rounded-[2px] bg-surface ring-1 ring-inset ring-white/10">
            {title.posterUrl ? (
              <Image src={title.posterUrl} alt={title.title} fill sizes="220px" className="object-cover" />
            ) : null}
          </div>
        </div>

        <div className="relative min-w-0">
          <button
            type="button"
            onClick={() => router.back()}
            className="mb-4 inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="h-4 w-4" aria-hidden /> Back
          </button>

          <h1 className="font-heading text-4xl font-semibold leading-tight tracking-tight sm:text-6xl">{title.title}</h1>

          {(facts.length > 0 || title.maturityRating) && (
            <p className="mt-4 flex flex-wrap items-center gap-x-3 gap-y-2 text-sm text-muted-foreground">
              <MaturityBadge rating={title.maturityRating} />
              {facts.map((f, i) => (
                <React.Fragment key={i}>
                  {(i > 0 || title.maturityRating) && <span aria-hidden>·</span>}
                  <span>{f}</span>
                </React.Fragment>
              ))}
            </p>
          )}

          {title.genres && title.genres.length > 0 && (
            <p className="mt-3 text-sm text-subtle">{title.genres.map((g) => g.name).join(' · ')}</p>
          )}

          <div className="mt-7 flex flex-wrap items-center gap-3">
            <Button size="lg" asChild className="gap-2">
              <Link href={`/watch/${title.id}`}>
                <Play className="h-4 w-4 fill-current" aria-hidden />
                {resumeAt ? `Resume at ${formatTime(resumeAt)}` : 'Play'}
              </Link>
            </Button>
            <Button variant="secondary" size="lg" onClick={onToggleList} disabled={listBusy || !ready} className="gap-2">
              {inList ? <Check className="h-4 w-4 text-primary" aria-hidden /> : <Plus className="h-4 w-4" aria-hidden />}
              <span>{inList ? 'In My List' : 'Add to My List'}</span>
            </Button>
          </div>
          {listError && (
            <p role="alert" className="mt-3 text-sm text-danger">
              {listError}
            </p>
          )}

          {title.synopsis && (
            <section className="mt-10 max-w-prose" aria-labelledby="synopsis-heading">
              <h2 id="synopsis-heading" className="text-xl text-foreground">Synopsis</h2>
              <p className="mt-3 text-base leading-relaxed text-foreground/85">{title.synopsis}</p>
            </section>
          )}

          {title.cast && title.cast.length > 0 && (
            <section className="mt-10" aria-labelledby="cast-heading">
              <h2 id="cast-heading" className="text-xl text-foreground">Cast and crew</h2>
              <ul className="mt-4 grid grid-cols-1 gap-x-8 gap-y-3 text-sm sm:grid-cols-2">
                {title.cast.map((member) => (
                  <li key={`${member.personId}-${member.role}`} className="flex items-baseline justify-between gap-3 border-b border-border pb-2">
                    <span className="text-foreground">{member.name}</span>
                    <span className="text-right text-xs text-subtle">
                      {member.characterName ? `${member.characterName} · ` : ''}
                      {member.role.charAt(0) + member.role.slice(1).toLowerCase()}
                    </span>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {title.type === 'SERIES' && title.seasons && title.seasons.length > 0 && (
            <section className="mt-10" aria-labelledby="episodes-heading">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <h2 id="episodes-heading" className="text-xl text-foreground">Episodes</h2>
                {title.seasons.length > 1 && (
                  <select
                    aria-label="Season"
                    value={seasonIndex}
                    onChange={(e) => setSeasonIndex(Number(e.target.value))}
                    className="h-9 rounded-card border border-border bg-surface px-3 text-sm text-foreground focus:border-primary focus:outline-none"
                  >
                    {title.seasons.map((s, index) => (
                      <option key={s.id} value={index}>
                        {s.name || `Season ${s.seasonNumber}`}
                      </option>
                    ))}
                  </select>
                )}
              </div>
              {season?.episodes && season.episodes.length > 0 ? (
                <ol className="mt-4 divide-y divide-border border-y border-border">
                  {season.episodes.map((ep) => (
                    <li key={ep.id}>
                      <Link href={`/watch/${title.id}?episode=${ep.id}`} className="group grid grid-cols-[2.5rem_1fr_auto] items-center gap-4 py-4">
                        <span className="text-right font-heading text-lg text-subtle">{ep.episodeNumber}</span>
                        <span className="min-w-0">
                          <span className="block truncate text-sm font-medium text-foreground group-hover:text-primary">{ep.name}</span>
                          {ep.overview && <span className="mt-0.5 line-clamp-1 block text-xs text-muted-foreground">{ep.overview}</span>}
                        </span>
                        <span className="text-xs text-subtle">{ep.durationSeconds ? `${Math.round(ep.durationSeconds / 60)} min` : ''}</span>
                      </Link>
                    </li>
                  ))}
                </ol>
              ) : (
                <p className="mt-4 text-sm text-muted-foreground">No episodes are published for this season yet.</p>
              )}
            </section>
          )}
        </div>
      </div>

      {similar.length > 0 && (
        <div className="mt-14">
          <ContentRail title="More like this" items={similar.filter((t) => t.id !== title.id)} />
        </div>
      )}
    </div>
  );
}
