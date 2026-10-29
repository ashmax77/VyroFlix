'use client';

import React from 'react';
import { AuthProvider } from '@/lib/auth-context';
import { WatchlistProvider } from '@/lib/watchlist-context';

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <AuthProvider>
      <WatchlistProvider>{children}</WatchlistProvider>
    </AuthProvider>
  );
}
