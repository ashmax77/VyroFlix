'use client';

import React, { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { EmptyState, ErrorState, Skeleton } from '@/components/common';
import { FieldError, inputClass } from '@/components/layout/AuthShell';
import {
  ApiError,
  createTitle,
  deleteTitle,
  fetchAdminTitles,
  publishTitle,
  unpublishTitle,
  type AdminTitleFilters,
} from '@/lib/api';
import { cn } from '@/lib/utils';
import type { PublicationState, Title, TitleType } from '@/types';

const STATES: { value: PublicationState | ''; label: string }[] = [
  { value: '', label: 'All' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PUBLISHED', label: 'Published' },
  { value: 'UNPUBLISHED', label: 'Unpublished' },
];

const optionalUrl = z
  .string()
  .trim()
  .max(500, 'Keep the link under 500 characters.')
  .refine((v) => v === '' || /^https:\/\//i.test(v), 'Use an https:// link.');

const createSchema = z.object({
  title: z.string().trim().min(1, 'Give the title a name.').max(255, 'Keep the name under 255 characters.'),
  type: z.enum(['MOVIE', 'SERIES']),
  releaseYear: z
    .string()
    .trim()
    .refine((v) => v === '' || (/^\d{4}$/.test(v) && Number(v) >= 1880 && Number(v) <= 2200), 'Enter a four-digit year.'),
  maturityRating: z.string().trim().max(10, 'Keep the rating under 10 characters.'),
  synopsis: z.string().trim().max(4000),
  posterUrl: optionalUrl,
  bannerUrl: optionalUrl,
});
type CreateValues = z.infer<typeof createSchema>;

export default function TitlesPage() {
  const router = useRouter();
  const [state, setState] = useState<PublicationState | ''>('');
  const [typeFilter, setTypeFilter] = useState<TitleType | ''>('');
  const [titles, setTitles] = useState<Title[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [rowError, setRowError] = useState<{ id: string; message: string } | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const form = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: { title: '', type: 'MOVIE', releaseYear: '', maturityRating: '', synopsis: '', posterUrl: '', bannerUrl: '' },
  });

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    const filters: AdminTitleFilters = {};
    if (state) filters.state = state;
    if (typeFilter) filters.type = typeFilter;
    fetchAdminTitles(filters)
      .then(setTitles)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Couldn't load titles. Try again."))
      .finally(() => setLoading(false));
  }, [state, typeFilter]);

  useEffect(load, [load]);

  const act = async (id: string, fn: () => Promise<unknown>) => {
    setBusyId(id);
    setRowError(null);
    try {
      await fn();
      load();
    } catch (e) {
      setRowError({ id, message: e instanceof ApiError ? e.message : "That didn't work. Try again." });
    } finally {
      setBusyId(null);
      setConfirmDeleteId(null);
    }
  };

  const onCreate = form.handleSubmit(async (v) => {
    setCreateError(null);
    try {
      const created = await createTitle({
        title: v.title,
        type: v.type,
        synopsis: v.synopsis || undefined,
        releaseYear: v.releaseYear ? Number(v.releaseYear) : undefined,
        maturityRating: v.maturityRating || undefined,
        posterUrl: v.posterUrl || undefined,
        bannerUrl: v.bannerUrl || undefined,
      });
      form.reset();
      setCreating(false);
      load();
      // Land on the new draft so the next step (media, seasons, credits) is one click away.
      router.push(`/admin/titles/${created.id}`);
    } catch (e) {
      setCreateError(e instanceof ApiError ? e.message : "Couldn't create the title. Try again.");
    }
  });

  return (
    <section aria-labelledby="titles-heading" className="space-y-8">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 id="titles-heading" className="text-3xl sm:text-4xl">Titles</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            A title goes live only after its media is ready. Drafts stay hidden from viewers.
          </p>
        </div>
        <Button onClick={() => setCreating((c) => !c)} aria-expanded={creating} variant={creating ? 'secondary' : 'default'}>
          {creating ? 'Cancel' : 'New title'}
        </Button>
      </header>

      {creating && (
        <form onSubmit={onCreate} noValidate className="grid gap-5 border-y border-border py-6 sm:grid-cols-2" aria-label="New title">
          <div className="sm:col-span-2">
            <label htmlFor="new-title" className="mb-1.5 block text-sm font-medium text-foreground">Title</label>
            <input id="new-title" className={inputClass} aria-invalid={!!form.formState.errors.title} {...form.register('title')} />
            <FieldError message={form.formState.errors.title?.message} />
          </div>
          <div>
            <label htmlFor="new-type" className="mb-1.5 block text-sm font-medium text-foreground">Type</label>
            <select id="new-type" className={inputClass} {...form.register('type')}>
              <option value="MOVIE">Movie</option>
              <option value="SERIES">Series</option>
            </select>
          </div>
          <div>
            <label htmlFor="new-year" className="mb-1.5 block text-sm font-medium text-foreground">Release year</label>
            <input id="new-year" inputMode="numeric" className={inputClass} aria-invalid={!!form.formState.errors.releaseYear} {...form.register('releaseYear')} />
            <FieldError message={form.formState.errors.releaseYear?.message} />
          </div>
          <div>
            <label htmlFor="new-rating" className="mb-1.5 block text-sm font-medium text-foreground">Rating</label>
            <input id="new-rating" placeholder="PG-13" className={inputClass} {...form.register('maturityRating')} />
            <FieldError message={form.formState.errors.maturityRating?.message} />
          </div>
          <div className="sm:col-span-2">
            <label htmlFor="new-synopsis" className="mb-1.5 block text-sm font-medium text-foreground">Synopsis</label>
            <textarea id="new-synopsis" rows={3} className={cn(inputClass, 'h-auto py-2.5')} {...form.register('synopsis')} />
          </div>
          <div>
            <label htmlFor="new-poster" className="mb-1.5 block text-sm font-medium text-foreground">Poster link</label>
            <input id="new-poster" placeholder="https://" className={inputClass} aria-invalid={!!form.formState.errors.posterUrl} {...form.register('posterUrl')} />
            <FieldError message={form.formState.errors.posterUrl?.message} />
          </div>
          <div>
            <label htmlFor="new-banner" className="mb-1.5 block text-sm font-medium text-foreground">Banner link</label>
            <input id="new-banner" placeholder="https://" className={inputClass} aria-invalid={!!form.formState.errors.bannerUrl} {...form.register('bannerUrl')} />
            <FieldError message={form.formState.errors.bannerUrl?.message} />
          </div>
          {createError && <div className="sm:col-span-2"><ErrorState compact message={createError} /></div>}
          <div className="sm:col-span-2">
            <Button type="submit" disabled={form.formState.isSubmitting}>
              {form.formState.isSubmitting ? 'Creating…' : 'Create draft'}
            </Button>
          </div>
        </form>
      )}

      <div className="flex flex-wrap items-center justify-between gap-4">
        <div role="tablist" aria-label="Publication state" className="flex gap-5 border-b border-border">
          {STATES.map((s) => (
            <button
              key={s.value || 'all'}
              type="button"
              role="tab"
              aria-selected={state === s.value}
              onClick={() => setState(s.value)}
              className={cn(
                '-mb-px border-b-2 pb-2 text-sm transition-colors',
                state === s.value ? 'border-primary text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground',
              )}
            >
              {s.label}
            </button>
          ))}
        </div>
        <label className="flex items-center gap-2 text-sm text-muted-foreground">
          <span>Type</span>
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value as TitleType | '')}
            className="h-9 rounded-card border border-border bg-surface px-3 text-sm text-foreground focus:border-primary focus:outline-none"
          >
            <option value="">All</option>
            <option value="MOVIE">Movies</option>
            <option value="SERIES">Series</option>
          </select>
        </label>
      </div>

      {error && <ErrorState message={error} onRetry={load} />}

      {loading ? (
        <div className="space-y-3" aria-busy="true">
          {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-14" />)}
        </div>
      ) : !error && titles.length === 0 ? (
        <EmptyState title="No titles here" description={state || typeFilter ? 'Try another filter.' : 'Create a draft to start the catalog.'} />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead className="text-xs text-subtle">
              <tr className="border-b border-border">
                <th scope="col" className="py-3 font-medium">Title</th>
                <th scope="col" className="py-3 font-medium">Type</th>
                <th scope="col" className="py-3 font-medium">State</th>
                <th scope="col" className="py-3 text-right font-medium">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {titles.map((t) => {
                const busy = busyId === t.id;
                return (
                  <tr key={t.id} className="align-top">
                    <td className="py-4 pr-4">
                      <Link href={`/admin/titles/${t.id}`} className="text-foreground hover:text-primary">{t.title}</Link>
                      {t.releaseYear ? <span className="ml-2 text-xs text-subtle">{t.releaseYear}</span> : null}
                      {rowError?.id === t.id && <p role="alert" className="mt-1 text-xs text-danger">{rowError.message}</p>}
                    </td>
                    <td className="py-4 pr-4 text-muted-foreground">{t.type === 'SERIES' ? 'Series' : 'Movie'}</td>
                    <td className="py-4 pr-4 text-muted-foreground">{stateLabel(t.publicationState)}</td>
                    <td className="py-4 text-right">
                      <div className="flex flex-wrap justify-end gap-2">
                        {t.publicationState === 'PUBLISHED' ? (
                          <Button size="sm" variant="secondary" disabled={busy} onClick={() => act(t.id, () => unpublishTitle(t.id))}>
                            Unpublish
                          </Button>
                        ) : (
                          <Button size="sm" disabled={busy} onClick={() => act(t.id, () => publishTitle(t.id))}>
                            Publish
                          </Button>
                        )}
                        {t.publicationState !== 'PUBLISHED' &&
                          (confirmDeleteId === t.id ? (
                            <>
                              <Button size="sm" variant="destructive" disabled={busy} onClick={() => act(t.id, () => deleteTitle(t.id))}>
                                Confirm delete
                              </Button>
                              <Button size="sm" variant="ghost" onClick={() => setConfirmDeleteId(null)}>
                                Keep
                              </Button>
                            </>
                          ) : (
                            <Button size="sm" variant="ghost" disabled={busy} onClick={() => setConfirmDeleteId(t.id)}>
                              Delete
                            </Button>
                          ))}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function stateLabel(s: PublicationState): string {
  return s === 'PUBLISHED' ? 'Published' : s === 'UNPUBLISHED' ? 'Unpublished' : 'Draft';
}
