'use client';

import React, { useCallback, useEffect, useState } from 'react';
import { EmptyState, ErrorState, Skeleton } from '../common';
import {
  ApiError,
  fetchCatalogStats,
  fetchEncodingStats,
  fetchPlaybackStats,
  type CatalogStats,
  type PlaybackStats,
} from '../../lib/api';
import { cn } from '../../lib/utils';

const RANGES = [7, 30, 90] as const;

/**
 * Read-only platform figures. Each number comes from the service that owns it:
 * catalog from Content, playback from Streaming, processing from Encoding.
 */
export default function AnalyticsPanel() {
  const [days, setDays] = useState<(typeof RANGES)[number]>(7);
  const [catalog, setCatalog] = useState<CatalogStats | null>(null);
  const [playback, setPlayback] = useState<PlaybackStats | null>(null);
  const [encoding, setEncoding] = useState<Record<string, number> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    Promise.allSettled([fetchCatalogStats(), fetchPlaybackStats(days), fetchEncodingStats()]).then(([c, p, e]) => {
      if (c.status === 'fulfilled') setCatalog(c.value);
      if (p.status === 'fulfilled') setPlayback(p.value);
      if (e.status === 'fulfilled') setEncoding(e.value);
      const failed = [c, p, e].find((r) => r.status === 'rejected') as PromiseRejectedResult | undefined;
      if (failed) {
        setError(failed.reason instanceof ApiError ? failed.reason.message : "Some figures couldn't load. Try again.");
      }
      setLoading(false);
    });
  }, [days]);

  useEffect(load, [load]);

  const queued = encoding ? (encoding.QUEUED ?? 0) + (encoding.PROCESSING ?? 0) : undefined;
  const sessionsInRange = playback?.daily?.reduce((sum, d) => sum + d.sessions, 0);
  const noFigures = !loading && !catalog && !playback && !encoding;

  return (
    <section aria-labelledby="overview-heading" className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 id="overview-heading" className="text-3xl sm:text-4xl">Overview</h1>
          <p className="mt-2 text-sm text-muted-foreground">Figures from Content, Streaming and Encoding.</p>
        </div>
        <div role="group" aria-label="Playback range" className="flex gap-1 rounded-card border border-border p-1">
          {RANGES.map((r) => (
            <button
              key={r}
              type="button"
              aria-pressed={days === r}
              onClick={() => setDays(r)}
              className={cn(
                'rounded-[4px] px-3 py-1.5 text-xs transition-colors',
                days === r ? 'bg-surface-elevated text-foreground' : 'text-muted-foreground hover:text-foreground',
              )}
            >
              {r} days
            </button>
          ))}
        </div>
      </div>

      {error && <ErrorState message={error} onRetry={load} />}

      {loading && !catalog && !playback && !encoding ? (
        <div className="grid grid-cols-2 gap-6 md:grid-cols-4" aria-busy="true">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-20" />
          ))}
        </div>
      ) : noFigures ? (
        <EmptyState title="No figures yet" description="Figures appear once the services respond." />
      ) : (
        <dl className="grid grid-cols-2 gap-x-8 gap-y-6 md:grid-cols-4">
          <Stat label="Titles" value={catalog?.totalTitles} />
          <Stat label="Published" value={catalog?.byPublicationState?.PUBLISHED} />
          <Stat label="Streams right now" value={playback?.activeSessions} />
          <Stat label={`Sessions, last ${days} days`} value={sessionsInRange} />
        </dl>
      )}

      {encoding && (
        <div className="border-t border-border pt-6">
          <h2 className="text-lg text-foreground">Encoding</h2>
          <dl className="mt-4 grid grid-cols-2 gap-x-8 gap-y-4 md:grid-cols-4">
            <Stat label="In progress" value={queued} />
            <Stat label="Ready" value={encoding.READY ?? 0} />
            <Stat label="Failed" value={encoding.FAILED ?? 0} />
            <Stat label="Queued" value={encoding.QUEUED ?? 0} />
          </dl>
        </div>
      )}

      {playback && playback.daily.length > 0 && (
        <div className="border-t border-border pt-6">
          <h2 className="text-lg text-foreground">Sessions per day</h2>
          <ul className="mt-4 divide-y divide-border text-sm">
            {playback.daily.map((d) => (
              <li key={d.date} className="flex justify-between py-2">
                <span className="text-muted-foreground">{d.date}</span>
                <span className="text-foreground">{d.sessions}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

function Stat({ label, value }: { label: string; value: number | undefined }) {
  return (
    <div>
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="mt-1 font-heading text-3xl text-foreground">{value ?? '—'}</dd>
    </div>
  );
}
