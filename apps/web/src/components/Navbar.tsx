'use client';

import React, { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { LogIn, LogOut, Menu, Search, Shield, User, X } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type NavLink = { label: string; href: string; match: (path: string, type: string | null) => boolean };

const LINKS: NavLink[] = [
  { label: 'Home', href: '/browse', match: (p, t) => p === '/browse' && !t },
  { label: 'Movies', href: '/browse?type=MOVIE', match: (p, t) => p === '/browse' && t === 'MOVIE' },
  { label: 'Series', href: '/browse?type=SERIES', match: (p, t) => p === '/browse' && t === 'SERIES' },
  { label: 'My list', href: '/my-list', match: (p) => p === '/my-list' },
];

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  // Read the `type` filter from the URL on the client; avoids useSearchParams' Suspense requirement on static pages.
  const [typeParam, setTypeParam] = useState<string | null>(null);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  const pathname = usePathname();
  const router = useRouter();
  const { user, isAuthed, isAdmin, signOut } = useAuth();

  useEffect(() => {
    setTypeParam(new URLSearchParams(window.location.search).get('type'));
    setMobileOpen(false);
    setUserMenuOpen(false);
  }, [pathname]);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) setUserMenuOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setUserMenuOpen(false);
        setMobileOpen(false);
        setSearchOpen(false);
      }
    };
    document.addEventListener('mousedown', onClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onClick);
      document.removeEventListener('keydown', onKey);
    };
  }, []);

  useEffect(() => {
    if (searchOpen) searchInputRef.current?.focus();
  }, [searchOpen]);

  const submitSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const q = searchQuery.trim();
    router.push(q ? `/search?q=${encodeURIComponent(q)}` : '/search');
    setSearchOpen(false);
  };

  const signOutNow = async () => {
    setUserMenuOpen(false);
    setMobileOpen(false);
    await signOut();
    router.push('/login');
  };

  const initials = user?.email ? user.email.slice(0, 2).toUpperCase() : '';

  return (
    <header
      className={cn(
        'fixed inset-x-0 top-0 z-50 transition-[background-color,border-color] duration-300',
        scrolled || mobileOpen ? 'border-b border-border bg-background/95 backdrop-blur-sm' : 'border-b border-transparent bg-transparent'
      )}
    >
      <nav aria-label="Main" className="mx-auto flex h-nav max-w-container items-center gap-6 px-4 sm:px-6 lg:px-10">
        <Link href="/browse" className="font-heading text-xl font-semibold tracking-tight text-foreground" aria-label="VyroFlix home">
          VyroFlix
        </Link>

        <ul className="hidden items-center gap-6 text-sm md:flex">
          {LINKS.map((link) => {
            const active = link.match(pathname, typeParam);
            return (
              <li key={link.label}>
                <Link
                  href={link.href}
                  aria-current={active ? 'page' : undefined}
                  className={cn(
                    'border-b-2 pb-1 transition-colors',
                    active ? 'border-primary text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground'
                  )}
                >
                  {link.label}
                </Link>
              </li>
            );
          })}
          {isAdmin && (
            <li>
              <Link
                href="/admin"
                aria-current={pathname.startsWith('/admin') ? 'page' : undefined}
                className={cn(
                  'border-b-2 pb-1 transition-colors',
                  pathname.startsWith('/admin') ? 'border-primary text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground'
                )}
              >
                Studio
              </Link>
            </li>
          )}
        </ul>

        <div className="ml-auto flex items-center gap-3 sm:gap-4">
          {/* Search: expands in place; submitting goes to /search */}
          {searchOpen ? (
            <form onSubmit={submitSearch} role="search" className="flex items-center gap-2">
              <input
                ref={searchInputRef}
                type="search"
                name="q"
                aria-label="Search titles"
                placeholder="Search titles"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onBlur={() => !searchQuery.trim() && setSearchOpen(false)}
                className="h-9 w-40 rounded-card border border-border bg-surface px-3 text-sm text-foreground placeholder:text-subtle focus:border-primary focus:outline-none sm:w-64"
              />
            </form>
          ) : (
            <button
              type="button"
              onClick={() => setSearchOpen(true)}
              className="grid h-9 w-9 place-items-center rounded-card text-muted-foreground transition-colors hover:text-foreground"
              aria-label="Search"
            >
              <Search className="h-5 w-5" aria-hidden />
            </button>
          )}

          {isAuthed ? (
            <div className="relative" ref={userMenuRef}>
              <button
                type="button"
                onClick={() => setUserMenuOpen((o) => !o)}
                aria-expanded={userMenuOpen}
                aria-haspopup="menu"
                aria-label="Account menu"
                className="grid h-9 w-9 place-items-center rounded-full border border-border bg-surface text-xs font-semibold text-foreground transition-colors hover:border-primary"
              >
                {initials || <User className="h-4 w-4" aria-hidden />}
              </button>

              {userMenuOpen && (
                <div role="menu" className="panel absolute right-0 mt-2 w-60 p-2 text-sm shadow-none">
                  <div className="border-b border-border px-3 py-2">
                    <p className="truncate font-medium text-foreground">{user?.email}</p>
                    <p className="text-xs text-subtle">{isAdmin ? 'Studio administrator' : 'Viewer'}</p>
                  </div>
                  <div className="py-1">
                    <Link role="menuitem" href="/account" className="block rounded-card px-3 py-2 text-muted-foreground hover:bg-surface-elevated hover:text-foreground">
                      Account
                    </Link>
                    <Link role="menuitem" href="/my-list" className="block rounded-card px-3 py-2 text-muted-foreground hover:bg-surface-elevated hover:text-foreground">
                      My list
                    </Link>
                    {isAdmin && (
                      <Link role="menuitem" href="/admin" className="flex items-center gap-2 rounded-card px-3 py-2 text-muted-foreground hover:bg-surface-elevated hover:text-foreground">
                        <Shield className="h-4 w-4" aria-hidden /> Studio
                      </Link>
                    )}
                  </div>
                  <div className="border-t border-border pt-1">
                    <button
                      type="button"
                      role="menuitem"
                      onClick={signOutNow}
                      className="flex w-full items-center gap-2 rounded-card px-3 py-2 text-left text-danger hover:bg-danger/10"
                    >
                      <LogOut className="h-4 w-4" aria-hidden /> Sign out
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <Button variant="default" size="sm" asChild className="hidden sm:inline-flex">
              <Link href="/login">
                <LogIn className="h-4 w-4" aria-hidden />
                <span>Sign in</span>
              </Link>
            </Button>
          )}

          {/* Mobile menu toggle */}
          <button
            type="button"
            onClick={() => setMobileOpen((o) => !o)}
            aria-expanded={mobileOpen}
            aria-controls="mobile-menu"
            aria-label={mobileOpen ? 'Close menu' : 'Open menu'}
            className="grid h-9 w-9 place-items-center rounded-card text-muted-foreground transition-colors hover:text-foreground md:hidden"
          >
            {mobileOpen ? <X className="h-5 w-5" aria-hidden /> : <Menu className="h-5 w-5" aria-hidden />}
          </button>
        </div>
      </nav>

      {/* Mobile sheet: every destination reachable without hover */}
      {mobileOpen && (
        <div id="mobile-menu" className="border-t border-border bg-background md:hidden">
          <ul className="flex flex-col px-4 py-3 text-base">
            {LINKS.map((link) => (
              <li key={link.label}>
                <Link
                  href={link.href}
                  aria-current={link.match(pathname, typeParam) ? 'page' : undefined}
                  className="block border-l-2 border-transparent px-3 py-3 text-muted-foreground aria-[current=page]:border-primary aria-[current=page]:text-foreground"
                >
                  {link.label}
                </Link>
              </li>
            ))}
            {isAdmin && (
              <li>
                <Link href="/admin" className="block border-l-2 border-transparent px-3 py-3 text-muted-foreground">
                  Studio
                </Link>
              </li>
            )}
            <li>
              {isAuthed ? (
                <button type="button" onClick={signOutNow} className="block w-full px-3 py-3 text-left text-danger">
                  Sign out
                </button>
              ) : (
                <Link href="/login" className="block px-3 py-3 text-primary">
                  Sign in
                </Link>
              )}
            </li>
          </ul>
        </div>
      )}
    </header>
  );
}
