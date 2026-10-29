'use client';

import React, { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useFieldArray, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { EmptyState, ErrorState, Skeleton } from '@/components/common';
import { FieldError, inputClass } from '@/components/layout/AuthShell';
import {
  ApiError,
  addEpisode,
  addSeason,
  createPerson,
  fetchAdminGenres,
  fetchAdminTitle,
  fetchUploadIntentsForContent,
  publishTitle,
  setCredits,
  unpublishTitle,
  updateTitle,
  type CreditRole,
  type GenreOption,
} from '@/lib/api';
import { cn } from '@/lib/utils';
import type { CastMember, Season, Title, UploadIntentResponse } from '@/types';

const optionalUrl = z
  .string()
  .trim()
  .max(500, 'Keep the link under 500 characters.')
  .refine((v) => v === '' || /^https:\/\//i.test(v), 'Use an https:// link.');

const metaSchema = z.object({
  title: z.string().trim().min(1, 'Give the title a name.').max(255),
  releaseYear: z
    .string()
    .trim()
    .refine((v) => v === '' || (/^\d{4}$/.test(v) && Number(v) >= 1880 && Number(v) <= 2200), 'Enter a four-digit year.'),
  maturityRating: z.string().trim().max(10, 'Keep the rating under 10 characters.'),
  synopsis: z.string().trim().max(4000),
  posterUrl: optionalUrl,
  bannerUrl: optionalUrl,
});
type MetaValues = z.infer<typeof metaSchema>;

const seasonSchema = z.object({
  seasonNumber: z.coerce.number({ invalid_type_error: 'Enter a season number.' }).int().min(1, 'Start at 1.'),
  name: z.string().trim().max(255),
  overview: z.string().trim().max(2000),
});
type SeasonValues = z.infer<typeof seasonSchema>;

const episodeSchema = z.object({
  episodeNumber: z.coerce.number({ invalid_type_error: 'Enter an episode number.' }).int().min(1, 'Start at 1.'),
  name: z.string().trim().min(1, 'Give the episode a name.').max(255),
  overview: z.string().trim().max(2000),
  minutes: z
    .string()
    .trim()
    .refine((v) => v === '' || (/^\d+$/.test(v) && Number(v) > 0 && Number(v) < 1000), 'Enter whole minutes.'),
});
type EpisodeValues = z.infer<typeof episodeSchema>;

const personSchema = z.object({
  name: z.string().trim().min(1, 'Give the person a name.').max(255),
  avatarUrl: optionalUrl,
});
type PersonValues = z.infer<typeof personSchema>;

const creditsSchema = z.object({
  credits: z.array(
    z.object({
      personId: z.string().trim().uuid('Paste a valid person ID.'),
      role: z.enum(['ACTOR', 'DIRECTOR', 'WRITER', 'PRODUCER']),
      characterName: z.string().trim().max(255),
    }),
  ),
});
type CreditsValues = z.infer<typeof creditsSchema>;

export default function TitleEditorPage() {
  const params = useParams();
  const id = params.id as string;

  const [title, setTitle] = useState<Title | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<{ tone: 'ok' | 'error'; text: string } | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    fetchAdminTitle(id)
      .then(setTitle)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Couldn't load this title. Try again."))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(load, [load]);

  const flash = (tone: 'ok' | 'error', text: string) => setNotice({ tone, text });

  const onState = async (action: 'publish' | 'unpublish') => {
    setBusy(true);
    setNotice(null);
    try {
      const updated = action === 'publish' ? await publishTitle(id) : await unpublishTitle(id);
      setTitle((prev) => (prev ? { ...prev, publicationState: updated.publicationState } : prev));
      flash('ok', action === 'publish' ? 'Published.' : 'Unpublished. Viewers no longer see it.');
    } catch (e) {
      // The API explains what is missing (for example, READY media), so show its message as-is.
      flash('error', e instanceof ApiError ? e.message : "That didn't work. Try again.");
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-4" aria-busy="true">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (error || !title) {
    return (
      <div className="space-y-6">
        {error ? <ErrorState message={error} onRetry={load} /> : <EmptyState title="Title not found" />}
        <Link href="/admin/titles" className="text-sm text-primary hover:underline">Back to titles</Link>
      </div>
    );
  }

  return (
    <div className="space-y-12">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <Link href="/admin/titles" className="text-sm text-muted-foreground hover:text-foreground">← Titles</Link>
          <h1 className="mt-2 text-3xl sm:text-4xl">{title.title}</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {title.type === 'SERIES' ? 'Series' : 'Movie'} · {stateLabel(title.publicationState)}
          </p>
        </div>
        <div className="flex gap-2">
          {title.publicationState === 'PUBLISHED' ? (
            <Button variant="secondary" disabled={busy} onClick={() => onState('unpublish')}>Unpublish</Button>
          ) : (
            <Button disabled={busy} onClick={() => onState('publish')}>Publish</Button>
          )}
        </div>
      </header>

      {notice && (
        <div role={notice.tone === 'error' ? 'alert' : 'status'} className={cn('text-sm', notice.tone === 'error' ? 'text-danger' : 'text-muted-foreground')}>
          {notice.text}
        </div>
      )}

      <MetadataForm title={title} onSaved={(t) => { setTitle(t); flash('ok', 'Saved.'); }} onError={(m) => flash('error', m)} />
      <SeasonsSection title={title} onChanged={load} onError={(m) => flash('error', m)} />
      <CreditsSection title={title} onSaved={(cast) => setTitle((prev) => (prev ? { ...prev, cast } : prev))} onError={(m) => flash('error', m)} />
      <MediaSection title={title} />
    </div>
  );
}

/** What the studio can act on for an upload. Encoding is reported by its own worker, which this server does not run. */
function uploadStatusText(intent: UploadIntentResponse): { text: string; tone: 'ok' | 'pending' | 'error' } {
  if (intent.status === 'FAILED') return { text: 'Upload failed. Create a new upload.', tone: 'error' };
  if (intent.status === 'UPLOADED') return { text: 'Uploaded. Waiting for the encoding worker.', tone: 'pending' };
  if (new Date(intent.expiresAt).getTime() < Date.now()) return { text: 'Upload link expired. Create a new upload.', tone: 'error' };
  return { text: 'Waiting for the file.', tone: 'pending' };
}

function MediaSection({ title }: { title: Title }) {
  const [rows, setRows] = useState<{ label: string; intent: UploadIntentResponse }[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    const targets =
      title.type === 'SERIES'
        ? (title.seasons ?? []).flatMap((s) =>
            (s.episodes ?? []).map((ep) => ({ id: ep.id, label: `Season ${s.seasonNumber}, episode ${ep.episodeNumber}: ${ep.name}` })),
          )
        : [{ id: title.id, label: title.title }];
    try {
      const lists = await Promise.all(
        targets.map(async (t) => (await fetchUploadIntentsForContent(t.id)).map((intent) => ({ label: t.label, intent }))),
      );
      setRows(lists.flat());
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Couldn't load upload status. Try again.");
    }
  }, [title]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <section aria-labelledby="media-heading" className="border-t border-border pt-8">
      <div className="flex flex-wrap items-baseline justify-between gap-3">
        <h2 id="media-heading" className="text-xl text-foreground">Media</h2>
        <Link href="/admin/uploads" className="text-sm text-primary underline-offset-4 hover:underline">Upload a file</Link>
      </div>
      <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
        Files upload straight to storage. The encoding worker turns each upload into streams, and viewers can play a title once that finishes.
      </p>

      {error ? (
        <div className="mt-6"><ErrorState compact message={error} onRetry={load} /></div>
      ) : rows === null ? (
        <Skeleton className="mt-6 h-16 w-full" />
      ) : rows.length === 0 ? (
        <p className="mt-6 text-sm text-muted-foreground">No uploads yet for this title.</p>
      ) : (
        <ul className="mt-6 divide-y divide-border border-y border-border">
          {rows.map(({ label, intent }) => {
            const status = uploadStatusText(intent);
            return (
              <li key={intent.id} className="grid gap-1 py-4 sm:grid-cols-[1fr_auto] sm:items-center sm:gap-4">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-foreground">{label}</p>
                  <p className="truncate text-xs text-muted-foreground">{intent.filename}</p>
                </div>
                <p className={cn('text-sm', status.tone === 'error' ? 'text-danger' : 'text-muted-foreground')}>{status.text}</p>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

function sameIds(a: string[], b: string[]): boolean {
  return a.length === b.length && a.every((id) => b.includes(id));
}

function MetadataForm({ title, onSaved, onError }: { title: Title; onSaved: (t: Title) => void; onError: (m: string) => void }) {
  const form = useForm<MetaValues>({
    resolver: zodResolver(metaSchema),
    defaultValues: {
      title: title.title,
      releaseYear: title.releaseYear ? String(title.releaseYear) : '',
      maturityRating: title.maturityRating ?? '',
      synopsis: title.synopsis ?? '',
      posterUrl: title.posterUrl ?? '',
      bannerUrl: title.bannerUrl ?? '',
    },
  });

  const [genres, setGenres] = useState<GenreOption[] | null>(null);
  const [genresError, setGenresError] = useState<string | null>(null);
  const initialGenreIds = title.genres?.map((g) => g.id) ?? [];
  const [savedGenreIds, setSavedGenreIds] = useState<string[]>(initialGenreIds);
  const [selectedGenreIds, setSelectedGenreIds] = useState<string[]>(initialGenreIds);
  const genresChanged = !sameIds(selectedGenreIds, savedGenreIds);

  const loadGenres = useCallback(() => {
    setGenresError(null);
    fetchAdminGenres()
      .then(setGenres)
      .catch((err: unknown) => setGenresError(err instanceof ApiError ? err.message : "Couldn't load genres. Try again."));
  }, []);

  useEffect(() => {
    loadGenres();
  }, [loadGenres]);

  const toggleGenre = (id: string) =>
    setSelectedGenreIds((current) => (current.includes(id) ? current.filter((g) => g !== id) : [...current, id]));

  const onSubmit = form.handleSubmit(async (v) => {
    try {
      const updated = await updateTitle(title.id, {
        title: v.title,
        releaseYear: v.releaseYear ? Number(v.releaseYear) : undefined,
        maturityRating: v.maturityRating || undefined,
        synopsis: v.synopsis || undefined,
        posterUrl: v.posterUrl || undefined,
        bannerUrl: v.bannerUrl || undefined,
        // Omitted when unchanged: the server reads a missing list as "keep the current genres".
        genreIds: genresChanged ? selectedGenreIds : undefined,
      });
      form.reset({ ...v });
      setSavedGenreIds(selectedGenreIds);
      onSaved({ ...title, ...updated });
    } catch (e) {
      onError(e instanceof ApiError ? e.message : "Couldn't save. Try again.");
    }
  });

  const e = form.formState.errors;
  return (
    <section aria-labelledby="details-heading" className="border-t border-border pt-8">
      <h2 id="details-heading" className="text-xl text-foreground">Details</h2>
      <form onSubmit={onSubmit} noValidate className="mt-6 grid max-w-3xl gap-5 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <label htmlFor="m-title" className="mb-1.5 block text-sm font-medium text-foreground">Title</label>
          <input id="m-title" className={inputClass} aria-invalid={!!e.title} {...form.register('title')} />
          <FieldError message={e.title?.message} />
        </div>
        <div>
          <label htmlFor="m-year" className="mb-1.5 block text-sm font-medium text-foreground">Release year</label>
          <input id="m-year" inputMode="numeric" className={inputClass} aria-invalid={!!e.releaseYear} {...form.register('releaseYear')} />
          <FieldError message={e.releaseYear?.message} />
        </div>
        <div>
          <label htmlFor="m-rating" className="mb-1.5 block text-sm font-medium text-foreground">Rating</label>
          <input id="m-rating" className={inputClass} {...form.register('maturityRating')} />
          <FieldError message={e.maturityRating?.message} />
        </div>
        <div className="sm:col-span-2">
          <label htmlFor="m-synopsis" className="mb-1.5 block text-sm font-medium text-foreground">Synopsis</label>
          <textarea id="m-synopsis" rows={4} className={cn(inputClass, 'h-auto py-2.5')} {...form.register('synopsis')} />
        </div>
        <div>
          <label htmlFor="m-poster" className="mb-1.5 block text-sm font-medium text-foreground">Poster link</label>
          <input id="m-poster" placeholder="https://" className={inputClass} aria-invalid={!!e.posterUrl} {...form.register('posterUrl')} />
          <FieldError message={e.posterUrl?.message} />
        </div>
        <div>
          <label htmlFor="m-banner" className="mb-1.5 block text-sm font-medium text-foreground">Banner link</label>
          <input id="m-banner" placeholder="https://" className={inputClass} aria-invalid={!!e.bannerUrl} {...form.register('bannerUrl')} />
          <FieldError message={e.bannerUrl?.message} />
        </div>
        <fieldset className="sm:col-span-2">
          <legend className="mb-1.5 text-sm font-medium text-foreground">Genres</legend>
          {genresError ? (
            <ErrorState compact message={genresError} onRetry={loadGenres} />
          ) : genres === null ? (
            <p className="text-sm text-muted-foreground">Loading genres…</p>
          ) : genres.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              No genres yet. <Link href="/admin/genres" className="text-primary underline-offset-4 hover:underline">Add genres first.</Link>
            </p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {genres.map((g) => {
                const on = selectedGenreIds.includes(g.id);
                return (
                  <label
                    key={g.id}
                    className={cn(
                      'cursor-pointer rounded-pill border px-3 py-1.5 text-sm transition-colors focus-within:ring-2 focus-within:ring-projector',
                      on ? 'border-projector text-projector' : 'border-border text-foreground hover:border-subtle',
                    )}
                  >
                    <input type="checkbox" className="sr-only" checked={on} onChange={() => toggleGenre(g.id)} />
                    {g.name}
                  </label>
                );
              })}
            </div>
          )}
        </fieldset>
        <div className="sm:col-span-2">
          <Button type="submit" disabled={form.formState.isSubmitting || (!form.formState.isDirty && !genresChanged)}>
            {form.formState.isSubmitting ? 'Saving…' : 'Save details'}
          </Button>
        </div>
      </form>
    </section>
  );
}

function SeasonsSection({ title, onChanged, onError }: { title: Title; onChanged: () => void; onError: (m: string) => void }) {
  const isSeries = title.type === 'SERIES';
  const seasons = title.seasons ?? [];
  const seasonForm = useForm<SeasonValues>({ resolver: zodResolver(seasonSchema), defaultValues: { seasonNumber: seasons.length + 1, name: '', overview: '' } });

  const onAddSeason = seasonForm.handleSubmit(async (v) => {
    try {
      await addSeason(title.id, { seasonNumber: v.seasonNumber, name: v.name || undefined, overview: v.overview || undefined });
      seasonForm.reset({ seasonNumber: seasons.length + 2, name: '', overview: '' });
      onChanged();
    } catch (e) {
      onError(e instanceof ApiError ? e.message : "Couldn't add the season. Try again.");
    }
  });

  return (
    <section aria-labelledby="seasons-heading" className="border-t border-border pt-8">
      <h2 id="seasons-heading" className="text-xl text-foreground">{isSeries ? 'Seasons and episodes' : 'Episodes'}</h2>
      {!isSeries ? (
        <p className="mt-3 text-sm text-muted-foreground">Episodes apply to series only. Movies play directly from their media.</p>
      ) : (
        <>
          {seasons.length === 0 && <p className="mt-3 text-sm text-muted-foreground">No seasons yet. Add one below.</p>}
          <div className="mt-6 space-y-10">
            {seasons.map((season) => (
              <SeasonBlock key={season.id} season={season} onChanged={onChanged} onError={onError} />
            ))}
          </div>

          <form onSubmit={onAddSeason} noValidate className="mt-10 grid max-w-3xl gap-5 border-t border-border pt-8 sm:grid-cols-[120px_1fr]">
            <div>
              <label htmlFor="s-number" className="mb-1.5 block text-sm font-medium text-foreground">Season</label>
              <input id="s-number" type="number" min={1} className={inputClass} aria-invalid={!!seasonForm.formState.errors.seasonNumber} {...seasonForm.register('seasonNumber')} />
              <FieldError message={seasonForm.formState.errors.seasonNumber?.message} />
            </div>
            <div>
              <label htmlFor="s-name" className="mb-1.5 block text-sm font-medium text-foreground">Name</label>
              <input id="s-name" className={inputClass} {...seasonForm.register('name')} />
            </div>
            <div className="sm:col-span-2">
              <label htmlFor="s-overview" className="mb-1.5 block text-sm font-medium text-foreground">Overview</label>
              <textarea id="s-overview" rows={2} className={cn(inputClass, 'h-auto py-2.5')} {...seasonForm.register('overview')} />
            </div>
            <div className="sm:col-span-2">
              <Button type="submit" variant="secondary" disabled={seasonForm.formState.isSubmitting}>Add season</Button>
            </div>
          </form>
        </>
      )}
    </section>
  );
}

function SeasonBlock({ season, onChanged, onError }: { season: Season; onChanged: () => void; onError: (m: string) => void }) {
  const [open, setOpen] = useState(false);
  const episodes = season.episodes ?? [];
  const form = useForm<EpisodeValues>({
    resolver: zodResolver(episodeSchema),
    defaultValues: { episodeNumber: episodes.length + 1, name: '', overview: '', minutes: '' },
  });

  const onAdd = form.handleSubmit(async (v) => {
    try {
      await addEpisode(season.id, {
        episodeNumber: v.episodeNumber,
        name: v.name,
        overview: v.overview || undefined,
        durationSeconds: v.minutes ? Number(v.minutes) * 60 : undefined,
      });
      form.reset({ episodeNumber: episodes.length + 2, name: '', overview: '', minutes: '' });
      onChanged();
    } catch (e) {
      onError(e instanceof ApiError ? e.message : "Couldn't add the episode. Try again.");
    }
  });

  return (
    <div>
      <div className="flex items-baseline justify-between gap-4">
        <h3 className="text-lg text-foreground">{season.name || `Season ${season.seasonNumber}`}</h3>
        <button type="button" onClick={() => setOpen((o) => !o)} aria-expanded={open} className="text-sm text-primary hover:underline">
          {open ? 'Close' : 'Add episode'}
        </button>
      </div>

      {episodes.length > 0 ? (
        <ol className="mt-3 divide-y divide-border border-y border-border text-sm">
          {episodes.map((ep) => (
            <li key={ep.id} className="grid grid-cols-[2.5rem_1fr_auto] items-center gap-4 py-3">
              <span className="text-right text-subtle">{ep.episodeNumber}</span>
              <span className="min-w-0 truncate text-foreground">{ep.name}</span>
              <span className="text-xs text-subtle">
                {ep.mediaReady ? 'Media ready' : 'Awaiting media'}
              </span>
            </li>
          ))}
        </ol>
      ) : (
        <p className="mt-3 text-sm text-muted-foreground">No episodes in this season yet.</p>
      )}

      {open && (
        <form onSubmit={onAdd} noValidate className="mt-5 grid max-w-3xl gap-5 sm:grid-cols-[100px_1fr_120px]">
          <div>
            <label htmlFor={`e-${season.id}-n`} className="mb-1.5 block text-sm font-medium text-foreground">Episode</label>
            <input id={`e-${season.id}-n`} type="number" min={1} className={inputClass} aria-invalid={!!form.formState.errors.episodeNumber} {...form.register('episodeNumber')} />
            <FieldError message={form.formState.errors.episodeNumber?.message} />
          </div>
          <div>
            <label htmlFor={`e-${season.id}-name`} className="mb-1.5 block text-sm font-medium text-foreground">Name</label>
            <input id={`e-${season.id}-name`} className={inputClass} aria-invalid={!!form.formState.errors.name} {...form.register('name')} />
            <FieldError message={form.formState.errors.name?.message} />
          </div>
          <div>
            <label htmlFor={`e-${season.id}-min`} className="mb-1.5 block text-sm font-medium text-foreground">Minutes</label>
            <input id={`e-${season.id}-min`} inputMode="numeric" className={inputClass} aria-invalid={!!form.formState.errors.minutes} {...form.register('minutes')} />
            <FieldError message={form.formState.errors.minutes?.message} />
          </div>
          <div className="sm:col-span-3">
            <label htmlFor={`e-${season.id}-ov`} className="mb-1.5 block text-sm font-medium text-foreground">Overview</label>
            <textarea id={`e-${season.id}-ov`} rows={2} className={cn(inputClass, 'h-auto py-2.5')} {...form.register('overview')} />
          </div>
          <div className="sm:col-span-3">
            <Button type="submit" variant="secondary" disabled={form.formState.isSubmitting}>Add episode</Button>
          </div>
        </form>
      )}
    </div>
  );
}

function CreditsSection({ title, onSaved, onError }: { title: Title; onSaved: (cast: CastMember[]) => void; onError: (m: string) => void }) {
  const initial: CreditsValues['credits'] = (title.cast ?? []).map((c) => ({
    personId: c.personId,
    role: c.role as CreditRole,
    characterName: c.characterName ?? '',
  }));
  const form = useForm<CreditsValues>({ resolver: zodResolver(creditsSchema), defaultValues: { credits: initial } });
  const fields = useFieldArray({ control: form.control, name: 'credits' });

  const onSave = form.handleSubmit(async (v) => {
    try {
      const cast = await setCredits(
        title.id,
        v.credits.map((c) => ({ personId: c.personId, role: c.role, characterName: c.characterName || undefined })),
      );
      form.reset({ credits: v.credits });
      onSaved(cast);
    } catch (e) {
      onError(e instanceof ApiError ? e.message : "Couldn't save credits. Check the person IDs and try again.");
    }
  });

  return (
    <section aria-labelledby="credits-heading" className="border-t border-border pt-8">
      <h2 id="credits-heading" className="text-xl text-foreground">Cast and crew</h2>
      <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
        Create a person below, then paste the ID it returns into a credit. The API has no list of people yet, so IDs come from here.
      </p>

      <form onSubmit={onSave} noValidate className="mt-6 max-w-3xl space-y-4">
        {fields.fields.length === 0 && <p className="text-sm text-muted-foreground">No credits yet.</p>}
        {fields.fields.map((field, index) => (
          <div key={field.id} className="grid gap-4 sm:grid-cols-[1fr_140px_1fr_auto] sm:items-start">
            <div>
              <label htmlFor={`c-${index}-id`} className="mb-1.5 block text-sm font-medium text-foreground">Person ID</label>
              <input id={`c-${index}-id`} className={inputClass} aria-invalid={!!form.formState.errors.credits?.[index]?.personId} {...form.register(`credits.${index}.personId`)} />
              <FieldError message={form.formState.errors.credits?.[index]?.personId?.message} />
            </div>
            <div>
              <label htmlFor={`c-${index}-role`} className="mb-1.5 block text-sm font-medium text-foreground">Role</label>
              <select id={`c-${index}-role`} className={inputClass} {...form.register(`credits.${index}.role`)}>
                <option value="ACTOR">Actor</option>
                <option value="DIRECTOR">Director</option>
                <option value="WRITER">Writer</option>
                <option value="PRODUCER">Producer</option>
              </select>
            </div>
            <div>
              <label htmlFor={`c-${index}-char`} className="mb-1.5 block text-sm font-medium text-foreground">Character</label>
              <input id={`c-${index}-char`} className={inputClass} {...form.register(`credits.${index}.characterName`)} />
            </div>
            <div className="pt-7">
              <Button type="button" variant="ghost" size="sm" onClick={() => fields.remove(index)}>Remove</Button>
            </div>
          </div>
        ))}

        <div className="flex flex-wrap gap-3 pt-2">
          <Button type="button" variant="secondary" onClick={() => fields.append({ personId: '', role: 'ACTOR', characterName: '' })}>
            Add credit
          </Button>
          <Button type="submit" disabled={form.formState.isSubmitting || !form.formState.isDirty}>
            {form.formState.isSubmitting ? 'Saving…' : 'Save credits'}
          </Button>
        </div>
      </form>

      <PersonForm onError={onError} />
    </section>
  );
}

function PersonForm({ onError }: { onError: (m: string) => void }) {
  const [created, setCreated] = useState<{ id: string; name: string } | null>(null);
  const form = useForm<PersonValues>({ resolver: zodResolver(personSchema), defaultValues: { name: '', avatarUrl: '' } });

  const onSubmit = form.handleSubmit(async (v) => {
    try {
      const person = await createPerson({ name: v.name, avatarUrl: v.avatarUrl || undefined });
      setCreated({ id: person.id, name: person.name });
      form.reset({ name: '', avatarUrl: '' });
    } catch (e) {
      onError(e instanceof ApiError ? e.message : "Couldn't create the person. Try again.");
    }
  });

  return (
    <div className="mt-12 border-t border-border pt-8">
      <h3 className="text-lg text-foreground">Add a person</h3>
      <form onSubmit={onSubmit} noValidate className="mt-4 grid max-w-3xl gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-start">
        <div>
          <label htmlFor="p-name" className="mb-1.5 block text-sm font-medium text-foreground">Name</label>
          <input id="p-name" className={inputClass} aria-invalid={!!form.formState.errors.name} {...form.register('name')} />
          <FieldError message={form.formState.errors.name?.message} />
        </div>
        <div>
          <label htmlFor="p-avatar" className="mb-1.5 block text-sm font-medium text-foreground">Photo link</label>
          <input id="p-avatar" placeholder="https://" className={inputClass} aria-invalid={!!form.formState.errors.avatarUrl} {...form.register('avatarUrl')} />
          <FieldError message={form.formState.errors.avatarUrl?.message} />
        </div>
        <div className="sm:pt-7">
          <Button type="submit" variant="secondary" disabled={form.formState.isSubmitting}>Create person</Button>
        </div>
      </form>
      {created && (
        <p role="status" className="mt-4 text-sm text-muted-foreground">
          {created.name} was added. Their ID is <span className="select-all break-all font-mono text-foreground">{created.id}</span>.
        </p>
      )}
    </div>
  );
}

function stateLabel(s: Title['publicationState']): string {
  return s === 'PUBLISHED' ? 'Published' : s === 'UNPUBLISHED' ? 'Unpublished' : 'Draft';
}
