'use client';

import React, { useCallback, useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Image from 'next/image';
import Link from 'next/link';
import { ArrowLeft, Check, Clock, Film, Play, Plus, Share2, ThumbsDown, ThumbsUp } from 'lucide-react';
import { ApiError, fetchProgress, fetchTitle, fetchSimilarTitles } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { formatTime } from '@/lib/utils';
import { useWatchlist } from '@/lib/watchlist-context';
import ContentRail from '@/components/ContentRail';
import { Button } from '@/components/ui/button';
import { EmptyState, ErrorState, MaturityBadge, PageContainer, Skeleton } from '@/components/common';
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
  const [liked, setLiked] = useState<boolean | null>(null);
  const [copied, setCopied] = useState(false);
  const { isAuthed, loading: authLoading } = useAuth();
  const [listBusy, setListBusy] = useState(false);

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
    try {
      await toggle(title.id);
    } finally {
      setListBusy(false);
    }
  };

  const handleShare = () => {
    if (typeof window !== 'undefined') {
      navigator.clipboard?.writeText(window.location.href);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  if (loading) {
    return (
      <div>
        <Skeleton className="h-[70vh] rounded-none" />
        <PageContainer className="mt-8 space-y-4">
          <Skeleton className="h-8 w-1/3" />
          <Skeleton className="h-4 w-2/3" />
        </PageContainer>
      </div>
    );
  }

  if (error || !title) {
    return (
      <PageContainer className="pt-32 pb-24 text-center">
        <EmptyState
          icon={Film}
          title="Title unavailable"
          description={error ?? "We couldn't locate this title in our catalog."}
          action={
            <Button variant="gradient" asChild>
              <Link href="/browse">Return to Home</Link>
            </Button>
          }
        />
      </PageContainer>
    );
  }

  const backdrop = title.bannerUrl || title.posterUrl;
  const curSeason = title.seasons?.[seasonIndex];
  const seasonsCount = title.seasons?.length ?? 1;

  return (
    <div className="pb-24 overflow-hidden">
      {/* Cinematic Full-Bleed Backdrop Stage */}
      <section className="relative min-h-[76vh] flex flex-col justify-end">
        {backdrop && (
          <div className="absolute inset-0 z-0">
            <Image src={backdrop} alt="" fill priority sizes="100vw" className="object-cover" />
          </div>
        )}
        <div className="absolute inset-0 bg-fade-left z-10" />
        <div className="absolute inset-0 bg-fade-bottom z-10" />
        <div className="absolute inset-0 bg-background/25 z-10 pointer-events-none" />

        <PageContainer className="relative z-20 pb-12 pt-36">
          <button
            type="button"
            onClick={() => router.back()}
            className="mb-6 inline-flex items-center gap-2 rounded-full border border-border bg-surface/70 px-4 py-1.5 text-xs font-semibold text-muted-foreground backdrop-blur hover:text-foreground hover:border-cyan transition-colors"
          >
            <ArrowLeft className="h-3.5 w-3.5" /> Back
          </button>

          <div className="max-w-3xl animate-page-in">
            <h1 className="text-4xl font-black tracking-tight sm:text-6xl lg:text-7xl text-foreground drop-shadow-md">
              {title.title}
            </h1>

            {/* Metadata Pills */}
            <div className="mt-5 flex flex-wrap items-center gap-3 text-sm text-foreground/80 font-medium">
              <MaturityBadge rating={title.maturityRating} />
              <span>{title.releaseYear}</span>
              <span>·</span>
              <span>
                {title.type === 'SERIES' ? `${seasonsCount} Season${seasonsCount > 1 ? 's' : ''}` : 'Feature Film'}
              </span>
              {title.genres && title.genres.length > 0 && (
                <>
                  <span>·</span>
                  <span className="text-cyan">{title.genres.map((g) => g.name).join(', ')}</span>
                </>
              )}
            </div>

            {/* Synopsis */}
            {title.synopsis && (
              <p className="mt-5 max-w-2xl text-base sm:text-lg leading-relaxed text-foreground/90 drop-shadow">
                {title.synopsis}
              </p>
            )}

            {/* Interactive Actions Bar */}
            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Button
                variant="light"
                size="lg"
                className="h-12 px-8 font-bold shadow-glow"
                asChild
              >
                <Link href={`/watch/${title.id}`}>
                  <Play className="h-5 w-5 fill-current mr-2" />
                  {resumeAt ? `Resume at ${formatTime(resumeAt)}` : 'Watch Now'}
                </Link>
              </Button>

              <Button
                variant="glass"
                size="lg"
                onClick={onToggleList}
                disabled={listBusy || !ready}
                className="h-12 px-6"
              >
                {inList ? <Check className="h-5 w-5 text-cyan mr-2" /> : <Plus className="h-5 w-5 mr-2" />}
                <span>{inList ? 'In Watchlist' : 'Add to List'}</span>
              </Button>

              {/* Thumbs Feedback */}
              <div className="flex items-center gap-1.5 border border-border bg-surface/80 rounded-full p-1 backdrop-blur">
                <button
                  type="button"
                  aria-label="I like this"
                  onClick={() => setLiked(liked === true ? null : true)}
                  className={`grid h-10 w-10 place-items-center rounded-full transition-colors ${
                    liked === true ? 'bg-cyan/20 text-cyan' : 'text-subtle hover:text-foreground'
                  }`}
                >
                  <ThumbsUp className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  aria-label="Not for me"
                  onClick={() => setLiked(liked === false ? null : false)}
                  className={`grid h-10 w-10 place-items-center rounded-full transition-colors ${
                    liked === false ? 'bg-danger/20 text-danger' : 'text-subtle hover:text-foreground'
                  }`}
                >
                  <ThumbsDown className="h-4 w-4" />
                </button>
              </div>

              {/* Share button */}
              <button
                type="button"
                onClick={handleShare}
                aria-label="Share"
                className="grid h-12 w-12 place-items-center rounded-full border border-border bg-surface/80 text-foreground hover:bg-surface-elevated transition-colors"
              >
                <Share2 className="h-4 w-4" />
              </button>
              {copied && <span className="text-xs font-bold text-cyan animate-page-in">Link copied!</span>}
            </div>
          </div>
        </PageContainer>
      </section>

      {/* Episodes & Seasons Section (For Series) */}
      {title.type === 'SERIES' && title.seasons && title.seasons.length > 0 && (
        <PageContainer className="mt-12">
          <div className="border-t border-border pt-10">
            <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
              <h2 className="text-2xl font-bold text-foreground">Episodes</h2>
              {title.seasons.length > 1 && (
                <div className="flex items-center gap-2">
                  <span className="text-xs text-subtle font-semibold uppercase">Season:</span>
                  <select
                    value={seasonIndex}
                    onChange={(e) => setSeasonIndex(Number(e.target.value))}
                    aria-label="Season selector"
                    className="h-9 rounded-full border border-border bg-surface px-4 text-xs font-semibold text-foreground outline-none focus:border-cyan cursor-pointer"
                  >
                    {title.seasons.map((s, idx) => (
                      <option key={s.id} value={idx}>
                        {s.name || `Season ${s.seasonNumber}`}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>

            {curSeason?.episodes && curSeason.episodes.length > 0 ? (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {curSeason.episodes.map((ep) => (
                  <Link
                    key={ep.id}
                    href={`/watch/${title.id}?episode=${ep.id}`}
                    className="group flex flex-col overflow-hidden rounded-2xl border border-border bg-surface hover:border-cyan hover:shadow-glow transition-all"
                  >
                    <div className="relative aspect-video w-full overflow-hidden bg-surface-elevated">
                      {ep.thumbnailUrl ? (
                        <Image src={ep.thumbnailUrl} alt={ep.name} fill sizes="380px" className="object-cover group-hover:scale-105 transition-transform duration-300" />
                      ) : (
                        <div className="absolute inset-0 grid place-items-center bg-surface">
                          <Play className="h-8 w-8 text-cyan" />
                        </div>
                      )}
                      <div className="absolute inset-0 bg-fade-bottom opacity-60" />
                      <div className="absolute left-3 top-3 grid h-8 w-8 place-items-center rounded-full glass font-bold text-xs text-white">
                        {ep.episodeNumber}
                      </div>
                    </div>
                    <div className="p-4 flex-1 flex flex-col justify-between">
                      <div>
                        <h3 className="font-bold text-base text-foreground group-hover:text-cyan transition-colors">
                          {ep.name}
                        </h3>
                        {ep.overview && (
                          <p className="mt-1.5 line-clamp-2 text-xs leading-relaxed text-muted-foreground">
                            {ep.overview}
                          </p>
                        )}
                      </div>
                      <p className="mt-4 text-[11px] font-semibold text-subtle">
                        {ep.durationSeconds ? `${Math.round(ep.durationSeconds / 60)}m` : '45m'}
                      </p>
                    </div>
                  </Link>
                ))}
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">No episodes available yet for this season.</p>
            )}
          </div>
        </PageContainer>
      )}

      {/* Cast & Crew Carousel */}
      {title.cast && title.cast.length > 0 && (
        <PageContainer className="mt-14">
          <div className="border-t border-border pt-10">
            <h2 className="text-2xl font-bold text-foreground mb-6">Cast & Crew</h2>
            <div className="no-scrollbar flex gap-4 overflow-x-auto pb-4">
              {title.cast.map((member, i) => (
                <div key={i} className="flex flex-col items-center gap-2.5 w-28 shrink-0 text-center">
                  <div className="grid h-20 w-20 place-items-center rounded-full bg-gradient-brand text-xl font-bold text-white shadow-glow">
                    {member.name.slice(0, 1)}
                  </div>
                  <div className="min-w-0">
                    <p className="truncate text-xs font-bold text-foreground">{member.name}</p>
                    <p className="truncate text-[11px] text-muted-foreground">
                      {member.characterName || (member.role.charAt(0) + member.role.slice(1).toLowerCase())}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </PageContainer>
      )}

      {/* More Like This Content Rail */}
      {similar.length > 0 && (
        <div className="mt-12">
          <ContentRail title="More Like This" items={similar.filter((t) => t.id !== title.id)} />
        </div>
      )}
    </div>
  );
}
