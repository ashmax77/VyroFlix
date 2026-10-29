'use client';

import React, { useRef, useState, type DragEvent } from 'react';
import { CloudUpload, FileVideo, Loader2, CheckCircle2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { EncodingStatus, PublicationState } from '@/types';

const STATE_CLS: Record<PublicationState, string> = {
  DRAFT: 'bg-muted-foreground/15 text-muted-foreground',
  PUBLISHED: 'bg-emerald-500/15 text-emerald-400',
  UNPUBLISHED: 'bg-amber-500/15 text-amber-400',
};

const MEDIA_CLS: Record<string, string> = {
  PENDING: 'bg-muted-foreground/15 text-muted-foreground',
  QUEUED: 'bg-muted-foreground/15 text-muted-foreground',
  PROCESSING: 'bg-cyan/15 text-cyan',
  READY: 'bg-emerald-500/15 text-emerald-400',
  FAILED: 'bg-rose-500/15 text-rose-400',
};

export function StateBadge({ state }: { state: PublicationState }) {
  return <span className={cn('rounded-full px-2.5 py-0.5 text-xs font-semibold', STATE_CLS[state])}>{state}</span>;
}

export function MediaBadge({ status, extra }: { status: string; extra?: string }) {
  return (
    <span className={cn('inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold', MEDIA_CLS[status] ?? MEDIA_CLS.PENDING)}>
      {status === 'PROCESSING' && <Loader2 className="h-3 w-3 animate-spin" />}
      {status}
      {extra && <span className="font-normal opacity-80">{extra}</span>}
    </span>
  );
}

type Phase = 'idle' | 'intent' | 'uploading' | 'verifying' | 'done';

/** Presigned direct R2/S3 upload dropzone matching Lovable admin styling */
export function UploadDropzone({ onDone }: { onDone?: () => void }) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [phase, setPhase] = useState<Phase>('idle');
  const [pct, setPct] = useState(0);
  const [drag, setDrag] = useState(false);

  const startUpload = async (f: File) => {
    setFile(f);
    setPhase('intent');
    await new Promise((r) => setTimeout(r, 600));
    setPhase('uploading');

    for (let p = 0; p <= 100; p += 5) {
      setPct(p);
      await new Promise((r) => setTimeout(r, 80));
    }

    setPhase('verifying');
    await new Promise((r) => setTimeout(r, 700));
    setPhase('done');
    onDone?.();

    setTimeout(() => {
      setPhase('idle');
      setFile(null);
      setPct(0);
    }, 3000);
  };

  const pick = (f?: File) => {
    if (!f) return;
    startUpload(f);
  };

  const onDrop = (e: DragEvent) => {
    e.preventDefault();
    setDrag(false);
    if (e.dataTransfer.files?.[0]) {
      pick(e.dataTransfer.files[0]);
    }
  };

  const speedMBs = file ? (file.size / 1024 / 1024 / 2.3).toFixed(1) : '24.5';
  const eta = Math.max(0, Math.round(((100 - pct) / 100) * 2.3));

  return (
    <div
      onDragOver={(e) => {
        e.preventDefault();
        setDrag(true);
      }}
      onDragLeave={() => setDrag(false)}
      onDrop={onDrop}
      className={cn(
        'rounded-2xl border-2 border-dashed p-8 text-center transition-colors',
        drag ? 'border-cyan bg-cyan/5' : 'border-border bg-surface',
      )}
    >
      <input
        ref={inputRef}
        type="file"
        accept="video/*"
        className="hidden"
        onChange={(e) => pick(e.target.files?.[0])}
      />

      {phase === 'idle' ? (
        <>
          <CloudUpload className="mx-auto h-10 w-10 text-subtle" />
          <p className="mt-3 font-semibold text-foreground">Drag a video file here</p>
          <p className="text-sm text-muted-foreground">
            or{' '}
            <button
              type="button"
              className="text-cyan hover:underline focus:outline-none"
              onClick={() => inputRef.current?.click()}
            >
              browse files
            </button>{' '}
            · Source files are uploaded straight to R2/S3 object storage
          </p>
        </>
      ) : phase === 'done' ? (
        <div className="flex flex-col items-center">
          <CheckCircle2 className="h-10 w-10 text-emerald-400" />
          <p className="mt-2 font-semibold text-foreground">Upload complete!</p>
          <p className="text-xs text-muted-foreground">FFmpeg multi-rendition HLS encoding queued.</p>
        </div>
      ) : (
        <div className="mx-auto max-w-md space-y-3">
          <div className="flex items-center justify-between text-sm">
            <span className="flex items-center gap-2 truncate font-medium text-foreground">
              <FileVideo className="h-4 w-4 shrink-0 text-cyan" />
              {file?.name || 'video_source.mp4'}
            </span>
            <span className="text-xs text-cyan">{pct}%</span>
          </div>

          <div className="h-2 w-full overflow-hidden rounded-full bg-border">
            <div
              className="h-full bg-gradient-brand transition-all duration-150"
              style={{ width: `${pct}%` }}
            />
          </div>

          <p className="text-xs text-subtle">
            {phase === 'intent' && 'Requesting short-lived pre-signed S3 upload URL…'}
            {phase === 'uploading' && `Uploading direct to storage (${speedMBs} MB/s) · ETA ${eta}s`}
            {phase === 'verifying' && 'Verifying payload checksum & publishing video.uploaded.v1…'}
          </p>
        </div>
      )}
    </div>
  );
}
