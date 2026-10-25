'use client';

import React, { type ReactNode } from 'react';
import Link from 'next/link';
import { Logo } from '@/components/common';

const POSTER_SAMPLES = [
  'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=300&auto=format&fit=crop&q=70',
  'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=300&auto=format&fit=crop&q=70',
  'https://images.unsplash.com/photo-1682687220063-4742bd7fd538?w=300&auto=format&fit=crop&q=70',
  'https://images.unsplash.com/photo-1578632767115-351597cf2477?w=300&auto=format&fit=crop&q=70',
  'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300&auto=format&fit=crop&q=70',
  'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=300&auto=format&fit=crop&q=70',
  'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=300&auto=format&fit=crop&q=70',
  'https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=300&auto=format&fit=crop&q=70',
];

export function AuthShell({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <div className="relative flex min-h-screen flex-col overflow-hidden bg-background">
      {/* Background Poster Wall with subtle opacity and overlay */}
      <div aria-hidden className="absolute inset-0 grid grid-cols-4 gap-2 opacity-20 sm:grid-cols-8 pointer-events-none">
        {Array.from({ length: 24 }).map((_, idx) => (
          <img
            key={idx}
            src={POSTER_SAMPLES[idx % POSTER_SAMPLES.length]}
            alt=""
            className="aspect-[2/3] w-full object-cover filter brightness-75"
          />
        ))}
      </div>
      <div className="absolute inset-0 bg-gradient-to-t from-background via-background/85 to-background/70 pointer-events-none" />

      {/* Header */}
      <header className="relative z-10 px-6 py-6 sm:px-12 flex items-center justify-between">
        <Link href="/" aria-label="VyroFlix home" className="hover:opacity-90 transition-opacity">
          <Logo size="md" />
        </Link>
        <Link href="/browse" className="text-sm font-medium text-text-secondary hover:text-foreground transition-colors">
          Browse as guest →
        </Link>
      </header>

      {/* Centered Glass Container */}
      <div className="relative z-10 flex flex-1 items-center justify-center px-4 pb-16">
        <div className="glass w-full max-w-md rounded-2xl p-8 shadow-card sm:p-10 border border-border/80">
          <h1 className="text-3xl font-extrabold tracking-tight text-foreground">{title}</h1>
          {subtitle && <p className="mt-2 text-sm text-text-secondary">{subtitle}</p>}
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
      className="flex h-11 w-full items-center justify-center gap-3 rounded-full border border-border bg-surface-elevated text-sm font-semibold transition-all hover:bg-surface-elevated/70 hover:border-text-secondary/40 active:scale-[0.99] disabled:opacity-50"
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
    <div className="my-6 flex items-center gap-3 text-xs uppercase tracking-wider text-subtle">
      <span className="h-px flex-1 bg-border" /> or <span className="h-px flex-1 bg-border" />
    </div>
  );
}

export function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return <p role="alert" className="mt-1.5 text-xs text-danger font-medium">{message}</p>;
}

export const inputClass =
  'h-11 w-full rounded-xl border border-border bg-surface/90 px-4 text-sm text-foreground placeholder:text-subtle focus:border-cyan focus:outline-none transition-colors aria-[invalid=true]:border-danger';
