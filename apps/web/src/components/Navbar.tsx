'use client';

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { Search, Bell, User, Play, Bookmark, LogIn, LogOut, Shield } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);

  const pathname = usePathname();
  const router = useRouter();
  const { user, isAuthed, isAdmin, signOut } = useAuth();

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      router.push(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    } else {
      router.push('/search');
    }
  };

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 30);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setUserMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const navLinks = [
    { label: 'Home', href: '/browse' },
    { label: 'Movies', href: '/browse?type=MOVIE' },
    { label: 'Series', href: '/browse?type=SERIES' },
    { label: 'My List', href: '/my-list' },
    ...(isAdmin ? [{ label: 'Studio', href: '/admin', adminOnly: true }] : []),
  ];

  return (
    <nav
      className={cn(
        'fixed top-0 left-0 right-0 z-50 transition-all duration-300',
        scrolled ? 'glass-nav py-3' : 'bg-gradient-to-b from-background/95 via-background/60 to-transparent py-5'
      )}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between">
        {/* Left: Brand & Navigation */}
        <div className="flex items-center space-x-8">
          <Link href="/browse" className="flex items-center space-x-2 group">
            <div className="w-8 h-8 rounded-lg bg-gradient-brand flex items-center justify-center shadow-glow group-hover:scale-105 transition-transform">
              <Play className="w-4 h-4 text-white fill-white ml-0.5" />
            </div>
            <span className="font-heading font-extrabold text-xl tracking-wider text-white">
              VYRO<span className="text-gradient-brand">FLIX</span>
            </span>
          </Link>

          <div className="hidden md:flex items-center space-x-6 text-sm font-medium">
            {navLinks.map((link) => {
              const isActive = pathname === link.href;
              return (
                <Link
                  key={link.label}
                  href={link.href}
                  className={cn(
                    'transition-colors hover:text-white flex items-center gap-1.5',
                    isActive ? 'text-white font-semibold' : 'text-text-secondary'
                  )}
                >
                  {link.label}
                  {link.adminOnly && (
                    <span className="rounded bg-cyan/20 px-1 py-0.2 text-[9px] font-bold text-cyan">
                      ADMIN
                    </span>
                  )}
                </Link>
              );
            })}
          </div>
        </div>

        {/* Right: Search, Notifications, Profile / Login */}
        <div className="flex items-center space-x-4 sm:space-x-5">
          {/* Search Bar */}
          <div className="relative flex items-center">
            {searchOpen ? (
              <form onSubmit={handleSearchSubmit} className="flex items-center bg-surface-elevated/90 border border-border rounded-full px-3 py-1.5 focus-within:border-cyan transition-all">
                <Search className="w-4 h-4 text-text-muted mr-2" />
                <input
                  type="text"
                  placeholder="Titles, people, genres..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="bg-transparent text-sm text-foreground focus:outline-none w-36 sm:w-56"
                  autoFocus
                  onBlur={() => !searchQuery && setSearchOpen(false)}
                />
              </form>
            ) : (
              <button
                onClick={() => {
                  setSearchOpen(true);
                  if (pathname !== '/search') {
                    router.push('/search');
                  }
                }}
                className="text-text-secondary hover:text-foreground transition-colors p-1"
                aria-label="Search"
              >
                <Search className="w-5 h-5" />
              </button>
            )}
          </div>

          {/* Watchlist shortcut */}
          <Link
            href="/my-list"
            className="text-text-secondary hover:text-foreground transition-colors p-1 relative hidden sm:block"
            aria-label="My Watchlist"
          >
            <Bookmark className="w-5 h-5" />
          </Link>

          {/* Notifications */}
          <button
            className="text-text-secondary hover:text-foreground transition-colors p-1 relative hidden sm:block"
            aria-label="Notifications"
          >
            <Bell className="w-5 h-5" />
            <span className="absolute top-1 right-1 w-2 h-2 rounded-full bg-primary-magenta" />
          </button>

          {/* User Auth Profile / Login Button */}
          {isAuthed ? (
            <div className="relative" ref={userMenuRef}>
              <button
                onClick={() => setUserMenuOpen((o) => !o)}
                className="flex items-center gap-2 rounded-full border border-border bg-surface-elevated p-1 hover:border-cyan transition-all cursor-pointer"
                aria-label="User menu"
              >
                <div className="w-7 h-7 rounded-full bg-gradient-brand flex items-center justify-center text-xs font-bold text-white uppercase">
                  {user?.email ? user.email.slice(0, 2) : <User className="w-4 h-4" />}
                </div>
              </button>

              {userMenuOpen && (
                <div className="absolute right-0 mt-2 w-56 rounded-xl glass shadow-card p-2 border border-border text-sm z-50 animate-page-in">
                  <div className="px-3 py-2 border-b border-border/60">
                    <p className="font-semibold text-foreground truncate">{user?.email}</p>
                    <p className="text-xs text-text-muted capitalize">Role: {isAdmin ? 'Administrator' : 'Viewer'}</p>
                  </div>

                  <div className="py-1">
                    <Link
                      href="/my-list"
                      onClick={() => setUserMenuOpen(false)}
                      className="flex items-center gap-2 px-3 py-2 text-text-secondary hover:text-foreground hover:bg-surface-elevated rounded-lg transition-colors"
                    >
                      <Bookmark className="w-4 h-4" /> My List
                    </Link>

                    {isAdmin && (
                      <Link
                        href="/admin"
                        onClick={() => setUserMenuOpen(false)}
                        className="flex items-center gap-2 px-3 py-2 text-cyan hover:bg-surface-elevated rounded-lg transition-colors"
                      >
                        <Shield className="w-4 h-4" /> Studio Dashboard
                      </Link>
                    )}
                  </div>

                  <div className="pt-1 border-t border-border/60">
                    <button
                      onClick={async () => {
                        setUserMenuOpen(false);
                        await signOut();
                        router.push('/login');
                      }}
                      className="w-full flex items-center gap-2 px-3 py-2 text-danger hover:bg-danger/10 rounded-lg transition-colors cursor-pointer text-left"
                    >
                      <LogOut className="w-4 h-4" /> Sign out
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <Button
              variant="gradient"
              size="sm"
              asChild
              className="font-semibold shadow-glow"
            >
              <Link href="/login" className="flex items-center gap-1.5">
                <LogIn className="w-4 h-4" />
                <span>Sign in</span>
              </Link>
            </Button>
          )}
        </div>
      </div>
    </nav>
  );
}
