'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Search, Bell, User, Play, Bookmark } from 'lucide-react';

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const pathname = usePathname();

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 40);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const navLinks = [
    { label: 'Home', href: '/browse' },
    { label: 'Movies', href: '/browse?type=MOVIE' },
    { label: 'Series', href: '/browse?type=SERIES' },
    { label: 'My List', href: '/my-list' },
  ];

  return (
    <nav
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        scrolled ? 'glass-nav py-3' : 'bg-gradient-to-b from-[#0A0A0F]/90 to-transparent py-5'
      }`}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between">
        {/* Left: Brand & Navigation */}
        <div className="flex items-center space-x-8">
          <Link href="/browse" className="flex items-center space-x-2 group">
            <div className="w-8 h-8 rounded-lg gradient-accent flex items-center justify-center shadow-lg shadow-purple-500/20 group-hover:scale-105 transition-transform">
              <Play className="w-4 h-4 text-white fill-white ml-0.5" />
            </div>
            <span className="font-heading font-extrabold text-xl tracking-wider text-white">
              VYRO<span className="gradient-text">FLIX</span>
            </span>
          </Link>

          <div className="hidden md:flex items-center space-x-6 text-sm font-medium">
            {navLinks.map((link) => {
              const isActive = pathname === link.href;
              return (
                <Link
                  key={link.label}
                  href={link.href}
                  className={`transition-colors hover:text-white ${
                    isActive ? 'text-white font-semibold' : 'text-text-secondary'
                  }`}
                >
                  {link.label}
                </Link>
              );
            })}
          </div>
        </div>

        {/* Right: Search, Notifications, Profile */}
        <div className="flex items-center space-x-5">
          {/* Search Bar */}
          <div className="relative flex items-center">
            {searchOpen ? (
              <div className="flex items-center bg-surface-elevated/90 border border-border rounded-full px-3 py-1.5 focus-within:border-accent-cyan transition-all">
                <Search className="w-4 h-4 text-text-muted mr-2" />
                <input
                  type="text"
                  placeholder="Titles, people, genres..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="bg-transparent text-sm text-text-primary focus:outline-none w-36 sm:w-56"
                  autoFocus
                  onBlur={() => !searchQuery && setSearchOpen(false)}
                />
              </div>
            ) : (
              <button
                onClick={() => setSearchOpen(true)}
                className="text-text-secondary hover:text-white transition-colors p-1"
                aria-label="Search"
              >
                <Search className="w-5 h-5" />
              </button>
            )}
          </div>

          {/* Watchlist shortcut */}
          <Link
            href="/my-list"
            className="text-text-secondary hover:text-white transition-colors p-1 relative hidden sm:block"
            aria-label="My Watchlist"
          >
            <Bookmark className="w-5 h-5" />
          </Link>

          {/* Notifications */}
          <button
            className="text-text-secondary hover:text-white transition-colors p-1 relative"
            aria-label="Notifications"
          >
            <Bell className="w-5 h-5" />
            <span className="absolute top-1 right-1 w-2 h-2 rounded-full bg-pink-500" />
          </button>

          {/* User Profile Avatar */}
          <div className="flex items-center space-x-2 pl-2">
            <div className="w-8 h-8 rounded-full bg-surface-elevated border border-border flex items-center justify-center text-text-primary hover:border-purple-500 cursor-pointer transition-colors">
              <User className="w-4 h-4" />
            </div>
          </div>
        </div>
      </div>
    </nav>
  );
}
