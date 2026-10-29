'use client';

import React, { createContext, useContext, useEffect, useState, useMemo, useCallback } from 'react';
import type { User, Session } from '@supabase/supabase-js';
import { createClient } from './supabase/client';
import { syncUserProfile } from './api';

export type AuthRole = 'USER' | 'ADMIN' | 'anonymous';

/**
 * Mirrors Constants.ADMIN_EMAIL_ALLOWLIST on the backend. The API is the real gate; this only decides what the UI shows.
 * Keep the two lists in step.
 */
const ADMIN_EMAIL_ALLOWLIST = ['imax77work@gmail.com'];

interface AuthContextType {
  user: User | null;
  session: Session | null;
  role: AuthRole;
  isAuthed: boolean;
  isAdmin: boolean;
  loading: boolean;
  signInWithPassword: (email: string, password: string) => Promise<{ error: Error | null }>;
  signUp: (email: string, password: string, name?: string) => Promise<{ error: Error | null }>;
  signInWithOAuth: (provider: 'google') => Promise<{ error: Error | null }>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const supabase = useMemo(() => createClient(), []);

  const determineRole = (u: User | null): AuthRole => {
    if (!u) return 'anonymous';
    // app_metadata is server-controlled; user_metadata is user-editable and must never decide roles.
    // This only drives UI visibility - the backend independently enforces ADMIN from the JWT.
    const email = (u.email ?? '').toLowerCase();
    const allowed = ADMIN_EMAIL_ALLOWLIST.some((e) => e.toLowerCase() === email);
    const claimsAdmin = String(u.app_metadata?.role ?? '').toUpperCase() === 'ADMIN';
    return claimsAdmin && allowed ? 'ADMIN' : 'USER';
  };

  const syncAuth = useCallback((s: Session | null) => {
    setSession(s);
    const u = s?.user ?? null;
    setUser(u);
  }, []);

  // Create or refresh the viewer's profile row once per signed-in user. Failures are non-fatal: the account page retries.
  const syncedUserId = React.useRef<string | null>(null);
  useEffect(() => {
    const u = user;
    if (!u || syncedUserId.current === u.id) return;
    syncedUserId.current = u.id;
    const name = (u.user_metadata?.name as string | undefined) || (u.user_metadata?.full_name as string | undefined);
    const avatar = u.user_metadata?.avatar_url as string | undefined;
    syncUserProfile({
      userId: u.id,
      email: u.email ?? '',
      displayName: name,
      avatarUrl: avatar,
    }).catch(() => {
      syncedUserId.current = null;
    });
  }, [user]);

  useEffect(() => {
    let mounted = true;

    async function init() {
      try {
        const { data: { session: initialSession } } = await supabase.auth.getSession();
        if (mounted) {
          syncAuth(initialSession);
          setLoading(false);
        }
      } catch (e) {
        console.warn('Supabase auth getSession check warning:', e);
        if (mounted) setLoading(false);
      }
    }

    init();

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, currentSession) => {
      if (mounted) {
        syncAuth(currentSession);
        setLoading(false);
      }
    });

    return () => {
      mounted = false;
      subscription.unsubscribe();
    };
  }, [supabase, syncAuth]);

  const signInWithPassword = async (email: string, password: string) => {
    try {
      const { data, error } = await supabase.auth.signInWithPassword({
        email,
        password,
      });
      if (error) return { error };
      syncAuth(data.session);
      return { error: null };
    } catch (err) {
      return { error: err as Error };
    }
  };

  const signUp = async (email: string, password: string, name?: string) => {
    try {
      const { data, error } = await supabase.auth.signUp({
        email,
        password,
        options: {
          data: {
            name: name || email.split('@')[0],
          },
        },
      });
      if (error) return { error };
      syncAuth(data.session);
      return { error: null };
    } catch (err) {
      return { error: err as Error };
    }
  };

  const signInWithOAuth = async (provider: 'google') => {
    try {
      const origin = typeof window !== 'undefined' ? window.location.origin : '';
      const { error } = await supabase.auth.signInWithOAuth({
        provider,
        options: {
          redirectTo: `${origin}/auth/callback`,
        },
      });
      if (error) return { error };
      return { error: null };
    } catch (err) {
      return { error: err as Error };
    }
  };

  const signOut = async () => {
    await supabase.auth.signOut();
    syncAuth(null);
  };

  const role = determineRole(user);
  const isAuthed = role !== 'anonymous';
  const isAdmin = role === 'ADMIN';

  const value = useMemo<AuthContextType>(() => ({
    user,
    session,
    role,
    isAuthed,
    isAdmin,
    loading,
    signInWithPassword,
    signUp,
    signInWithOAuth,
    signOut,
  }), [user, session, role, isAuthed, isAdmin, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
