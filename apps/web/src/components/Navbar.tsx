'use client';

import React, { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { Bell, Bookmark, Film, Home, LogIn, LogOut, Menu, Search, Server, Shield, Tv, User, X } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { Button } from '@/components/ui/button';
import { Logo } from '@/components/common';
import { cn } from '@/lib/utils';

const NAV_LINKS = [
  { label: 'Home', href: '/browse' },
  { label: 'Movies', href: '/movies' },
  { label: 'Series', href: '/series' },
  { label: 'My List', href: '/my-list' },
];

const NOTIFICATIONS = [
  {
    id: 'n1',
    title: 'Neon Tides now streaming',
    desc: 'The complete sci-fi thriller is ready to watch.',
    time: '2h ago',
    image: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=120&q=80',
    link: '/title/t-neon-tides',
  },
  {
    id: 'n2',
    title: 'Starfall Academy: New Episode',
    desc: 'Episode 2: Solar Flare has landed.',
    time: '1d ago',
    image: 'https://images.unsplash.com/photo-1516339901601-2e1b62dc0c45?w=120&q=80',
    link: '/title/t-starfall-academy',
  },
];

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const pathname = usePathname();
  const router = useRouter();
  const { user, isAuthed, isAdmin, signOut } = useAuth();

  const userMenuRef = useRef<HTMLDivElement>(null);
  const notifRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) setUserMenuOpen(false);
      if (notifRef.current && !notifRef.current.contains(e.target as Node)) setNotifOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setUserMenuOpen(false);
        setNotifOpen(false);
        setSearchOpen(false);
        setMobileMenuOpen(false);
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

  const handleSignOut = async () => {
    setUserMenuOpen(false);
    await signOut();
    router.push('/');
  };

  return (
    <header
      className={cn(
        'fixed inset-x-0 top-0 z-50 transition-all duration-300',
        scrolled ? 'glass border-x-0 border-t-0 shadow-lg' : 'bg-gradient-to-b from-background/95 to-transparent'
      )}
    >
      <nav className="mx-auto flex h-16 max-w-[1800px] items-center gap-6 px-4 sm:px-8 lg:px-12" aria-label="Main Navigation">
        {/* Brand Logo */}
        <Link href={isAuthed ? '/browse' : '/'} className="flex items-center gap-2 group transition-transform active:scale-95" aria-label="VyroFlix home">
          <Logo size="md" />
        </Link>

        {/* Primary Desktop Navigation */}
        {isAuthed && (
          <ul className="hidden md:flex items-center gap-6 text-sm font-medium">
            {NAV_LINKS.map((link) => {
              const active = pathname === link.href || (link.href !== '/browse' && pathname.startsWith(link.href));
              return (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className={cn(
                      'transition-colors py-1 hover:text-foreground',
                      active ? 'text-foreground font-semibold border-b-2 border-brand-violet' : 'text-muted-foreground'
                    )}
                  >
                    {link.label}
                  </Link>
                </li>
              );
            })}
          </ul>
        )}

        {/* Actions Area */}
        <div className="ml-auto flex items-center gap-2 sm:gap-3">
          {isAuthed ? (
            <>
              {/* Expandable Search Pill */}
              <form onSubmit={submitSearch} className="flex items-center">
                <div
                  className={cn(
                    'flex items-center overflow-hidden rounded-full transition-all duration-300',
                    searchOpen
                      ? 'w-48 sm:w-64 border border-border bg-surface/90 shadow-glow backdrop-blur'
                      : 'w-10'
                  )}
                >
                  <button
                    type="button"
                    aria-label="Search"
                    onClick={() => setSearchOpen((prev) => !prev)}
                    className="grid h-10 w-10 shrink-0 place-items-center rounded-full text-foreground hover:bg-surface-elevated transition-colors"
                  >
                    <Search className="h-4 w-4" />
                  </button>
                  {searchOpen && (
                    <>
                      <input
                        ref={searchInputRef}
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder="Search titles, cast…"
                        className="h-10 flex-1 bg-transparent pr-2 text-sm text-foreground outline-none placeholder:text-subtle"
                      />
                      <button
                        type="button"
                        aria-label="Close search"
                        onClick={() => setSearchOpen(false)}
                        className="pr-3 text-muted-foreground hover:text-foreground"
                      >
                        <X className="h-4 w-4" />
                      </button>
                    </>
                  )}
                </div>
              </form>

              {/* Notifications Dropdown */}
              <div className="relative" ref={notifRef}>
                <button
                  type="button"
                  aria-label="Notifications"
                  onClick={() => setNotifOpen((prev) => !prev)}
                  className="relative grid h-10 w-10 place-items-center rounded-full text-foreground hover:bg-surface-elevated transition-colors"
                >
                  <Bell className="h-4 w-4" />
                  <span className="absolute top-2.5 right-2.5 h-2 w-2 rounded-full bg-brand-magenta animate-pulse" />
                </button>

                {notifOpen && (
                  <div className="absolute right-0 mt-2 w-80 rounded-2xl border border-border bg-surface-elevated/95 p-3 shadow-card backdrop-blur-xl animate-page-in">
                    <p className="px-2 py-1 text-xs font-semibold uppercase tracking-wider text-subtle">Notifications</p>
                    <div className="mt-2 space-y-2">
                      {NOTIFICATIONS.map((item) => (
                        <Link
                          key={item.id}
                          href={item.link}
                          onClick={() => setNotifOpen(false)}
                          className="flex items-start gap-3 rounded-xl p-2 transition-colors hover:bg-surface"
                        >
                          <img src={item.image} alt="" className="h-10 w-14 rounded-lg object-cover" />
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-xs font-semibold text-foreground">{item.title}</p>
                            <p className="line-clamp-1 text-xs text-muted-foreground">{item.desc}</p>
                            <p className="mt-0.5 text-[10px] text-subtle">{item.time}</p>
                          </div>
                        </Link>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Profile Avatar Dropdown */}
              <div className="relative" ref={userMenuRef}>
                <button
                  type="button"
                  aria-label="Profile menu"
                  onClick={() => setUserMenuOpen((prev) => !prev)}
                  className="flex items-center gap-2 rounded-full ring-2 ring-brand-violet/50 hover:ring-brand-violet transition-all p-0.5"
                >
                  <div className="grid h-8 w-8 place-items-center rounded-full bg-gradient-brand text-xs font-bold text-white shadow-glow">
                    {user?.email ? user.email.slice(0, 1).toUpperCase() : 'V'}
                  </div>
                </button>

                {userMenuOpen && (
                  <div className="absolute right-0 mt-2 w-56 rounded-2xl border border-border bg-surface-elevated/95 p-2 shadow-card backdrop-blur-xl animate-page-in">
                    <div className="px-3 py-2 border-b border-border">
                      <p className="text-xs font-semibold text-foreground truncate">{user?.email}</p>
                      <span className="inline-block mt-1 text-[10px] font-semibold text-cyan uppercase tracking-wider">
                        {isAdmin ? 'Administrator' : 'Subscriber'}
                      </span>
                    </div>

                    <div className="py-1 text-sm">
                      <Link
                        href="/account"
                        onClick={() => setUserMenuOpen(false)}
                        className="flex items-center gap-2.5 rounded-xl px-3 py-2 text-foreground hover:bg-surface transition-colors"
                      >
                        <User className="h-4 w-4 text-cyan" />
                        Account & Plan
                      </Link>
                      <Link
                        href="/my-list"
                        onClick={() => setUserMenuOpen(false)}
                        className="flex items-center gap-2.5 rounded-xl px-3 py-2 text-foreground hover:bg-surface transition-colors"
                      >
                        <Bookmark className="h-4 w-4 text-brand-magenta" />
                        My Watchlist
                      </Link>
                      {isAdmin && (
                        <Link
                          href="/admin"
                          onClick={() => setUserMenuOpen(false)}
                          className="flex items-center gap-2.5 rounded-xl px-3 py-2 text-foreground hover:bg-surface transition-colors"
                        >
                          <Shield className="h-4 w-4 text-brand-violet" />
                          Admin Console
                        </Link>
                      )}
                    </div>

                    <div className="border-t border-border pt-1">
                      <button
                        type="button"
                        onClick={handleSignOut}
                        className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-sm text-danger hover:bg-danger/10 transition-colors"
                      >
                        <LogOut className="h-4 w-4" />
                        Sign Out
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="flex items-center gap-2">
              <Button variant="ghost" size="sm" asChild>
                <Link href="/browse">Guest</Link>
              </Button>
              <Button variant="gradient" size="sm" asChild>
                <Link href="/login">Sign In</Link>
              </Button>
            </div>
          )}
        </div>
      </nav>
    </header>
  );
}
