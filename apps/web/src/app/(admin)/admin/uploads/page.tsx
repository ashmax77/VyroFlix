'use client';

import React, { useCallback, useEffect, useState } from 'react';
import { RotateCcw, Upload } from 'lucide-react';
import { MediaBadge, UploadDropzone } from '@/components/admin/shared';
import { EmptyState, ErrorState, Skeleton } from '@/components/common';
import { Button } from '@/components/ui/button';
import { getEncodingJobs, retryEncodingJob } from '@/lib/api';
import { cn } from '@/lib/utils';
import type { EncodingJob, EncodingStatus } from '@/types';

const FILTERS: (EncodingStatus | 'ALL')[] = ['ALL', 'QUEUED', 'PROCESSING', 'READY', 'FAILED'];

export default function UploadsPage() {
  const [filter, setFilter] = useState<EncodingStatus | 'ALL'>('ALL');
  const [open, setOpen] = useState<string | null>(null);
  const [jobs, setJobs] = useState<EncodingJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retryingId, setRetryingId] = useState<string | null>(null);

  const loadJobs = useCallback(() => {
    getEncodingJobs()
      .then((data) => {
        setJobs(data);
        setError(null);
      })
      .catch((e) => setError(e instanceof Error ? e.message : 'Could not load jobs.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadJobs();
    const interval = setInterval(loadJobs, 4000);
    return () => clearInterval(interval);
  }, [loadJobs]);

  const handleRetry = async (jobId: string) => {
    setRetryingId(jobId);
    try {
      await retryEncodingJob(jobId);
      loadJobs();
    } catch (e) {
      console.error(e);
    } finally {
      setRetryingId(null);
    }
  };

  const filteredJobs = jobs.filter((j) => filter === 'ALL' || j.status === filter);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
          Uploads & Encoding
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Upload raw media directly to object storage and monitor FFmpeg HLS encoding jobs.
        </p>
      </div>

      <UploadDropzone onDone={loadJobs} />

      {/* Filter Badges */}
      <div className="flex flex-wrap gap-2">
        {FILTERS.map((f) => {
          const count = f === 'ALL' ? jobs.length : jobs.filter((j) => j.status === f).length;
          return (
            <button
              key={f}
              type="button"
              onClick={() => setFilter(f)}
              className={cn(
                'rounded-full border px-3.5 py-1 text-xs font-medium transition-all duration-150',
                filter === f
                  ? 'border-transparent bg-gradient-brand text-white shadow-glow'
                  : 'border-border text-muted-foreground hover:border-cyan/30 hover:text-foreground',
              )}
            >
              {f === 'ALL' ? 'All' : f.charAt(0) + f.slice(1).toLowerCase()}
              <span className="ml-1.5 opacity-70">({count})</span>
            </button>
          );
        })}
      </div>

      {error ? (
        <ErrorState message={error} onRetry={loadJobs} />
      ) : loading && jobs.length === 0 ? (
        <div className="space-y-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-16 rounded-xl" />
          ))}
        </div>
      ) : filteredJobs.length === 0 ? (
        <EmptyState
          icon={Upload}
          title="No jobs matching filter"
          description="Drop a video above to enqueue a new multi-bitrate HLS packaging job."
        />
      ) : (
        <ul className="divide-y divide-border overflow-hidden rounded-2xl border border-border bg-surface shadow-card">
          {filteredJobs.map((j) => (
            <li key={j.id}>
              <button
                type="button"
                className="flex w-full flex-wrap items-center gap-4 p-4 text-left transition-colors hover:bg-surface-elevated"
                onClick={() => setOpen(open === j.id ? null : j.id)}
                aria-expanded={open === j.id}
              >
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium text-foreground">{j.rawS3Key.split('/').pop() || j.id}</p>
                  <p className="mt-0.5 text-xs text-subtle">
                    {j.id} · {j.rawS3Bucket} · Updated {new Date(j.updatedAt).toLocaleTimeString()}
                    {j.retryCount > 0 ? ` · ${j.retryCount} retry` : ''}
                  </p>
                  {(j.status === 'PROCESSING' || j.status === 'QUEUED') && (
                    <div className="mt-2.5 h-1.5 max-w-sm overflow-hidden rounded-full bg-border">
                      <div className="h-full bg-gradient-brand transition-all duration-300" style={{ width: '68%' }} />
                    </div>
                  )}
                </div>

                <MediaBadge status={j.status} extra={j.status === 'PROCESSING' ? '68%' : undefined} />

                {j.status === 'FAILED' && (
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={retryingId === j.id}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleRetry(j.id);
                    }}
                  >
                    <RotateCcw className={retryingId === j.id ? 'h-3.5 w-3.5 animate-spin' : 'h-3.5 w-3.5'} />
                    {retryingId === j.id ? 'Requeuing…' : 'Retry'}
                  </Button>
                )}
              </button>

              {open === j.id && (
                <div className="space-y-3 border-t border-border bg-background/50 p-4 text-xs">
                  {j.errorMessage && (
                    <div className="rounded-lg bg-rose-500/10 p-2.5 text-rose-400">
                      <p className="font-semibold">Encoding Error:</p>
                      <p className="mt-0.5">{j.errorMessage}</p>
                    </div>
                  )}

                  {j.renditions && j.renditions.length > 0 && (
                    <div>
                      <p className="font-semibold text-foreground">Generated HLS Renditions:</p>
                      <div className="mt-1.5 flex flex-wrap gap-2">
                        {j.renditions.map((r) => (
                          <span
                            key={r.id}
                            className="rounded-md border border-border bg-surface px-2 py-1 text-muted-foreground"
                          >
                            {r.resolution} ({Math.round(r.bitrate / 1000)} kbps) → {r.playlistKey}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  <div className="space-y-1 text-muted-foreground">
                    <p className="font-semibold text-foreground">Pipeline Events:</p>
                    <p className="flex gap-2 font-mono text-[11px]">
                      <span className="text-cyan">video.uploaded.v1</span> → Direct R2 raw upload completed
                    </p>
                    {j.status === 'READY' && (
                      <p className="flex gap-2 font-mono text-[11px]">
                        <span className="text-emerald-400">video.encoded.v1</span> → HLS master manifest written &
                        cached
                      </p>
                    )}
                  </div>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
