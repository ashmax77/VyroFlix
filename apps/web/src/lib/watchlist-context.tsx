'use client';

import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { fetchWatchlistTitleIds, toggleWatchlist } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';

interface WatchlistContextValue {
  /** True when the viewer has the title in My list. Always false while signed out or still loading. */
  has: (titleId: string) => boolean;
  /** Adds or removes a title. Resolves once the server has accepted the change; rejects with ApiError otherwise. */
  toggle: (titleId: string) => Promise<boolean>;
  ready: boolean;
}

const WatchlistContext = createContext<WatchlistContextValue | null>(null);

/**
 * One watchlist lookup per session, shared by every poster, hero and title page.
 * Replaces a `checkWatchlist` request per card.
 */
export function WatchlistProvider({ children }: { children: React.ReactNode }) {
  const { isAuthed, loading } = useAuth();
  const [ids, setIds] = useState<Set<string>>(new Set());
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (loading) return;
    if (!isAuthed) {
      setIds(new Set());
      setReady(true);
      return;
    }
    let active = true;
    fetchWatchlistTitleIds()
      .then((list) => {
        if (active) setIds(new Set(list));
      })
      .catch(() => {
        // Non-critical: cards fall back to "not in list" and the toggle still works.
      })
      .finally(() => {
        if (active) setReady(true);
      });
    return () => {
      active = false;
    };
  }, [isAuthed, loading]);

  const has = useCallback((titleId: string) => ids.has(titleId), [ids]);

  const toggle = useCallback(
    async (titleId: string) => {
      // Callers decide how to react to a rejection (inline message or redirect); the list is unchanged on failure.
      const wasIn = ids.has(titleId);
      const nowIn = await toggleWatchlist(titleId, wasIn);
      setIds((prev) => {
        const next = new Set(prev);
        if (nowIn) next.add(titleId);
        else next.delete(titleId);
        return next;
      });
      return nowIn;
    },
    [ids],
  );

  const value = useMemo(() => ({ has, toggle, ready }), [has, toggle, ready]);
  return <WatchlistContext.Provider value={value}>{children}</WatchlistContext.Provider>;
}

export function useWatchlist(): WatchlistContextValue {
  const ctx = useContext(WatchlistContext);
  if (!ctx) throw new Error('useWatchlist must be used inside WatchlistProvider');
  return ctx;
}
