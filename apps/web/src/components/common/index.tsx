import React, { type ReactNode } from 'react';
import { AlertTriangle, Loader2, type LucideIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

/** Wordmark. One text node, so tests and screen readers see a single brand name. */
export function Logo({ className, size = 'md' }: { className?: string; size?: 'sm' | 'md' | 'lg' }) {
  const s = size === 'lg' ? 'text-4xl' : size === 'sm' ? 'text-lg' : 'text-xl';
  return <span className={cn('font-heading font-semibold tracking-tight text-foreground', s, className)}>VyroFlix</span>;
}

export function MaturityBadge({ rating }: { rating: string }) {
  if (!rating) return null;
  return (
    <span className="inline-flex items-center rounded-[2px] border border-muted-foreground/40 px-1.5 py-px text-[11px] font-medium text-muted-foreground">
      {rating}
    </span>
  );
}

export function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-muted-foreground" role="status">
      <Loader2 className="h-6 w-6 animate-spin text-primary" aria-hidden />
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
  icon?: LucideIcon;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-3 py-16 text-center">
      {Icon && <Icon className="h-8 w-8 text-muted-foreground" aria-hidden />}
      <h2 className="text-xl text-foreground">{title}</h2>
      {description && <p className="text-sm text-muted-foreground">{description}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}

/** Error with a plain-language message and a retry action. Messages say what failed, not that something did. */
export function ErrorState({ message, onRetry, compact }: { message?: string; onRetry?: () => void; compact?: boolean }) {
  return (
    <div
      role="alert"
      className={cn(
        'flex gap-4 rounded-card border border-danger/40 bg-danger/5',
        compact ? 'items-center px-4 py-3 text-sm' : 'mx-auto max-w-lg flex-col items-start p-6 sm:flex-row sm:items-center'
      )}
    >
      <AlertTriangle className="h-5 w-5 shrink-0 text-danger" aria-hidden />
      <p className="flex-1 text-foreground">{message ?? "Something went wrong. Try again."}</p>
      {onRetry && (
        <Button size="sm" variant="outline" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  );
}

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('shimmer rounded-card', className)} aria-hidden />;
}

export function PosterGridSkeleton({ count = 12 }: { count?: number }) {
  return (
    <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
      {Array.from({ length: count }).map((_, i) => (
        <Skeleton key={i} className="aspect-[2/3] rounded-[2px]" />
      ))}
    </div>
  );
}
