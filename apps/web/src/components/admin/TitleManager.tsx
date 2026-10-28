'use client';

import React, { useState } from 'react';
import { ApiError, createTitle, deleteTitle, publishTitle, unpublishTitle, type TitleInput } from '../../lib/api';
import type { Title } from '../../types';

interface TitleManagerProps {
  titles: Title[];
  /** Reloads the list after a change. */
  onChanged: () => void;
}

/**
 * Create, publish, unpublish and delete titles. The server enforces the rules (publishing needs READY media,
 * deleting needs an unpublished title); this component shows the server's message when one is refused.
 */
export default function TitleManager({ titles, onChanged }: TitleManagerProps) {
  const [draft, setDraft] = useState<TitleInput>({ title: '', type: 'MOVIE', synopsis: '' });
  const [busyId, setBusyId] = useState<string | null>(null);
  const [message, setMessage] = useState<{ tone: 'ok' | 'error'; text: string } | null>(null);

  const run = async (id: string, action: () => Promise<unknown>, success: string) => {
    setBusyId(id);
    setMessage(null);
    try {
      await action();
      setMessage({ tone: 'ok', text: success });
      onChanged();
    } catch (e) {
      setMessage({ tone: 'error', text: e instanceof ApiError ? e.message : 'The change could not be saved.' });
    } finally {
      setBusyId(null);
    }
  };

  const submitCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!draft.title.trim()) {
      setMessage({ tone: 'error', text: 'A title name is required.' });
      return;
    }
    setBusyId('new');
    setMessage(null);
    try {
      await createTitle({ ...draft, title: draft.title.trim() });
      setDraft({ title: '', type: 'MOVIE', synopsis: '' });
      setMessage({ tone: 'ok', text: 'Draft created. Upload media, then publish it.' });
      onChanged();
    } catch (err) {
      setMessage({ tone: 'error', text: err instanceof ApiError ? err.message : 'The draft could not be created.' });
    } finally {
      setBusyId(null);
    }
  };

  return (
    <section aria-label="Title management" className="space-y-6">
      {message && (
        <p role={message.tone === 'error' ? 'alert' : 'status'} className={message.tone === 'error' ? 'text-sm text-red-400' : 'text-sm text-green-400'}>
          {message.text}
        </p>
      )}

      <form onSubmit={submitCreate} className="grid gap-3 rounded-xl border border-white/10 bg-card p-4 md:grid-cols-4">
        <input
          aria-label="New title name"
          placeholder="Title name"
          value={draft.title}
          maxLength={255}
          onChange={(e) => setDraft({ ...draft, title: e.target.value })}
          className="rounded-lg border border-white/10 bg-[#12121A] px-3 py-2 text-sm text-white md:col-span-2"
        />
        <select
          aria-label="New title type"
          value={draft.type}
          onChange={(e) => setDraft({ ...draft, type: e.target.value as TitleInput['type'] })}
          className="rounded-lg border border-white/10 bg-[#12121A] px-3 py-2 text-sm text-white"
        >
          <option value="MOVIE">Movie</option>
          <option value="SERIES">Series</option>
        </select>
        <button
          type="submit"
          disabled={busyId === 'new'}
          className="rounded-lg bg-purple-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
        >
          Create draft
        </button>
      </form>

      <ul className="divide-y divide-white/10 rounded-xl border border-white/10 bg-card">
        {titles.length === 0 && <li className="p-4 text-sm text-text-secondary">No titles yet.</li>}
        {titles.map((t) => {
          const published = t.publicationState === 'PUBLISHED';
          const busy = busyId === t.id;
          return (
            <li key={t.id} className="flex flex-wrap items-center justify-between gap-3 p-4">
              <div>
                <p className="font-semibold text-white">{t.title}</p>
                <p className="text-xs text-text-secondary">
                  {t.type} · {t.publicationState}
                </p>
              </div>
              <div className="flex gap-2">
                {published ? (
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => run(t.id, () => unpublishTitle(t.id), 'Title unpublished.')}
                    className="rounded-lg border border-white/15 px-3 py-1.5 text-xs text-white disabled:opacity-50"
                  >
                    Unpublish
                  </button>
                ) : (
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => run(t.id, () => publishTitle(t.id), 'Title published.')}
                    className="rounded-lg bg-green-600/80 px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50"
                  >
                    Publish
                  </button>
                )}
                <button
                  type="button"
                  disabled={busy || published}
                  title={published ? 'Unpublish before deleting' : 'Delete this draft'}
                  onClick={() => {
                    if (window.confirm(`Delete "${t.title}"? This cannot be undone.`)) {
                      void run(t.id, () => deleteTitle(t.id), 'Title deleted.');
                    }
                  }}
                  className="rounded-lg border border-red-500/40 px-3 py-1.5 text-xs text-red-300 disabled:opacity-40"
                >
                  Delete
                </button>
              </div>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
