import React from 'react';
import Link from 'next/link';

// Slim site footer. Links are plain text, so no hover-only affordances.
export default function Footer() {
  return (
    <footer className="mt-16 border-t border-border">
      <div className="mx-auto flex max-w-container flex-col gap-6 px-4 py-10 text-sm text-muted-foreground sm:px-6 md:flex-row md:items-center md:justify-between lg:px-10">
        <p className="max-w-md text-subtle">
          Low-cost streaming built on multi-bitrate HLS and a CDN. A portfolio project, not a production service.
        </p>
        <div className="flex flex-wrap gap-x-6 gap-y-2">
          <Link href="/browse" className="hover:text-foreground">Browse</Link>
          <Link href="/my-list" className="hover:text-foreground">My list</Link>
          <a href="https://github.com/ashmax77/VyroFlix" target="_blank" rel="noreferrer" className="hover:text-foreground">
            Source on GitHub
          </a>
        </div>
      </div>
      <p className="border-t border-border px-4 py-4 text-center text-xs text-subtle sm:px-6 lg:px-10">
        © {new Date().getFullYear()}. All rights reserved.
      </p>
    </footer>
  );
}
