'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { EmptyState, ErrorState, Skeleton } from '@/components/common';
import {
  ApiError,
  completeUploadIntent,
  createUploadIntent,
  fetchAdminTitles,
  getEncodingJobs,
  retryEncodingJob,
  uploadFileDirectToStorage,
} from '@/lib/api';
import { cn } from '@/lib/utils';
import type { EncodingJob, EncodingStatus, Title } from '@/types';

type Step = 'IDLE' | 'INTENT' | 'UPLOADING' | 'COMPLETING' | 'DONE';

const STATUS_FILTERS: { value: EncodingStatus | ''; label: string }[] = [
  { value: '', label: 'All' },
  { value: 'QUEUED', label: 'Queued' },
  { value: 'PROCESSING', label: 'Processing' },
  { value: 'READY', label: 'Ready' },
  { value: 'FAILED', label: 'Failed' },
];

const STEP_TEXT: Record<Step, string> = {
  IDLE: '',
  INTENT: 'Requesting an upload link…',
  UPLOADING: 'Uploading the file straight to storage. It does not pass through the API.',
  COMPLETING: 'Checking the file and queueing encoding…',
  DONE: 'Uploaded. Encoding has been queued and will appear below.',
};

export default function UploadsPage() {
  const [titles, setTitles] = useState<Title[]>([]);
  const [titlesError, setTitlesError] = useState<string | null>(null);
  const [contentId, setContentId] = useState('');
  const [targetType, setTargetType] = useState<'MOVIE' | 'EPISODE'>('MOVIE');
  const [file, setFile] = useState<File | null>(null);
  const [step, setStep] = useState<Step>('IDLE');
  const [progress, setProgress] = useState(0);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);

  const [jobs, setJobs] = useState<EncodingJob[]>([]);
  const [jobsLoading, setJobsLoading] = useState(true);
  const [jobsError, setJobsError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<EncodingStatus | ''>('');
  const [retryingId, setRetryingId] = useState<string | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const loadTitles = useCallback(() => {
    setTitlesError(null);
    fetchAdminTitles()
      .then((data) => {
        setTitles(data);
        setContentId((current) => current || data[0]?.id || '');
      })
      .catch((e) => setTitlesError(e instanceof ApiError ? e.message : "Couldn't load titles."));
  }, []);

  const loadJobs = useCallback(() => {
    getEncodingJobs(statusFilter || undefined)
      .then((data) => {
        setJobs(data);
        setJobsError(null);
      })
      .catch((e) => {
        // The encoding worker runs outside the free-tier API, so a 404 or network failure usually means it is offline.
        setJobsError(
          e instanceof ApiError && (e.isNotFound || e.isNetwork)
            ? "The encoding worker isn't reachable, so new uploads won't be processed. Start it to continue."
            : e instanceof ApiError
              ? e.message
              : "Couldn't load encoding jobs.",
        );
      })
      .finally(() => setJobsLoading(false));
  }, [statusFilter]);

  useEffect(loadTitles, [loadTitles]);
  useEffect(loadJobs, [loadJobs]);

  useEffect(() => {
    if (!autoRefresh) return;
    const id = setInterval(loadJobs, 8000);
    return () => clearInterval(id);
  }, [autoRefresh, loadJobs]);

  const busy = step !== 'IDLE' && step !== 'DONE';

  const start = async () => {
    if (!file || !contentId) return;
    setUploadError(null);
    setProgress(0);
    try {
      setStep('INTENT');
      const intent = await createUploadIntent({
        contentId,
        targetType,
        filename: file.name,
        contentType: file.type || 'video/mp4',
        fileSizeBytes: file.size,
      });
      setStep('UPLOADING');
      await uploadFileDirectToStorage(intent.uploadUrl, file, setProgress);
      setStep('COMPLETING');
      await completeUploadIntent(intent.id);
      setStep('DONE');
      setFile(null);
      if (fileInput.current) fileInput.current.value = '';
      loadJobs();
    } catch (e) {
      setUploadError(e instanceof Error ? e.message : 'The upload did not finish. Try again.');
      setStep('IDLE');
    }
  };

  const retry = async (jobId: string) => {
    setRetryingId(jobId);
    try {
      await retryEncodingJob(jobId);
      loadJobs();
    } catch (e) {
      setJobsError(e instanceof ApiError ? e.message : "Couldn't retry this job. Try again.");
    } finally {
      setRetryingId(null);
    }
  };

  const titleName = (id: string) => titles.find((t) => t.id === id)?.title ?? 'Unknown title';

  return (
    <section aria-labelledby="uploads-heading" className="space-y-12">
      <header>
        <h1 id="uploads-heading" className="text-3xl sm:text-4xl">Uploads</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Upload a source file for a title. It is converted to adaptive streams, then the title can be published.
        </p>
      </header>

      <div className="grid gap-12 lg:grid-cols-[minmax(0,420px)_minmax(0,1fr)]">
        <div className="space-y-6">
          <h2 className="text-xl text-foreground">New upload</h2>

          {titlesError && <ErrorState compact message={titlesError} onRetry={loadTitles} />}

          <div>
            <label htmlFor="u-title" className="mb-1.5 block text-sm font-medium text-foreground">Title</label>
            <select
              id="u-title"
              value={contentId}
              onChange={(e) => setContentId(e.target.value)}
              disabled={busy || titles.length === 0}
              className="h-11 w-full rounded-card border border-border bg-surface px-3.5 text-sm text-foreground focus:border-primary focus:outline-none disabled:opacity-50"
            >
              {titles.length === 0 && <option value="">{titlesError ? 'Titles unavailable' : 'Create a title first'}</option>}
              {titles.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.title} ({t.type === 'SERIES' ? 'Series' : 'Movie'})
                </option>
              ))}
            </select>
          </div>

          <fieldset>
            <legend className="mb-1.5 text-sm font-medium text-foreground">This file is</legend>
            <div className="flex gap-6 text-sm">
              {(['MOVIE', 'EPISODE'] as const).map((t) => (
                <label key={t} className="flex items-center gap-2 text-muted-foreground">
                  <input type="radio" name="target" checked={targetType === t} onChange={() => setTargetType(t)} disabled={busy} className="accent-[rgb(var(--projector))]" />
                  {t === 'MOVIE' ? 'A movie' : 'An episode'}
                </label>
              ))}
            </div>
          </fieldset>

          <div>
            <label htmlFor="u-file" className="mb-1.5 block text-sm font-medium text-foreground">Video file</label>
            <input
              id="u-file"
              ref={fileInput}
              type="file"
              accept="video/*"
              disabled={busy}
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              className="block w-full text-sm text-muted-foreground file:mr-4 file:rounded-card file:border file:border-border file:bg-surface file:px-3 file:py-2 file:text-foreground"
            />
            {file && <p className="mt-2 text-xs text-subtle">{file.name} · {(file.size / (1024 * 1024)).toFixed(1)} MB</p>}
          </div>

          {step !== 'IDLE' && (
            <div role="status" className="space-y-2">
              <p className="text-sm text-muted-foreground">{STEP_TEXT[step]}</p>
              {step === 'UPLOADING' && (
                <div className="h-1 w-full overflow-hidden rounded-full bg-surface-elevated">
                  <div className="h-full bg-primary transition-[width]" style={{ width: `${progress}%` }} />
                </div>
              )}
            </div>
          )}
          {uploadError && <ErrorState compact message={uploadError} />}

          <Button onClick={start} disabled={!file || !contentId || busy}>
            {busy ? 'Uploading…' : 'Start upload'}
          </Button>
        </div>

        <div className="min-w-0 space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <h2 className="text-xl text-foreground">Encoding</h2>
            <div className="flex items-center gap-4">
              <label className="flex items-center gap-2 text-sm text-muted-foreground">
                <input type="checkbox" checked={autoRefresh} onChange={(e) => setAutoRefresh(e.target.checked)} className="accent-[rgb(var(--projector))]" />
                Refresh every 8 seconds
              </label>
              <Button size="sm" variant="secondary" onClick={loadJobs}>Refresh</Button>
            </div>
          </div>

          <div role="tablist" aria-label="Job status" className="flex flex-wrap gap-5 border-b border-border">
            {STATUS_FILTERS.map((f) => (
              <button
                key={f.value || 'all'}
                type="button"
                role="tab"
                aria-selected={statusFilter === f.value}
                onClick={() => setStatusFilter(f.value)}
                className={cn(
                  '-mb-px border-b-2 pb-2 text-sm transition-colors',
                  statusFilter === f.value ? 'border-primary text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground',
                )}
              >
                {f.label}
              </button>
            ))}
          </div>

          {jobsError && <ErrorState message={jobsError} onRetry={loadJobs} />}

          {jobsLoading ? (
            <div className="space-y-3" aria-busy="true">
              <Skeleton className="h-14" />
              <Skeleton className="h-14" />
            </div>
          ) : !jobsError && jobs.length === 0 ? (
            <EmptyState title="No jobs here" description="Uploads appear here as they are encoded." />
          ) : (
            <ul className="divide-y divide-border border-y border-border">
              {jobs.map((job) => (
                <li key={job.id} className="grid gap-3 py-4 sm:grid-cols-[1fr_auto] sm:items-center">
                  <div className="min-w-0">
                    <p className="truncate text-sm text-foreground">{titleName(job.contentId)}</p>
                    <p className="mt-1 text-xs text-subtle">
                      {statusText(job.status)} · updated {new Date(job.updatedAt).toLocaleString()}
                      {job.retryCount > 0 ? ` · ${job.retryCount} ${job.retryCount === 1 ? 'retry' : 'retries'}` : ''}
                    </p>
                    {job.status === 'FAILED' && job.errorMessage && (
                      <p className="mt-1 text-xs text-danger">{job.errorMessage}</p>
                    )}
                  </div>
                  {job.status === 'FAILED' && (
                    <Button size="sm" variant="secondary" disabled={retryingId === job.id} onClick={() => retry(job.id)}>
                      {retryingId === job.id ? 'Retrying…' : 'Retry'}
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </section>
  );
}

function statusText(s: EncodingStatus): string {
  return s === 'READY' ? 'Ready' : s === 'FAILED' ? 'Failed' : s === 'PROCESSING' ? 'Processing' : 'Queued';
}
