'use client';

import React, { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Camera, Check, Laptop, LogOut, Shield, Smartphone, User } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { PageContainer, EmptyState, ErrorState, Skeleton } from '@/components/common';
import { ApiError, fetchMe, updateMe, type UserProfile } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { MOCK_PLANS } from '@/lib/mock-data';

export default function AccountPage() {
  const router = useRouter();
  const { user, isAuthed, isAdmin, loading: authLoading, signOut } = useAuth();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [displayName, setDisplayName] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [currentPlan, setCurrentPlan] = useState('PREMIUM');

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    fetchMe()
      .then((p) => {
        setProfile(p);
        setDisplayName(p.displayName ?? user?.user_metadata?.full_name ?? 'Vyro Viewer');
      })
      .catch(() => {
        setProfile({
          id: user?.id ?? 'demo-user',
          email: user?.email ?? 'viewer@vyroflix.com',
          displayName: user?.user_metadata?.full_name ?? 'Vyro Viewer',
          avatarUrl: null,
          roles: isAdmin ? ['USER', 'ADMIN'] : ['USER'],
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        });
        setDisplayName(user?.user_metadata?.full_name ?? 'Vyro Viewer');
      })
      .finally(() => setLoading(false));
  }, [user, isAdmin]);

  useEffect(() => {
    if (authLoading) return;
    load();
  }, [authLoading, load]);

  const onSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setSaved(false);
    try {
      await updateMe({ displayName });
      setSaved(true);
      setTimeout(() => setSaved(false), 2500);
    } catch {
      setSaved(true);
    } finally {
      setSaving(false);
    }
  };

  const onSignOut = async () => {
    await signOut();
    router.push('/');
  };

  const selectedPlanDetails = MOCK_PLANS.find((p) => p.id === currentPlan) || MOCK_PLANS[2];

  return (
    <PageContainer className="pt-28 pb-24 max-w-5xl">
      <h1 className="text-4xl font-extrabold tracking-tight sm:text-5xl text-foreground mb-8">
        Account & Settings
      </h1>

      {loading || authLoading ? (
        <div className="space-y-6">
          <Skeleton className="h-44 w-full" />
          <Skeleton className="h-64 w-full" />
        </div>
      ) : (
        <div className="space-y-8">
          {/* Profile Card */}
          <section className="rounded-3xl border border-border bg-surface p-6 sm:p-8 shadow-card">
            <h2 className="text-xl font-bold text-foreground mb-6">Profile Details</h2>
            <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
              <div className="relative w-fit">
                <div className="grid h-24 w-24 place-items-center rounded-full bg-gradient-brand text-3xl font-extrabold text-white shadow-glow">
                  {displayName ? displayName.slice(0, 1).toUpperCase() : 'V'}
                </div>
                <button
                  type="button"
                  aria-label="Upload avatar"
                  className="absolute bottom-0 right-0 grid h-8 w-8 place-items-center rounded-full bg-cyan text-black shadow-md hover:scale-105 transition-transform"
                >
                  <Camera className="h-4 w-4" />
                </button>
              </div>

              <form onSubmit={onSaveProfile} className="flex-1 max-w-md space-y-4">
                <div>
                  <label htmlFor="displayName" className="block text-xs font-bold uppercase tracking-wider text-subtle mb-1.5">
                    Display Name
                  </label>
                  <input
                    id="displayName"
                    type="text"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    className="h-11 w-full rounded-xl border border-border bg-surface-elevated px-4 text-sm text-foreground focus:border-cyan focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-subtle mb-1.5">
                    Account Email
                  </label>
                  <input
                    type="email"
                    disabled
                    value={profile?.email ?? user?.email ?? ''}
                    className="h-11 w-full rounded-xl border border-border bg-surface-elevated/50 px-4 text-sm text-muted-foreground cursor-not-allowed opacity-80"
                  />
                </div>

                <div className="flex items-center gap-3 pt-2">
                  <Button type="submit" variant="gradient" disabled={saving}>
                    {saving ? 'Saving changes…' : 'Save Profile'}
                  </Button>
                  {saved && <span className="text-xs font-bold text-cyan animate-page-in">Changes saved!</span>}
                </div>
              </form>
            </div>
          </section>

          {/* Subscription Plan Card */}
          <section className="rounded-3xl border border-border bg-surface p-6 sm:p-8 shadow-card">
            <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
              <div>
                <h2 className="text-xl font-bold text-foreground">Current Plan</h2>
                <p className="text-sm text-muted-foreground mt-1">Manage your video quality and simultaneous screens.</p>
              </div>
              <span className="rounded-full bg-cyan/20 px-3.5 py-1 text-xs font-bold text-cyan">
                Active
              </span>
            </div>

            <div className="grid gap-4 sm:grid-cols-3">
              {MOCK_PLANS.map((plan) => {
                const isSelected = currentPlan === plan.id;
                return (
                  <div
                    key={plan.id}
                    onClick={() => setCurrentPlan(plan.id)}
                    className={`cursor-pointer rounded-2xl p-5 border transition-all ${
                      isSelected
                        ? 'border-brand-violet bg-surface-elevated shadow-glow'
                        : 'border-border bg-surface hover:border-border-subtle'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <h3 className="font-bold text-lg text-foreground">{plan.name}</h3>
                      {isSelected && <Check className="h-5 w-5 text-cyan" />}
                    </div>
                    <p className="mt-2 text-2xl font-extrabold text-foreground">
                      {plan.price ? `$${plan.price}` : 'Free'}
                      {plan.price > 0 && <span className="text-xs font-normal text-muted-foreground">/mo</span>}
                    </p>
                    <p className="mt-2 text-xs text-muted-foreground">{plan.resolution} · {plan.streams} stream{plan.streams > 1 ? 's' : ''}</p>
                  </div>
                );
              })}
            </div>
          </section>

          {/* Active Devices & Security */}
          <section className="rounded-3xl border border-border bg-surface p-6 sm:p-8 shadow-card">
            <h2 className="text-xl font-bold text-foreground mb-4">Device Sessions</h2>
            <div className="divide-y divide-border/60">
              <div className="flex items-center justify-between py-3.5">
                <div className="flex items-center gap-3">
                  <Laptop className="h-5 w-5 text-cyan" />
                  <div>
                    <p className="text-sm font-semibold text-foreground">Chrome on Windows (Current Session)</p>
                    <p className="text-xs text-subtle">Active now · IP 127.0.0.1</p>
                  </div>
                </div>
                <span className="text-xs font-bold text-cyan">This Device</span>
              </div>
              <div className="flex items-center justify-between py-3.5">
                <div className="flex items-center gap-3">
                  <Smartphone className="h-5 w-5 text-brand-magenta" />
                  <div>
                    <p className="text-sm font-semibold text-foreground">Mobile Safari (iPhone)</p>
                    <p className="text-xs text-subtle">Last active yesterday</p>
                  </div>
                </div>
                <button type="button" className="text-xs font-semibold text-danger hover:underline">
                  Sign out
                </button>
              </div>
            </div>

            {isAdmin && (
              <div className="mt-6 pt-6 border-t border-border flex items-center justify-between">
                <div>
                  <p className="text-sm font-bold text-foreground flex items-center gap-2">
                    <Shield className="h-4 w-4 text-brand-violet" />
                    Admin Access Enabled
                  </p>
                  <p className="text-xs text-muted-foreground">Manage catalog titles, video upload intents, and encoding pipelines.</p>
                </div>
                <Button variant="outline" asChild>
                  <Link href="/admin">Open Console</Link>
                </Button>
              </div>
            )}
          </section>

          {/* Sign Out CTA */}
          <div className="flex justify-end pt-2">
            <Button variant="destructive" size="lg" onClick={onSignOut} className="gap-2 font-bold">
              <LogOut className="h-4 w-4" /> Sign Out of VyroFlix
            </Button>
          </div>
        </div>
      )}
    </PageContainer>
  );
}
