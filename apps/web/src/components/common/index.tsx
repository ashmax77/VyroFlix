import React, { type ReactNode } from 'react';
import { AlertTriangle, Loader2, type LucideIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

export function Logo({ className, size = 'md' }: { className?: string; size?: 'sm' | 'md' | 'lg' }) {
  const s = size === 'lg' ? 'text-4xl' : size === 'sm' ? 'text-lg' : 'text-2xl';
  return (
    <span className={cn('inline-flex items-center font-display font-extrabold tracking-tight', s, className)} aria-label="VyroFlix">
      <svg viewBox="0 0 24 24" className="h-[1.05em] w-[1.05em] -mr-[0.05em]" aria-hidden>
        <defs>
          <linearGradient id="vf-g" x1="0" x2="1" y1="0" y2="1">
            <stop offset="0" stopColor="var(--brand-violet)" />
            <stop offset="1" stopColor="var(--brand-magenta)" />
          </linearGradient>
        </defs>
        <path d="M3 3 L21 3 L12 21 Z" fill="url(#vf-g)" />
        <path d="M9.5 7.5 L14.5 7.5 L12 12.5 Z" fill="var(--background)" opacity="0.9" />
      </svg>
      <span>YROFLIX</span>
    </span>
  );
}

export function MaturityBadge({ rating }: { rating: string }) {
  return (
    <span className="inline-flex items-center rounded border border-muted-foreground/50 px-1.5 py-px text-[11px] font-semibold text-muted-foreground">
      {rating}
    </span>
  );
}

export function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-muted-foreground" role="status">
      <Loader2 className="h-8 w-8 animate-spin text-primary" />
      {label && <p className="text-sm">{label}</p>}
    </div>
  );
}

export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
}: {
  icon: LucideIcon;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-4 py-20 text-center">
      <div className="grid h-20 w-20 place-items-center rounded-full bg-surface-elevated">
        <Icon className="h-9 w-9 text-muted-foreground" />
      </div>
      <h2 className="text-xl font-semibold">{title}</h2>
      {description && <p className="text-sm text-muted-foreground">{description}</p>}
      {action}
    </div>
  );
}

export function ErrorState({ message, onRetry, compact }: { message?: string; onRetry?: () => void; compact?: boolean }) {
  return (
    <div
      role="alert"
      className={cn(
        'flex items-center gap-4 rounded-xl border border-danger/30 bg-danger/10 text-sm',
        compact ? 'px-4 py-3' : 'mx-auto my-12 max-w-lg flex-col p-8 text-center'
      )}
    >
      <AlertTriangle className={cn('text-danger', compact ? 'h-5 w-5' : 'h-9 w-9')} />
      <p className="flex-1">{message ?? 'Something went wrong.'}</p>
      {onRetry && (
        <Button size="sm" variant="outline" onClick={onRetry}>
          Retry
        </Button>
      )}
    </div>
  );
}

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('shimmer rounded-xl', className)} />;
}

export function PosterGridSkeleton({ count = 12 }: { count?: number }) {
  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 2xl:grid-cols-7">
      {Array.from({ length: count }).map((_, i) => (
        <Skeleton key={i} className="aspect-[2/3]" />
      ))}
    </div>
  );
}
