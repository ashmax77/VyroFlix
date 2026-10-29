import React from 'react';
import Link from 'next/link';
import { Logo } from '@/components/common';

export default function Footer() {
  return (
    <footer className="mt-20 border-t border-border/60 pb-20 pt-10 md:pb-10">
      <div className="mx-auto flex max-w-[1800px] flex-col gap-6 px-4 text-sm text-subtle sm:flex-row sm:items-center sm:px-8 lg:px-12">
        <Logo size="sm" className="opacity-80" />
        <ul className="flex flex-wrap gap-6 text-sm">
          <li>
            <Link href="/browse" className="hover:text-foreground transition-colors">
              Browse
            </Link>
          </li>
          <li>
            <Link href="/my-list" className="hover:text-foreground transition-colors">
              My List
            </Link>
          </li>
          <li>
            <Link href="/search" className="hover:text-foreground transition-colors">
              Search
            </Link>
          </li>
          <li>
            <a href="https://github.com/ashmax77/VyroFlix" target="_blank" rel="noreferrer" className="hover:text-foreground transition-colors">
              GitHub
            </a>
          </li>
        </ul>
        <p className="text-xs text-subtle sm:ml-auto">
          © {new Date().getFullYear()} VyroFlix · Cloud-native multi-bitrate HLS streaming
        </p>
      </div>
    </footer>
  );
}
