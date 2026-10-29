'use client';

import React, { useCallback, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { EmptyState, ErrorState, Skeleton } from '@/components/common';
import { FieldError, inputClass } from '@/components/layout/AuthShell';
import { ApiError, createGenre, fetchAdminGenres, type GenreOption } from '@/lib/api';

const createSchema = z.object({
  name: z.string().trim().min(1, 'Give the genre a name.').max(100),
  slug: z
    .string()
    .trim()
    .min(1, 'Add a slug, such as science-fiction.')
    .max(100)
    .refine((v) => /^[a-z0-9]+(-[a-z0-9]+)*$/.test(v), 'Use lowercase letters, digits and single hyphens.'),
});
type CreateValues = z.infer<typeof createSchema>;

function slugify(name: string): string {
  return name
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

export default function GenresPage() {
  const [genres, setGenres] = useState<GenreOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [createError, setCreateError] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    fetchAdminGenres()
      .then((g) => setGenres([...g].sort((a, b) => a.name.localeCompare(b.name))))
      .catch((e) => setError(e instanceof ApiError ? e.message : "Couldn't load genres. Try again."))
      .finally(() => setLoading(false));
  }, []);

  useEffect(load, [load]);

  const form = useForm<CreateValues>({ resolver: zodResolver(createSchema), defaultValues: { name: '', slug: '' } });

  const onCreate = form.handleSubmit(async (v) => {
    setCreateError(null);
    try {
      await createGenre({ name: v.name, slug: v.slug });
      form.reset({ name: '', slug: '' });
      load();
    } catch (e) {
      setCreateError(e instanceof ApiError ? e.message : "Couldn't create the genre. Try again.");
    }
  });

  return (
    <section aria-labelledby="genres-heading" className="space-y-10">
      <header>
        <h1 id="genres-heading" className="text-3xl sm:text-4xl">Genres</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">Genres power the search filter and the genre tags on each title.</p>
      </header>

      <form onSubmit={onCreate} noValidate className="grid max-w-2xl gap-5 sm:grid-cols-[1fr_1fr_auto] sm:items-start">
        <div>
          <label htmlFor="g-name" className="mb-1.5 block text-sm font-medium text-foreground">Name</label>
          <input
            id="g-name"
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
          <label htmlFor="g-slug" className="mb-1.5 block text-sm font-medium text-foreground">Slug</label>
          <input id="g-slug" className={inputClass} aria-invalid={!!form.formState.errors.slug} {...form.register('slug')} />
          <FieldError message={form.formState.errors.slug?.message} />
        </div>
        <div className="sm:pt-7">
          <Button type="submit" disabled={form.formState.isSubmitting}>Add genre</Button>
        </div>
        {createError && <div className="sm:col-span-3"><ErrorState compact message={createError} /></div>}
      </form>

      {error && <ErrorState message={error} onRetry={load} />}

      {loading ? (
        <div className="space-y-3" aria-busy="true">
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
        </div>
      ) : !error && genres.length === 0 ? (
        <EmptyState title="No genres yet" description="Add the first one above." />
      ) : (
        <ul className="divide-y divide-border border-y border-border text-sm">
          {genres.map((g) => (
            <li key={g.id} className="flex items-baseline justify-between gap-4 py-3">
              <span className="text-foreground">{g.name}</span>
              <span className="text-subtle">{g.slug}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
