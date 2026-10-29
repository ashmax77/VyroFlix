'use client';

import React, { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ExternalLink, MoreHorizontal, Plus, Search } from 'lucide-react';
import { MediaBadge, StateBadge } from '@/components/admin/shared';
import { EmptyState, ErrorState, Skeleton } from '@/components/common';
import { Button } from '@/components/ui/button';
import * as dropdownMenu from '@/components/ui/dropdown-menu';
import { fetchAdminTitles, publishTitle, unpublishTitle } from '@/lib/api';
import type { PublicationState, Title, TitleType } from '@/types';

export default function TitlesPage() {
  const router = useRouter();
  const [search, setSearch] = useState('');
  const [type, setType] = useState<TitleType | 'ALL'>('ALL');
  const [state, setState] = useState<PublicationState | 'ALL'>('ALL');
  const [sort, setSort] = useState<'updated' | 'name'>('updated');
  const [titles, setTitles] = useState<Title[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    fetchAdminTitles()
      .then((data) => setTitles(data))
      .catch((e) => setError(e instanceof Error ? e.message : 'Could not load titles.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(load, [load]);

  const filtered = titles
    .filter((t) => {
      const matchSearch =
        !search || t.title.toLowerCase().includes(search.toLowerCase());
      const matchType = type === 'ALL' || t.type === type;
      const matchState = state === 'ALL' || t.publicationState === state;
      return matchSearch && matchType && matchState;
    })
    .sort((a, b) => {
      if (sort === 'name') return a.title.localeCompare(b.title);
      return 0;
    });

  const handleToggleState = async (title: Title) => {
    try {
      if (title.publicationState === 'PUBLISHED') {
        await unpublishTitle(title.id);
      } else {
        await publishTitle(title.id);
      }
      load();
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
            Titles
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Manage movies, series catalog entries, media references, and publication states.
          </p>
        </div>

        <Button variant="gradient" asChild>
          <Link href="/admin/titles/new">
            <Plus className="h-4 w-4" />
            New title
          </Link>
        </Button>
      </div>

      {/* Filter bar */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative">
          <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-subtle" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search titles…"
            aria-label="Search titles"
            className="h-10 w-64 rounded-full border border-border bg-surface pl-10 pr-4 text-sm text-foreground placeholder:text-muted-foreground focus:border-cyan focus:outline-none"
          />
        </div>

        <select
          value={type}
          onChange={(e) => setType(e.target.value as TitleType | 'ALL')}
          aria-label="Filter by type"
          className="h-10 rounded-full border border-border bg-surface px-4 text-sm text-foreground focus:border-cyan focus:outline-none"
        >
          <option value="ALL">All types</option>
          <option value="MOVIE">Movie</option>
          <option value="SERIES">Series</option>
        </select>

        <select
          value={state}
          onChange={(e) => setState(e.target.value as PublicationState | 'ALL')}
          aria-label="Filter by state"
          className="h-10 rounded-full border border-border bg-surface px-4 text-sm text-foreground focus:border-cyan focus:outline-none"
        >
          <option value="ALL">All states</option>
          <option value="DRAFT">Draft</option>
          <option value="PUBLISHED">Published</option>
          <option value="UNPUBLISHED">Unpublished</option>
        </select>

        <select
          value={sort}
          onChange={(e) => setSort(e.target.value as 'updated' | 'name')}
          aria-label="Sort by"
          className="h-10 rounded-full border border-border bg-surface px-4 text-sm text-foreground focus:border-cyan focus:outline-none"
        >
          <option value="updated">Last updated</option>
          <option value="name">Name</option>
        </select>

        {selectedIds.size > 0 && (
          <span className="text-sm font-medium text-cyan">
            {selectedIds.size} selected
          </span>
        )}
      </div>

      {error ? (
        <ErrorState message={error} onRetry={load} />
      ) : loading && titles.length === 0 ? (
        <div className="space-y-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-16 rounded-xl" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          title="No titles found"
          description="Try adjusting your search query or filter settings."
        />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-border bg-surface shadow-card">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-border text-xs text-muted-foreground">
                <th className="w-12 px-4 py-3.5">
                  <input
                    type="checkbox"
                    checked={
                      filtered.length > 0 &&
                      filtered.every((t) => selectedIds.has(t.id))
                    }
                    onChange={(e) =>
                      setSelectedIds(
                        e.target.checked
                          ? new Set(filtered.map((t) => t.id))
                          : new Set(),
                      )
                    }
                    className="rounded accent-cyan"
                  />
                </th>
                <th className="px-4 py-3.5 font-medium">Title</th>
                <th className="px-4 py-3.5 font-medium">Type</th>
                <th className="px-4 py-3.5 font-medium">State</th>
                <th className="px-4 py-3.5 font-medium">Media</th>
                <th className="px-4 py-3.5 font-medium">Release</th>
                <th className="w-12 px-4 py-3.5" />
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {filtered.map((t) => (
                <tr
                  key={t.id}
                  className="cursor-pointer transition-colors hover:bg-surface-elevated"
                  onClick={() => router.push(`/admin/titles/${t.id}`)}
                >
                  <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                    <input
                      type="checkbox"
                      checked={selectedIds.has(t.id)}
                      onChange={(e) => {
                        const next = new Set(selectedIds);
                        if (e.target.checked) next.add(t.id);
                        else next.delete(t.id);
                        setSelectedIds(next);
                      }}
                      className="rounded accent-cyan"
                    />
                  </td>

                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      {t.posterUrl ? (
                        <img
                          src={t.posterUrl}
                          alt=""
                          className="h-12 w-8 shrink-0 rounded object-cover shadow-sm"
                        />
                      ) : (
                        <div className="grid h-12 w-8 shrink-0 place-items-center rounded bg-surface-elevated text-[10px] text-muted-foreground">
                          No art
                        </div>
                      )}
                      <div>
                        <span className="font-semibold text-foreground hover:text-cyan">
                          {t.title}
                        </span>
                        {t.genres && t.genres.length > 0 && (
                          <p className="text-xs text-subtle">
                            {t.genres.map((g) => g.name).join(', ')}
                          </p>
                        )}
                      </div>
                    </div>
                  </td>

                  <td className="px-4 py-3 text-muted-foreground">
                    {t.type === 'MOVIE' ? 'Movie' : 'Series'}
                  </td>

                  <td className="px-4 py-3">
                    <StateBadge state={t.publicationState} />
                  </td>

                  <td className="px-4 py-3">
                    <MediaBadge status="READY" />
                  </td>

                  <td className="px-4 py-3 text-muted-foreground">
                    {t.releaseYear || '—'}
                  </td>

                  <td className="px-4 py-3 text-right" onClick={(e) => e.stopPropagation()}>
                    <dropdownMenu.DropdownMenu>
                      <dropdownMenu.DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" aria-label="More actions">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </dropdownMenu.DropdownMenuTrigger>
                      <dropdownMenu.DropdownMenuContent align="end">
                        <dropdownMenu.DropdownMenuItem onSelect={() => router.push(`/admin/titles/${t.id}`)}>
                          Edit details
                        </dropdownMenu.DropdownMenuItem>
                        <dropdownMenu.DropdownMenuItem onSelect={() => router.push(`/title/${t.id}`)}>
                          View on site
                        </dropdownMenu.DropdownMenuItem>
                        <dropdownMenu.DropdownMenuItem onSelect={() => handleToggleState(t)}>
                          {t.publicationState === 'PUBLISHED' ? 'Unpublish' : 'Publish'}
                        </dropdownMenu.DropdownMenuItem>
                      </dropdownMenu.DropdownMenuContent>
                    </dropdownMenu.DropdownMenu>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
