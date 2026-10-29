'use client';

import React, { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { ErrorState, Skeleton } from '@/components/common';
import { FieldError, inputClass } from '@/components/layout/AuthShell';
import { ApiError, fetchMe, updateMe, type UserProfile } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';

const schema = z.object({
  displayName: z
    .string()
    .trim()
    .max(100, 'Keep the name under 100 characters.'),
  avatarUrl: z
    .string()
    .trim()
    .max(500, 'Keep the link under 500 characters.')
    .refine((v) => v === '' || /^https:\/\//i.test(v), 'Use an https:// link.'),
});
type FormValues = z.infer<typeof schema>;

export default function AccountPage() {
  const router = useRouter();
  const { user, isAuthed, isAdmin, loading: authLoading, signOut } = useAuth();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const form = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { displayName: '', avatarUrl: '' } });

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    fetchMe()
      .then((p) => {
        setProfile(p);
        form.reset({ displayName: p.displayName ?? '', avatarUrl: p.avatarUrl ?? '' });
      })
      .catch((e) => {
        if (e instanceof ApiError && e.isUnauthorized) {
          router.replace('/login?redirect=/account');
          return;
        }
        setError(
          e instanceof ApiError && e.status === 404
            ? 'Your profile is still being set up. Try again in a moment.'
            : "Couldn't load your account. Try again.",
        );
      })
      .finally(() => setLoading(false));
  }, [form, router]);

  useEffect(() => {
    if (authLoading) return;
    if (!isAuthed) {
      router.replace('/login?redirect=/account');
      return;
    }
    load();
  }, [authLoading, isAuthed, load, router]);

  const onSubmit = form.handleSubmit(async (values) => {
    setSaved(false);
    setSaveError(null);
    try {
      const updated = await updateMe({
        displayName: values.displayName || undefined,
        avatarUrl: values.avatarUrl || undefined,
      });
      setProfile(updated);
      setSaved(true);
    } catch (e) {
      setSaveError(e instanceof ApiError ? e.message : "Couldn't save your changes. Try again.");
    }
  });

  const onSignOut = async () => {
    await signOut();
    router.push('/login');
  };

  const plan = String(user?.app_metadata?.plan ?? 'FREE').toUpperCase();
  const streams = plan === 'PREMIUM' ? 4 : plan === 'BASIC' ? 2 : 1;

  return (
    <div className="mx-auto max-w-container px-4 pb-16 pt-6 sm:px-6 lg:px-10">
      <h1 className="text-3xl sm:text-4xl">Account</h1>

      {loading || authLoading ? (
        <div className="mt-8 space-y-4" aria-busy="true">
          <Skeleton className="h-10 w-72" />
          <Skeleton className="h-64 w-full max-w-xl" />
        </div>
      ) : error ? (
        <div className="mt-8 max-w-xl">
          <ErrorState message={error} onRetry={load} />
        </div>
      ) : (
        <div className="mt-8 grid gap-12 lg:grid-cols-[minmax(0,1fr)_280px]">
          <section aria-labelledby="profile-heading" className="max-w-xl">
            <h2 id="profile-heading" className="text-xl text-foreground">Profile</h2>
            <form onSubmit={onSubmit} noValidate className="mt-6 space-y-5">
              <div>
                <label htmlFor="displayName" className="mb-1.5 block text-sm font-medium text-foreground">
                  Display name
                </label>
                <input id="displayName" className={inputClass} aria-invalid={!!form.formState.errors.displayName} {...form.register('displayName')} />
                <FieldError message={form.formState.errors.displayName?.message} />
              </div>
              <div>
                <label htmlFor="avatarUrl" className="mb-1.5 block text-sm font-medium text-foreground">
                  Avatar link
                </label>
                <input
                  id="avatarUrl"
                  inputMode="url"
                  placeholder="https://"
                  className={inputClass}
                  aria-invalid={!!form.formState.errors.avatarUrl}
                  {...form.register('avatarUrl')}
                />
                <FieldError message={form.formState.errors.avatarUrl?.message} />
              </div>

              {saveError && <ErrorState compact message={saveError} />}
              {saved && !saveError && (
                <p role="status" className="text-sm text-muted-foreground">
                  Saved.
                </p>
              )}

              <div className="flex items-center gap-3 pt-2">
                <Button type="submit" disabled={form.formState.isSubmitting}>
                  {form.formState.isSubmitting ? 'Saving…' : 'Save changes'}
                </Button>
              </div>
            </form>
          </section>

          <aside aria-labelledby="plan-heading" className="space-y-8 border-t border-border pt-6 lg:border-l lg:border-t-0 lg:pl-10 lg:pt-0">
            <section>
              <h2 id="plan-heading" className="text-xl text-foreground">Plan</h2>
              <p className="mt-3 font-heading text-3xl text-foreground">{plan.charAt(0) + plan.slice(1).toLowerCase()}</p>
              <p className="mt-1 text-sm text-muted-foreground">
                {streams} {streams === 1 ? 'screen' : 'screens'} at once.
              </p>
            </section>

            <section>
              <h2 className="text-sm font-medium text-foreground">Signed in as</h2>
              <p className="mt-2 break-all text-sm text-muted-foreground">{profile?.email ?? user?.email}</p>
              {isAdmin && (
                <p className="mt-3 text-sm">
                  <Link href="/admin" className="text-primary hover:underline">Open the studio</Link>
                </p>
              )}
            </section>

            <Button variant="secondary" onClick={onSignOut}>
              Sign out
            </Button>
          </aside>
        </div>
      )}
    </div>
  );
}
