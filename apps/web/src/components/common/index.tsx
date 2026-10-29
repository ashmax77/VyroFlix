import React, { type ReactNode } from 'react';
import { AlertTriangle, Loader2, type LucideIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

/** Signature Lovable geometric gradient emblem with modern Outfit typography */
export function Logo({ className, size = 'md' }: { className?: string; size?: 'sm' | 'md' | 'lg' }) {
  const s = size === 'lg' ? 'text-4xl' : size === 'sm' ? 'text-lg' : 'text-2xl';
  return (
    <span className={cn('inline-flex items-center font-display font-extrabold tracking-tight select-none', s, className)} aria-label="VyroFlix">
      <svg viewBox="0 0 24 24" className="h-[1.05em] w-[1.05em] -mr-[0.05em]" aria-hidden>
        <defs>
          <linearGradient id="vf-g" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="rgb(var(--brand-violet))" />
            <stop offset="100%" stopColor="rgb(var(--brand-magenta))" />
          </linearGradient>
        </defs>
        <path d="M3 3 L21 3 L12 21 Z" fill="url(#vf-g)" />
        <path d="M9.5 7.5 L14.5 7.5 L12 12.5 Z" fill="rgb(var(--background))" opacity="0.9" />
      </svg>
      <span className="ml-1 tracking-wider text-foreground">YROFLIX</span>
    </span>
  );
}

export function MaturityBadge({ rating }: { rating: string }) {
  if (!rating) return null;
  return (
    <span className="inline-flex items-center rounded border border-muted-foreground/40 px-1.5 py-px text-[11px] font-semibold text-muted-foreground">
      {rating}
    </span>
  );
}

export function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-muted-foreground" role="status">
      <Loader2 className="h-7 w-7 animate-spin text-cyan" aria-hidden />
      {label && <p className="text-sm font-medium">{label}</p>}
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
    <div className="mx-auto flex max-w-md flex-col items-center gap-4 py-20 text-center">
      {Icon && (
        <div className="grid h-16 w-16 place-items-center rounded-2xl bg-surface-elevated text-cyan">
          <Icon className="h-8 w-8" aria-hidden />
        </div>
      )}
      <h2 className="text-2xl font-bold text-foreground">{title}</h2>
      {description && <p className="text-sm leading-relaxed text-muted-foreground">{description}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}

export function ErrorState({ message, onRetry, compact }: { message?: string; onRetry?: () => void; compact?: boolean }) {
  return (
    <div
      role="alert"
      className={cn(
        'flex gap-4 rounded-xl border border-danger/30 bg-danger/10',
        compact ? 'items-center px-4 py-3 text-sm' : 'mx-auto max-w-lg flex-col items-center p-6 text-center'
      )}
    >
      <AlertTriangle className="h-6 w-6 shrink-0 text-danger" aria-hidden />
      <p className="flex-1 text-foreground">{message ?? 'Something went wrong. Try again.'}</p>
      {onRetry && (
        <Button size="sm" variant="outline" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  );
}

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('shimmer rounded-xl', className)} aria-hidden />;
}

export function PosterGridSkeleton({ count = 12 }: { count?: number }) {
  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-7">
      {Array.from({ length: count }).map((_, i) => (
        <Skeleton key={i} className="aspect-[2/3] rounded-xl" />
      ))}
    </div>
  );
}

export function PosterGrid({ children }: { children: ReactNode }) {
  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-7">
      {children}
    </div>
  );
}

export function PageContainer({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn('mx-auto w-full max-w-[1800px] px-4 sm:px-8 lg:px-12', className)}>{children}</div>;
}

export { Logo as VyroFlixLogo };

