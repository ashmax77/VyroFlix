'use client';

import React, { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { Play, Plus, Check, ArrowLeft, Clock, Calendar, Shield } from 'lucide-react';
import { ApiError, fetchTitle, fetchSimilarTitles, toggleWatchlist, checkWatchlist } from '../../../lib/api';
import { SLOW_LOAD_MESSAGE, useSlowHint } from '../../../lib/hooks';
import { Title } from '../../../types';
import ContentRail from '../../../components/ContentRail';
import { ErrorState, Spinner } from '../../../components/common';

export default function TitleDetailsPage() {
  const params = useParams();
  const router = useRouter();
  const titleId = params.id as string;

  const [title, setTitle] = useState<Title | null>(null);
  const [inList, setInList] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingList, setLoadingList] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [listError, setListError] = useState<string | null>(null);
  const [similar, setSimilar] = useState<Title[]>([]);
  const [seasonIndex, setSeasonIndex] = useState(0);
  const slow = useSlowHint(loading);

  const load = () => {
    if (!titleId) return;
    setError(null);
    setLoading(true);
    Promise.all([fetchTitle(titleId), checkWatchlist(titleId)])
      .then(([titleData, inListData]) => {
        setTitle(titleData);
        setInList(inListData);
        setSeasonIndex(0);
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : 'Could not load this title.'))
      .finally(() => setLoading(false));
    fetchSimilarTitles(titleId, 12).then(setSimilar);
  };

  useEffect(load, [titleId]);

  const handleToggleList = async () => {
    if (!title) return;
    setLoadingList(true);
    setListError(null);
    try {
      setInList(await toggleWatchlist(title.id, inList));
    } catch (e) {
      setListError(
        e instanceof ApiError && e.isUnauthorized
          ? 'Sign in to use My List.'
          : 'Could not update My List. Please try again.'
      );
    } finally {
      setLoadingList(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Spinner label={slow ? SLOW_LOAD_MESSAGE : undefined} />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <ErrorState message={error} onRetry={load} />
      </div>
    );
  }

  if (!title) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center">
        <h2 className="text-xl font-bold mb-4">Title Not Found</h2>
        <Link href="/browse" className="px-6 py-2.5 rounded-full bg-white text-black font-semibold">
          Return to Browse
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen pb-20">
      {/* Backdrop Header */}
      <div className="relative w-full h-[50vh] sm:h-[65vh] overflow-hidden">
        <img
          src={title.backdropUrl || title.posterUrl}
          alt={title.title}
          className="w-full h-full object-cover object-center"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-background via-background/60 to-transparent" />
        <div className="absolute inset-0 bg-gradient-to-r from-background via-transparent to-transparent" />

        {/* Back Button */}
        <button
          onClick={() => router.back()}
          className="absolute top-24 left-6 z-30 w-10 h-10 rounded-full glass-panel flex items-center justify-center text-white hover:scale-105 transition-transform"
          aria-label="Back"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
      </div>

      {/* Main Details Body */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 -mt-28 relative z-20">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Poster Column */}
          <div className="hidden md:block col-span-1">
            <div className="aspect-[2/3] rounded-card overflow-hidden shadow-2xl border border-border">
              <img src={title.posterUrl} alt={title.title} className="w-full h-full object-cover" />
            </div>
          </div>

          {/* Details Column */}
          <div className="col-span-1 md:col-span-3 flex flex-col justify-end">
            <h1 className="font-heading font-extrabold text-3xl sm:text-5xl text-white mb-3">
              {title.title}
            </h1>

            {/* Badges and metadata */}
            <div className="flex flex-wrap items-center gap-3 text-xs sm:text-sm text-text-secondary mb-6">
              <span className="flex items-center space-x-1 px-2 py-0.5 rounded border border-border text-white font-medium">
                <Shield className="w-3.5 h-3.5 text-accent-cyan" />
                <span>{title.maturityRating}</span>
              </span>
              <span className="flex items-center space-x-1">
                <Calendar className="w-3.5 h-3.5" />
                <span>{title.releaseYear}</span>
              </span>
              <span>•</span>
              <span className="flex items-center space-x-1">
                <Clock className="w-3.5 h-3.5" />
                <span>{title.durationMinutes} min</span>
              </span>
              <span>•</span>
              <span className="uppercase text-[11px] font-bold tracking-wider text-purple-400">
                {title.type}
              </span>
            </div>

            {/* Action buttons */}
            <div className="flex flex-wrap items-center gap-3 mb-8">
              <Link
                href={`/watch/${title.id}`}
                className="flex items-center space-x-2 px-8 py-3.5 bg-white text-black hover:bg-gray-200 rounded-full font-bold text-sm shadow-xl transition-all transform hover:scale-105"
              >
                <Play className="w-5 h-5 fill-black" />
                <span>Play</span>
              </Link>

              <button
                onClick={handleToggleList}
                disabled={loadingList}
                className="flex items-center space-x-2 px-6 py-3.5 glass-panel text-white hover:bg-surface-elevated rounded-full font-semibold text-sm transition-all border border-border"
              >
                {inList ? (
                  <>
                    <Check className="w-5 h-5 text-green-400" />
                    <span>In My List</span>
                  </>
                ) : (
                  <>
                    <Plus className="w-5 h-5" />
                    <span>Add to My List</span>
                  </>
                )}
              </button>
            </div>
            {listError && (
              <p role="alert" className="-mt-5 mb-6 text-sm text-red-400">
                {listError}
              </p>
            )}

            {/* Synopsis */}
            <div className="glass-panel p-6 rounded-card border border-border mb-8">
              <h3 className="font-heading font-bold text-base text-white mb-2">Synopsis</h3>
              <p className="text-text-secondary text-sm leading-relaxed">{title.synopsis}</p>
            </div>

            {/* Seasons & Episodes (if series) */}
            {title.type === 'SERIES' && title.seasons && title.seasons.length > 0 && (
              <div className="glass-panel p-6 rounded-card border border-border mb-8">
                {title.seasons.length > 1 ? (
                  <select
                    aria-label="Season"
                    value={seasonIndex}
                    onChange={(e) => setSeasonIndex(Number(e.target.value))}
                    className="mb-4 rounded-lg border border-border bg-surface px-3 py-2 font-heading font-bold text-white"
                  >
                    {title.seasons.map((season, index) => (
                      <option key={season.id} value={index}>
                        {season.title || `Season ${season.seasonNumber}`}
                      </option>
                    ))}
                  </select>
                ) : (
                  <h3 className="font-heading font-bold text-lg text-white mb-4">
                    {title.seasons[0].title}
                  </h3>
                )}
                <div className="space-y-3">
                  {title.seasons[seasonIndex]?.episodes?.map((ep) => (
                    <Link
                      key={ep.id}
                      href={`/watch/${title.id}?episode=${ep.id}`}
                      className="flex items-center justify-between p-3 rounded-lg bg-surface hover:bg-surface-elevated transition-colors border border-border/50 group"
                    >
                      <div className="flex items-center space-x-4">
                        <span className="font-heading font-bold text-sm text-text-muted w-6 text-center">
                          {ep.episodeNumber}
                        </span>
                        <div>
                          <h4 className="font-semibold text-sm text-white group-hover:text-purple-300 transition-colors">
                            {ep.title}
                          </h4>
                          <p className="text-xs text-text-secondary line-clamp-1">{ep.synopsis}</p>
                        </div>
                      </div>
                      <span className="text-xs text-text-muted">{ep.durationMinutes}m</span>
                    </Link>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* More Like This Rail */}
        {similar.length > 0 && (
          <div className="mt-12">
            <ContentRail title="More Like This" items={similar.filter((t) => t.id !== title.id)} />
          </div>
        )}
      </div>
    </div>
  );
}
