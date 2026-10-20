import React from 'react';
import Link from 'next/link';

export default function Footer() {
  return (
    <footer className="border-t border-border/40 bg-surface/50 mt-20 py-12 px-4 sm:px-6 lg:px-8 text-sm text-text-muted">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-6">
        <div>
          <span className="font-heading font-extrabold text-lg tracking-wider text-white">
            VYRO<span className="gradient-text">FLIX</span>
          </span>
          <p className="mt-2 text-xs text-text-muted max-w-sm">
            Cloud-native, low-cost video streaming platform. Delivering multi-bitrate HLS media across global edge CDN.
          </p>
        </div>

        <div className="flex flex-wrap gap-x-8 gap-y-2 text-xs">
          <Link href="/browse" className="hover:text-text-primary transition-colors">Catalog</Link>
          <Link href="/my-list" className="hover:text-text-primary transition-colors">My List</Link>
          <a href="https://github.com/ashmax77/VyroFlix" target="_blank" rel="noreferrer" className="hover:text-text-primary transition-colors">
            GitHub
          </a>
          <span className="text-text-muted/60">Portfolio Demonstration Project</span>
        </div>
      </div>
      <div className="max-w-7xl mx-auto mt-8 pt-6 border-t border-border/20 text-center text-xs text-text-muted/60">
        © {new Date().getFullYear()} VyroFlix. All rights reserved.
      </div>
    </footer>
  );
}
