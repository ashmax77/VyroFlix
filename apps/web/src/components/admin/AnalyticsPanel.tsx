'use client';

import React, { useEffect, useState } from 'react';
import { ErrorState, Spinner } from '../common';
import {
  ApiError,
  fetchCatalogStats,
  fetchEncodingStats,
  fetchPlaybackStats,
  type CatalogStats,
  type PlaybackStats,
} from '../../lib/api';

/**
 * Read-only platform figures for administrators. Each number comes from the service that owns it:
 * catalog from Content, playback from Streaming, processing from Encoding.
 */
export default function AnalyticsPanel() {
  const [catalog, setCatalog] = useState<CatalogStats | null>(null);
  const [playback, setPlayback] = useState<PlaybackStats | null>(null);
  const [encoding, setEncoding] = useState<Record<string, number> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    setError(null);
    Promise.allSettled([fetchCatalogStats(), fetchPlaybackStats(7), fetchEncodingStats()]).then(
      ([c, p, e]) => {
        if (c.status === 'fulfilled') setCatalog(c.value);
        if (p.status === 'fulfilled') setPlayback(p.value);
        if (e.status === 'fulfilled') setEncoding(e.value);
        const failed = [c, p, e].find((r) => r.status === 'rejected') as PromiseRejectedResult | undefined;
        if (failed) {
          setError(failed.reason instanceof ApiError ? failed.reason.message : 'Some figures could not be loaded.');
        }
        setLoading(false);
      },
    );
  };

  useEffect(load, []);

  if (loading && !catalog && !playback && !encoding) {
    return <Spinner label="Loading analytics…" />;
  }

  return (
    <section aria-label="Platform analytics" className="space-y-4">
      {error && <ErrorState compact message={error} onRetry={load} />}

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <Stat label="Titles" value={catalog?.totalTitles} />
        <Stat label="Published" value={catalog?.byPublicationState?.PUBLISHED} />
        <Stat label="Active streams" value={playback?.activeSessions} />
        <Stat
          label="Jobs in progress"
          value={encoding ? (encoding.QUEUED ?? 0) + (encoding.PROCESSING ?? 0) : undefined}
        />
      </div>

      {playback && playback.daily.length > 0 && (
        <div className="rounded-xl border border-white/10 bg-surface p-4">
          <h3 className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Playback sessions, last 7 days
          </h3>
          <ul className="space-y-1 text-sm">
            {playback.daily.map((d) => (
              <li key={d.date} className="flex justify-between">
                <span className="text-muted-foreground">{d.date}</span>
                <span className="font-semibold text-white">{d.sessions}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {encoding && (
        <div className="rounded-xl border border-white/10 bg-surface p-4 text-sm">
          <h3 className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Processing jobs</h3>
          <p className="text-muted-foreground">
            Queued {encoding.QUEUED ?? 0} · Processing {encoding.PROCESSING ?? 0} · Ready {encoding.READY ?? 0} ·
            Failed {encoding.FAILED ?? 0}
          </p>
        </div>
      )}
    </section>
  );
}

function Stat({ label, value }: { label: string; value: number | undefined }) {
  return (
    <div className="rounded-xl border border-white/10 bg-surface p-4">
      <p className="text-xs uppercase tracking-wider text-muted-foreground">{label}</p>
      <p className="mt-1 text-2xl font-bold text-white">{value ?? '—'}</p>
    </div>
  );
}
