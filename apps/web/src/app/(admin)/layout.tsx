'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import {
  ExternalLink,
  Film,
  Layers,
  LayoutDashboard,
  PanelLeftClose,
  PanelLeftOpen,
  ShieldAlert,
  Tags,
  Upload,
} from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { Button } from '@/components/ui/button';
import { EmptyState, Spinner, VyroFlixLogo } from '@/components/common';
import { cn } from '@/lib/utils';

const SECTIONS = [
  { href: '/admin', label: 'Dashboard', icon: LayoutDashboard, exact: true },
  { href: '/admin/titles', label: 'Titles', icon: Film, exact: false },
  { href: '/admin/uploads', label: 'Uploads & Encoding', icon: Upload, exact: false },
  { href: '/admin/collections', label: 'Collections', icon: Layers, exact: false },
  { href: '/admin/genres', label: 'Genres', icon: Tags, exact: false },
] as const;

/**
 * Studio shell with collapsible sidebar, active brand gradient highlight,
 * breadcrumb header, and quick link to viewer site.
 */
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { isAuthed, isAdmin, loading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    if (!loading && !isAuthed) router.replace('/login?redirect=/admin');
  }, [loading, isAuthed, router]);

  if (loading || !isAuthed) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <Spinner label="Checking studio credentials…" />
      </div>
    );
  }

  if (!isAdmin) {
    return (
      <div className="mx-auto max-w-container px-4 py-24 sm:px-6 lg:px-10">
        <EmptyState
          icon={ShieldAlert}
          title="Studio Administrator Access Required"
          description="Your account has standard viewer privileges. Studio administration requires an ADMIN role."
          action={
            <Button variant="gradient" asChild>
              <Link href="/browse">Back to browse</Link>
            </Button>
          }
        />
      </div>
    );
  }

  const crumbs = pathname.split('/').filter(Boolean);

  return (
    <div className="flex min-h-screen bg-background">
      {/* Desktop Collapsible Sidebar */}
      <aside
        className={cn(
          'sticky top-0 hidden h-screen shrink-0 flex-col border-r border-border bg-surface transition-all duration-300 md:flex',
          collapsed ? 'w-20' : 'w-64',
        )}
      >
        <div className="flex h-16 items-center justify-between px-4">
          <Link href="/admin" className="flex items-center gap-2 overflow-hidden">
            {collapsed ? (
              <span className="font-display text-xl font-black text-cyan">V</span>
            ) : (
              <VyroFlixLogo size="sm" />
            )}
          </Link>
        </div>

        <nav aria-label="Studio navigation" className="flex-1 space-y-1.5 px-3 py-4">
          {SECTIONS.map((s) => {
            const active = s.exact ? pathname === s.href : pathname.startsWith(s.href);
            return (
              <Link
                key={s.href}
                href={s.href}
                title={s.label}
                aria-current={active ? 'page' : undefined}
                className={cn(
                  'flex items-center gap-3.5 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-200',
                  active
                    ? 'bg-gradient-brand text-white shadow-glow'
                    : 'text-muted-foreground hover:bg-surface-elevated hover:text-foreground',
                )}
              >
                <s.icon className="h-4 w-4 shrink-0" />
                {!collapsed && <span>{s.label}</span>}
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-border p-3">
          <button
            type="button"
            onClick={() => setCollapsed((c) => !c)}
            aria-label="Toggle sidebar width"
            className="flex h-10 w-full items-center justify-center rounded-xl text-subtle transition-colors hover:bg-surface-elevated hover:text-foreground"
          >
            {collapsed ? <PanelLeftOpen className="h-4 w-4" /> : <PanelLeftClose className="h-4 w-4" />}
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="min-w-0 flex-1">
        {/* Sticky Header Bar */}
        <header className="glass sticky top-0 z-30 flex h-16 items-center justify-between border-x-0 border-t-0 px-4 sm:px-8">
          <nav aria-label="Breadcrumb" className="flex items-center gap-2 text-xs text-muted-foreground sm:text-sm">
            {crumbs.map((c, i) => (
              <span key={i} className="flex items-center gap-2">
                {i > 0 && <span className="text-subtle">/</span>}
                <span className={cn('capitalize', i === crumbs.length - 1 && 'font-medium text-foreground')}>
                  {c}
                </span>
              </span>
            ))}
          </nav>

          {/* Mobile section dropdown */}
          <select
            aria-label="Studio section"
            className="ml-2 rounded-lg border border-border bg-surface px-2.5 py-1.5 text-xs text-foreground md:hidden"
            value={pathname}
            onChange={(e) => router.push(e.target.value)}
          >
            {SECTIONS.map((s) => (
              <option key={s.href} value={s.href}>
                {s.label}
              </option>
            ))}
          </select>

          <Link
            href="/browse"
            className="flex items-center gap-1.5 text-xs font-medium text-cyan hover:underline sm:text-sm"
          >
            View site <ExternalLink className="h-3.5 w-3.5" />
          </Link>
        </header>

        {/* Studio Content Stage */}
        <main className="p-4 sm:p-6 lg:p-8">{children}</main>
      </div>
    </div>
  );
}
