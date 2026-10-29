'use client';

import React, { type ReactNode } from 'react';
import Link from 'next/link';
import { Logo } from '@/components/common';

/**
 * Sign-in and register chrome: a quiet statement beside the form on large screens, the form alone on small ones.
 * No site navbar or footer; the only way out is the wordmark or "Browse as guest".
 */
export function AuthShell({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <div className="grid min-h-screen grid-cols-1 bg-background lg:grid-cols-[1fr_minmax(0,460px)]">
      <aside className="relative hidden flex-col justify-between border-r border-border p-12 lg:flex">
        <Link href="/" aria-label="VyroFlix home" className="w-fit">
          <Logo size="md" />
        </Link>
        <p className="max-w-md font-heading text-4xl leading-tight text-foreground">
          Pick a film, press play, and pick up where you stopped on any screen.
        </p>
        <p className="text-sm text-subtle">Streaming in HLS, delivered from the edge.</p>
      </aside>

      <div className="flex flex-col px-6 py-8 sm:px-12 lg:px-16">
        <div className="flex items-center justify-between lg:justify-end">
          <Link href="/" aria-label="VyroFlix home" className="lg:hidden">
            <Logo size="sm" />
          </Link>
          <Link href="/browse" className="text-sm text-muted-foreground hover:text-foreground">
            Browse as guest
          </Link>
        </div>

        <div className="my-auto w-full max-w-sm self-center py-12">
          <h1 className="text-3xl text-foreground">{title}</h1>
          {subtitle && <p className="mt-2 text-sm text-muted-foreground">{subtitle}</p>}
          <div className="mt-8">{children}</div>
        </div>
      </div>
    </div>
  );
}

export function GoogleButton({ onClick, disabled }: { onClick: () => void; disabled?: boolean }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className="flex h-11 w-full items-center justify-center gap-3 rounded-card border border-border bg-surface text-sm font-medium text-foreground transition-colors hover:border-primary disabled:opacity-50"
    >
      <svg viewBox="0 0 48 48" className="h-5 w-5" aria-hidden>
        <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3 0 5.8 1.1 7.9 3l5.7-5.7C34 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.4-.4-3.5z" />
        <path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.7 15.1 19 12 24 12c3 0 5.8 1.1 7.9 3l5.7-5.7C34 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z" />
        <path fill="#4CAF50" d="M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2C29.2 35.1 26.7 36 24 36c-5.2 0-9.6-3.3-11.3-7.9l-6.5 5C9.5 39.6 16.2 44 24 44z" />
        <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.2-2.2 4.2-4.1 5.6l6.2 5.2C37 39.2 44 34 44 24c0-1.3-.1-2.4-.4-3.5z" />
      </svg>
      Continue with Google
    </button>
  );
}

export function Divider() {
  return (
    <div className="my-6 flex items-center gap-3 text-xs text-subtle">
      <span className="h-px flex-1 bg-border" />
      <span>or</span>
      <span className="h-px flex-1 bg-border" />
    </div>
  );
}

export function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return <p role="alert" className="mt-1.5 text-xs font-medium text-danger">{message}</p>;
}

export const inputClass =
  'h-11 w-full rounded-card border border-border bg-surface px-3.5 text-sm text-foreground placeholder:text-subtle transition-colors focus:border-primary focus:outline-none aria-[invalid=true]:border-danger';
