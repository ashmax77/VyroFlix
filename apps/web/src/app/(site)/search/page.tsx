'use client';

import React, { Suspense, useCallback, useEffect, useRef, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Search, TrendingUp, X } from 'lucide-react';
import { Poster } from '@/components/cards/Poster';
import { EmptyState, ErrorState, PageContainer, PosterGrid, PosterGridSkeleton, Spinner } from '@/components/common';
import { searchTitles, getSearchSuggestions, fetchGenres, type GenreOption, type SearchFilters } from '@/lib/api';
import { cn } from '@/lib/utils';
import type { Title } from '@/types';

const TRENDING_SEARCHES = ['Neon Tides', 'Starfall Academy', 'Sci-Fi', 'Mirrorlake', 'Orbit of Ash', 'Documentary', 'Thriller'];
const RECENT_KEY = 'vyroflix-recent-searches';

const TYPES = [
  { value: '', label: 'All' },
  { value: 'MOVIE', label: 'Movies' },
  { value: 'SERIES', label: 'Series' },
] as const;

function SearchContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const [query, setQuery] = useState(searchParams.get('q') || '');
  const [selectedType, setSelectedType] = useState(searchParams.get('type') || '');
  const [titles, setTitles] = useState<Title[]>([]);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [recent, setRecent] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [totalResults, setTotalResults] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const [genreOptions, setGenreOptions] = useState<GenreOption[]>([]);
  const [selectedGenre, setSelectedGenre] = useState<string>('');

  useEffect(() => {
    try {
      setRecent(JSON.parse(window.localStorage.getItem(RECENT_KEY) ?? '[]'));
    } catch {
      // ignore
    }
    fetchGenres().then(setGenreOptions).catch(() => setGenreOptions([]));
  }, []);

  const fetchResults = useCallback(async (searchQuery: string, typeFilter: string, genreFilter: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const page = await searchTitles(searchQuery, typeFilter, 0, 30, { genre: genreFilter || undefined });
      setTitles(
        page.content.map((item) => ({
          id: item.id,
          type: item.type,
          title: item.title,
          synopsis: item.synopsis,
          releaseYear: item.releaseYear,
          maturityRating: item.maturityRating,
          posterUrl: item.posterUrl,
          bannerUrl: null,
          publicationState: 'PUBLISHED',
          genres: (item.genres || []).map((name) => ({ id: name, name, slug: name })),
        }))
      );
      setTotalResults(page.totalElements);
    } catch {
      setTitles([]);
      setTotalResults(0);
      setError("Search isn't available right now. Try again.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    const q = searchParams.get('q') || '';
    const t = searchParams.get('type') || '';
    setQuery(q);
    setSelectedType(t);
    fetchResults(q, t, selectedGenre);
  }, [searchParams, selectedGenre, fetchResults]);

  useEffect(() => {
    if (query.trim().length < 2) {
      setSuggestions([]);
      return;
    }
    const timer = setTimeout(() => {
      getSearchSuggestions(query).then(setSuggestions).catch(() => setSuggestions([]));
    }, 200);
    return () => clearTimeout(timer);
  }, [query]);

  const saveRecent = (term: string) => {
    if (term.trim().length < 2) return;
    try {
      const next = [term.trim(), ...recent.filter((x) => x.toLowerCase() !== term.trim().toLowerCase())].slice(0, 6);
      setRecent(next);
      window.localStorage.setItem(RECENT_KEY, JSON.stringify(next));
    } catch {
      // ignore
    }
  };

  const pushUrl = (q: string, type: string) => {
    const params = new URLSearchParams();
    if (q.trim()) params.append('q', q.trim());
    if (type) params.append('type', type);
    const qs = params.toString();
    router.push(qs ? `/search?${qs}` : '/search');
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSuggestions([]);
    saveRecent(query);
    pushUrl(query, selectedType);
  };

  const handleSelectSuggestion = (term: string) => {
    setQuery(term);
    setSuggestions([]);
    saveRecent(term);
    pushUrl(term, selectedType);
  };

  const clearQuery = () => {
    setQuery('');
    setSuggestions([]);
    pushUrl('', selectedType);
  };

  const hasQuery = query.trim().length > 0;

  return (
    <PageContainer className="pt-28 pb-24">
      {/* Search Input Hero Box */}
      <div className="relative max-w-4xl mx-auto">
        <form onSubmit={handleSubmit} role="search" className="relative">
          <Search className="absolute left-5 top-1/2 h-6 w-6 -translate-y-1/2 text-subtle" aria-hidden />
          <input
            autoFocus
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search films, series, genres, creators…"
            aria-label="Search"
            className="h-16 w-full rounded-2xl border border-border bg-surface pl-14 pr-14 font-display text-xl text-foreground placeholder:text-subtle focus:border-cyan focus:outline-none sm:text-2xl shadow-card transition-all"
          />
          {query && (
            <button
              type="button"
              onClick={clearQuery}
              aria-label="Clear search"
              className="absolute right-5 top-1/2 -translate-y-1/2 text-subtle hover:text-foreground transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          )}

          {/* Autocomplete Suggestions Box */}
          {suggestions.length > 0 && (
            <ul className="glass absolute inset-x-0 top-full z-40 mt-2 overflow-hidden rounded-2xl p-2 shadow-card animate-page-in">
              {suggestions.map((item) => (
                <li key={item}>
                  <button
                    type="button"
                    onClick={() => handleSelectSuggestion(item)}
                    className="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-left text-sm text-foreground hover:bg-surface-elevated transition-colors"
                  >
                    <Search className="h-4 w-4 text-cyan" />
                    <span className="font-medium">{item}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </form>

        {/* Filter Pills (All / Movies / Series) */}
        <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            {TYPES.map((t) => {
              const active = selectedType === t.value;
              return (
                <button
                  key={t.value || 'all'}
                  type="button"
                  onClick={() => {
                    setSelectedType(t.value);
                    pushUrl(query, t.value);
                  }}
                  className={cn(
                    'rounded-full px-4 py-1.5 text-xs font-semibold transition-all',
                    active
                      ? 'bg-gradient-brand text-white shadow-glow'
                      : 'border border-border bg-surface text-muted-foreground hover:text-foreground hover:border-border-subtle'
                  )}
                >
                  {t.label}
                </button>
              );
            })}
          </div>

          {/* Genre select dropdown */}
          {genreOptions.length > 0 && (
            <select
              value={selectedGenre}
              onChange={(e) => setSelectedGenre(e.target.value)}
              aria-label="Filter by genre"
              className="h-8 rounded-full border border-border bg-surface px-3 text-xs text-foreground outline-none focus:border-cyan cursor-pointer"
            >
              <option value="">All Genres</option>
              {genreOptions.map((g) => (
                <option key={g.id} value={g.slug}>{g.name}</option>
              ))}
            </select>
          )}
        </div>
      </div>

      {/* Discovery Tags when query is empty */}
      {!hasQuery && (
        <div className="mt-12 max-w-4xl mx-auto grid gap-8 md:grid-cols-2 animate-page-in">
          {recent.length > 0 && (
            <section className="rounded-2xl border border-border bg-surface p-6">
              <h2 className="mb-4 text-xs font-bold uppercase tracking-wider text-subtle">Recent Searches</h2>
              <div className="flex flex-wrap gap-2">
                {recent.map((r) => (
                  <button
                    key={r}
                    onClick={() => handleSelectSuggestion(r)}
                    className="rounded-full border border-border bg-surface-elevated px-3.5 py-1.5 text-xs font-medium text-foreground hover:border-cyan transition-colors"
                  >
                    {r}
                  </button>
                ))}
              </div>
            </section>
          )}

          <section className="rounded-2xl border border-border bg-surface p-6">
            <h2 className="mb-4 text-xs font-bold uppercase tracking-wider text-subtle">Trending on VyroFlix</h2>
            <ul className="space-y-1.5">
              {TRENDING_SEARCHES.map((t) => (
                <li key={t}>
                  <button
                    onClick={() => handleSelectSuggestion(t)}
                    className="flex w-full items-center gap-3 rounded-xl px-3 py-2 text-left text-sm text-foreground hover:bg-surface-elevated transition-colors group"
                  >
                    <TrendingUp className="h-4 w-4 text-cyan group-hover:scale-110 transition-transform" />
                    <span>{t}</span>
                  </button>
                </li>
              ))}
            </ul>
          </section>
        </div>
      )}

      {/* Results Display */}
      <div className="mt-12">
        <p className="mb-6 text-xs font-bold uppercase tracking-wider text-subtle" aria-live="polite">
          {isLoading
            ? 'Searching library…'
            : hasQuery
              ? `${totalResults} ${totalResults === 1 ? 'result' : 'results'} for “${query.trim()}”`
              : `${totalResults} titles available`}
        </p>

        {isLoading ? (
          <PosterGridSkeleton count={12} />
        ) : error ? (
          <ErrorState message={error} onRetry={() => fetchResults(query, selectedType, selectedGenre)} />
        ) : titles.length > 0 ? (
          <PosterGrid>
            {titles.map((title) => (
              <Poster key={title.id} title={title} highlight={hasQuery ? query.trim() : undefined} />
            ))}
          </PosterGrid>
        ) : (
          <EmptyState
            title={hasQuery ? `Nothing found for “${query.trim()}”` : 'No titles match these filters'}
            description="Try exploring our suggested trends or adjusting your genre filters."
            action={
              <div className="flex flex-wrap justify-center gap-2 mt-2">
                {['Sci-Fi', 'Action', 'Drama', 'Thriller'].map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => handleSelectSuggestion(s)}
                    className="rounded-full border border-border bg-surface px-4 py-1.5 text-xs font-semibold text-foreground hover:border-cyan transition-colors"
                  >
                    {s}
                  </button>
                ))}
              </div>
            }
          />
        )}
      </div>
    </PageContainer>
  );
}

export default function SearchPage() {
  return (
    <Suspense fallback={<Spinner label="Loading search…" />}>
      <SearchContent />
    </Suspense>
  );
}
