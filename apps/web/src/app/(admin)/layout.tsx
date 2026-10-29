'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { ShieldAlert } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { Button } from '@/components/ui/button';
import { EmptyState, Spinner } from '@/components/common';
import { cn } from '@/lib/utils';

const SECTIONS = [
  { href: '/admin', label: 'Overview', exact: true },
  { href: '/admin/titles', label: 'Titles', exact: false },
  { href: '/admin/collections', label: 'Collections', exact: false },
  { href: '/admin/genres', label: 'Genres', exact: false },
  { href: '/admin/uploads', label: 'Uploads', exact: false },
] as const;

/**
 * Studio shell. Gates every admin route on the signed-in role; the API enforces the same rule on each call.
 */
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { isAuthed, isAdmin, loading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!loading && !isAuthed) router.replace('/login?redirect=/admin');
  }, [loading, isAuthed, router]);

  if (loading || !isAuthed) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <Spinner label="Checking access…" />
      </div>
    );
  }

  if (!isAdmin) {
    return (
      <div className="mx-auto max-w-container px-4 py-24 sm:px-6 lg:px-10">
        <EmptyState
          icon={ShieldAlert}
          title="This area is for studio administrators"
          description="Your account can watch and keep a list. It can't manage titles or uploads."
          action={
            <Button variant="secondary" asChild>
              <Link href="/browse">Back to browse</Link>
            </Button>
          }
        />
      </div>
    );
  }

  return (
    <main className="mx-auto max-w-container px-4 pb-16 pt-6 sm:px-6 lg:px-10">
      <div className="flex flex-col gap-8 md:flex-row md:gap-12">
        <aside className="md:w-44 md:shrink-0">
          <p className="mb-3 hidden text-xs font-medium uppercase tracking-wide text-subtle md:block">Studio</p>
          <nav aria-label="Studio sections" className="-mx-4 overflow-x-auto px-4 md:mx-0 md:px-0">
            <ul className="flex gap-1 md:flex-col md:gap-0.5">
              {SECTIONS.map((s) => {
                const active = s.exact ? pathname === s.href : pathname.startsWith(s.href);
                return (
                  <li key={s.href}>
                    <Link
                      href={s.href}
                      aria-current={active ? 'page' : undefined}
                      className={cn(
                        'block whitespace-nowrap rounded-card px-3 py-2 text-sm transition-colors',
                        active ? 'bg-surface text-foreground' : 'text-muted-foreground hover:text-foreground',
                      )}
                    >
                      {s.label}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </nav>
        </aside>
        <div className="min-w-0 flex-1">{children}</div>
      </div>
    </main>
  );
}
