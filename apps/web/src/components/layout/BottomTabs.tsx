'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Home, Search, Bookmark, User } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { cn } from '@/lib/utils';

export function BottomTabs() {
  const { isAuthed } = useAuth();
  const pathname = usePathname();

  if (!isAuthed) return null;

  const tabs = [
    { href: '/browse', label: 'Home', icon: Home },
    { href: '/search', label: 'Search', icon: Search },
    { href: '/my-list', label: 'My List', icon: Bookmark },
    { href: '/account', label: 'Account', icon: User },
  ] as const;

  return (
    <nav
      aria-label="Mobile Navigation"
      className="glass fixed inset-x-0 bottom-0 z-50 grid grid-cols-4 border-x-0 border-b-0 py-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] md:hidden shadow-card"
    >
      {tabs.map(({ href, label, icon: Icon }) => {
        const active = pathname === href || (href !== '/browse' && pathname.startsWith(href));
        return (
          <Link
            key={href}
            href={href}
            className={cn(
              'flex flex-col items-center justify-center gap-1 py-1 text-[11px] font-medium transition-colors',
              active ? 'text-foreground font-semibold' : 'text-subtle hover:text-foreground'
            )}
          >
            <Icon className={cn('h-5 w-5 transition-transform', active && 'text-cyan scale-110')} />
            <span>{label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
