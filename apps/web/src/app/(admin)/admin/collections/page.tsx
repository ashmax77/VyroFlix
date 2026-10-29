'use client';

import React, { useCallback, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { EmptyState, ErrorState, Skeleton } from '@/components/common';
import { FieldError, inputClass } from '@/components/layout/AuthShell';
import {
  ApiError,
  createCollection,
  fetchAdminCollections,
  fetchAdminTitles,
  setCollectionTitles,
  type AdminCollection,
} from '@/lib/api';
import { cn } from '@/lib/utils';
import type { Title } from '@/types';

const slugPattern = /^[a-z0-9]+(-[a-z0-9]+)*$/;

const createSchema = z.object({
  name: z.string().trim().min(1, 'Give the collection a name.').max(255),
  slug: z
    .string()
    .trim()
    .min(1, 'Add a slug, such as sci-fi-classics.')
    .max(255)
    .refine((v) => slugPattern.test(v), 'Use lowercase letters, digits and single hyphens.'),
  displayOrder: z.coerce.number({ invalid_type_error: 'Enter a number.' }).int().min(0).max(10000),
});
type CreateValues = z.infer<typeof createSchema>;

/** Lowercase, hyphen-separated slug from a display name. */
function slugify(name: string): string {
  return name
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

export default function CollectionsPage() {
  const [collections, setCollections] = useState<AdminCollection[]>([]);
  const [titles, setTitles] = useState<Title[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [createError, setCreateError] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    Promise.all([fetchAdminCollections(), fetchAdminTitles()])
      .then(([c, t]) => {
        setCollections(c);
        setTitles(t);
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : "Couldn't load collections. Try again."))
      .finally(() => setLoading(false));
  }, []);

  useEffect(load, [load]);

  const form = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: { name: '', slug: '', displayOrder: 0 },
  });

  const onCreate = form.handleSubmit(async (v) => {
    setCreateError(null);
    try {
      await createCollection({ name: v.name, slug: v.slug, displayOrder: v.displayOrder });
      form.reset({ name: '', slug: '', displayOrder: 0 });
      load();
    } catch (e) {
      setCreateError(e instanceof ApiError ? e.message : "Couldn't create the collection. Try again.");
    }
  });

  return (
    <section aria-labelledby="collections-heading" className="space-y-10">
      <header>
        <h1 id="collections-heading" className="text-3xl sm:text-4xl">Collections</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Collections become rails on the browse page, in display order. Choose which titles each one shows.
        </p>
      </header>

      <form onSubmit={onCreate} noValidate className="grid max-w-3xl gap-5 sm:grid-cols-[1fr_1fr_120px_auto] sm:items-start">
        <div>
          <label htmlFor="c-name" className="mb-1.5 block text-sm font-medium text-foreground">Name</label>
          <input
            id="c-name"
            className={inputClass}
            aria-invalid={!!form.formState.errors.name}
            {...form.register('name', {
              onChange: (e) => {
                if (!form.getValues('slug')) form.setValue('slug', slugify(e.target.value));
              },
            })}
          />
          <FieldError message={form.formState.errors.name?.message} />
        </div>
        <div>
          <label htmlFor="c-slug" className="mb-1.5 block text-sm font-medium text-foreground">Slug</label>
          <input id="c-slug" className={inputClass} aria-invalid={!!form.formState.errors.slug} {...form.register('slug')} />
          <FieldError message={form.formState.errors.slug?.message} />
        </div>
        <div>
          <label htmlFor="c-order" className="mb-1.5 block text-sm font-medium text-foreground">Order</label>
          <input id="c-order" type="number" min={0} className={inputClass} aria-invalid={!!form.formState.errors.displayOrder} {...form.register('displayOrder')} />
          <FieldError message={form.formState.errors.displayOrder?.message} />
        </div>
        <div className="sm:pt-7">
          <Button type="submit" disabled={form.formState.isSubmitting}>Create collection</Button>
        </div>
        {createError && <div className="sm:col-span-4"><ErrorState compact message={createError} /></div>}
      </form>

      {error && <ErrorState message={error} onRetry={load} />}

      {loading ? (
        <div className="space-y-4" aria-busy="true">
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
        </div>
      ) : !error && collections.length === 0 ? (
        <EmptyState title="No collections yet" description="Create one above, then choose its titles." />
      ) : (
        <div className="divide-y divide-border border-y border-border">
          {collections.map((c) => (
            <CollectionEditor key={c.id} collection={c} titles={titles} onSaved={load} />
          ))}
        </div>
      )}
    </section>
  );
}

function CollectionEditor({ collection, titles, onSaved }: { collection: AdminCollection; titles: Title[]; onSaved: () => void }) {
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState<Set<string>>(() => new Set(collection.titles.map((t) => t.id)));
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ tone: 'ok' | 'error'; text: string } | null>(null);

  const toggle = (id: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const save = async () => {
    setSaving(true);
    setMessage(null);
    try {
      await setCollectionTitles(collection.id, Array.from(selected));
      setMessage({ tone: 'ok', text: 'Saved. The rail updates on browse.' });
      onSaved();
    } catch (e) {
      setMessage({ tone: 'error', text: e instanceof ApiError ? e.message : "Couldn't save. Try again." });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="py-5">
      <div className="flex flex-wrap items-baseline justify-between gap-4">
        <div>
          <h2 className="text-lg text-foreground">{collection.name}</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            {collection.titles.length} {collection.titles.length === 1 ? 'title' : 'titles'} · order {collection.displayOrder} · /{collection.slug}
          </p>
        </div>
        <button type="button" onClick={() => setOpen((o) => !o)} aria-expanded={open} className="text-sm text-primary hover:underline">
          {open ? 'Close' : 'Choose titles'}
        </button>
      </div>

      {open && (
        <div className="mt-5">
          {titles.length === 0 ? (
            <p className="text-sm text-muted-foreground">Create a title first, then add it here.</p>
          ) : (
            <ul className="grid gap-x-6 gap-y-2 sm:grid-cols-2">
              {titles.map((t) => (
                <li key={t.id}>
                  <label className={cn('flex cursor-pointer items-center gap-3 rounded-card px-2 py-1.5 text-sm hover:bg-surface', selected.has(t.id) ? 'text-foreground' : 'text-muted-foreground')}>
                    <input type="checkbox" checked={selected.has(t.id)} onChange={() => toggle(t.id)} className="h-4 w-4 accent-[rgb(var(--projector))]" />
                    <span className="min-w-0 truncate">{t.title}</span>
                    <span className="ml-auto text-xs text-subtle">{t.publicationState === 'PUBLISHED' ? 'Published' : 'Not published'}</span>
                  </label>
                </li>
              ))}
            </ul>
          )}
          <div className="mt-4 flex items-center gap-4">
            <Button size="sm" onClick={save} disabled={saving}>{saving ? 'Saving…' : 'Save titles'}</Button>
            {message && (
              <span role={message.tone === 'error' ? 'alert' : 'status'} className={cn('text-sm', message.tone === 'error' ? 'text-danger' : 'text-muted-foreground')}>
                {message.text}
              </span>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
