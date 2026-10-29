'use client';

import React, { Suspense, useCallback, useEffect, useRef, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Search, X } from 'lucide-react';
import { Poster } from '@/components/cards/Poster';
import { EmptyState, ErrorState, PosterGridSkeleton, Spinner } from '@/components/common';
import { ApiError, searchTitles, getSearchSuggestions, fetchGenres, type GenreOption, type SearchFilters } from '@/lib/api';
import { cn } from '@/lib/utils';
import type { Title } from '@/types';

const TYPES = [
  { value: '', label: 'All' },
  { value: 'MOVIE', label: 'Movies' },
  { value: 'SERIES', label: 'Series' },
] as const;

const RATINGS = ['G', 'PG', 'PG-13', 'R', 'TV-MA'];

function SearchContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const [query, setQuery] = useState(searchParams.get('q') || '');
  const [selectedType, setSelectedType] = useState(searchParams.get('type') || '');
  const [titles, setTitles] = useState<Title[]>([]);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [totalResults, setTotalResults] = useState(0);
  const [error, setError] = useState<string | null>(null);
  // Filters are read through a ref so every call site sees the latest values without re-creating callbacks.
  const [filters, setFilters] = useState<SearchFilters>({});
  const filtersRef = useRef<SearchFilters>({});
  const [genreOptions, setGenreOptions] = useState<GenreOption[]>([]);

  useEffect(() => {
    fetchGenres().then(setGenreOptions).catch(() => setGenreOptions([]));
  }, []);

  const fetchResults = useCallback(async (searchQuery: string, typeFilter: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const page = await searchTitles(searchQuery, typeFilter, 0, 30, filtersRef.current);
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
        })),
      );
      setTotalResults(page.totalElements);
    } catch (err) {
      setTitles([]);
      setTotalResults(0);
      setError(err instanceof ApiError ? err.message : "Search isn't available right now. Try again.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Keep the page in step with the URL (mount, back and forward).
  useEffect(() => {
    const q = searchParams.get('q') || '';
    const t = searchParams.get('type') || '';
    setQuery(q);
    setSelectedType(t);
    fetchResults(q, t);
  }, [searchParams, fetchResults]);

  // Debounced suggestions while typing.
  useEffect(() => {
    if (query.trim().length < 2) {
      setSuggestions([]);
      return;
    }
    const timer = setTimeout(() => {
      getSearchSuggestions(query, 5).then(setSuggestions).catch(() => setSuggestions([]));
    }, 250);
    return () => clearTimeout(timer);
  }, [query]);

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
    pushUrl(query, selectedType);
  };

  const handleType = (type: string) => {
    setSelectedType(type);
    pushUrl(query, type);
  };

  const handleSuggestion = (suggestion: string) => {
    setQuery(suggestion);
    setSuggestions([]);
    pushUrl(suggestion, selectedType);
  };

  const clearQuery = () => {
    setQuery('');
    setSuggestions([]);
    pushUrl('', selectedType);
  };

  const updateFilter = (patch: SearchFilters) => {
    filtersRef.current = { ...filtersRef.current, ...patch };
    setFilters(filtersRef.current);
    fetchResults(query, selectedType);
  };

  const hasQuery = query.trim().length > 0;
  const selectClass =
    'h-9 rounded-card border border-border bg-surface px-3 text-sm text-foreground focus:border-primary focus:outline-none';

  return (
    <div className="mx-auto max-w-container px-4 pb-16 sm:px-6 lg:px-10">
      <header className="mx-auto max-w-2xl pt-6">
        <h1 className="text-3xl sm:text-4xl">Search</h1>

        <form onSubmit={handleSubmit} role="search" className="relative mt-6">
          <label htmlFor="search-input" className="sr-only">
            Search titles
          </label>
          <div className="flex h-12 items-center gap-3 rounded-card border border-border bg-surface px-4 focus-within:border-primary">
            <Search className="h-5 w-5 shrink-0 text-subtle" aria-hidden />
            <input
              id="search-input"
              type="text"
              placeholder="Titles, genres, people"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              autoComplete="off"
              className="w-full bg-transparent text-base text-foreground placeholder:text-subtle focus:outline-none"
            />
            {query && (
              <button type="button" onClick={clearQuery} aria-label="Clear search" className="text-subtle hover:text-foreground">
                <X className="h-4 w-4" aria-hidden />
              </button>
            )}
          </div>

          {suggestions.length > 0 && (
            <ul className="panel absolute inset-x-0 top-full z-30 mt-2 overflow-hidden py-1" aria-label="Suggestions">
              {suggestions.map((item) => (
                <li key={item}>
                  <button
                    type="button"
                    onClick={() => handleSuggestion(item)}
                    className="flex w-full items-center gap-3 px-4 py-2.5 text-left text-sm text-foreground hover:bg-surface-elevated"
                  >
                    <Search className="h-3.5 w-3.5 text-subtle" aria-hidden />
                    {item}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </form>

        <div className="mt-5 flex flex-wrap items-end gap-x-6 gap-y-4">
          <div role="tablist" aria-label="Type" className="flex gap-5 border-b border-border">
            {TYPES.map((t) => (
              <button
                key={t.value || 'all'}
                type="button"
                role="tab"
                aria-selected={selectedType === t.value}
                onClick={() => handleType(t.value)}
                className={cn(
                  '-mb-px border-b-2 pb-2 text-sm transition-colors',
                  selectedType === t.value ? 'border-primary text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground',
                )}
              >
                {t.label}
              </button>
            ))}
          </div>

          <div className="flex flex-wrap gap-2">
            <label className="sr-only" htmlFor="filter-genre">Genre</label>
            <select id="filter-genre" aria-label="Genre" value={filters.genre ?? ''} onChange={(e) => updateFilter({ genre: e.target.value || undefined })} className={selectClass}>
              <option value="">Any genre</option>
              {genreOptions.map((g) => (
                <option key={g.id} value={g.slug}>{g.name}</option>
              ))}
            </select>
            <select aria-label="Maturity rating" value={filters.maturity ?? ''} onChange={(e) => updateFilter({ maturity: e.target.value || undefined })} className={selectClass}>
              <option value="">Any rating</option>
              {RATINGS.map((r) => (
                <option key={r} value={r}>{r}</option>
              ))}
            </select>
            <input
              aria-label="Release year"
              type="number"
              inputMode="numeric"
              min={1880}
              max={2200}
              placeholder="Year"
              value={filters.year ?? ''}
              onChange={(e) => {
                const year = Number(e.target.value);
                updateFilter({ year: e.target.value && year >= 1880 && year <= 2200 ? year : undefined });
              }}
              className={cn(selectClass, 'w-24')}
            />
          </div>
        </div>
      </header>

      <div className="mt-10">
        <p className="mb-6 text-sm text-muted-foreground" aria-live="polite">
          {isLoading
            ? 'Searching…'
            : hasQuery
              ? `${totalResults} ${totalResults === 1 ? 'result' : 'results'} for “${query.trim()}”`
              : `${totalResults} ${totalResults === 1 ? 'title' : 'titles'} in the catalog`}
        </p>

        {isLoading ? (
          <PosterGridSkeleton count={12} />
        ) : error ? (
          <ErrorState message={error} onRetry={() => fetchResults(query, selectedType)} />
        ) : titles.length > 0 ? (
          <ul className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
            {titles.map((title) => (
              <li key={title.id}>
                <Poster title={title} highlight={hasQuery ? query.trim() : undefined} />
              </li>
            ))}
          </ul>
        ) : (
          <EmptyState
            title={hasQuery ? `Nothing matches “${query.trim()}”` : 'No titles match these filters'}
            description="Check the spelling, or try a broader word or a different genre."
            action={
              <div className="flex flex-wrap justify-center gap-2">
                {['Sci-Fi', 'Drama', 'Thriller'].map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => handleSuggestion(s)}
                    className="rounded-card border border-border px-3 py-1.5 text-sm text-muted-foreground hover:border-primary hover:text-foreground"
                  >
                    {s}
                  </button>
                ))}
              </div>
            }
          />
        )}
      </div>
    </div>
  );
}

export default function SearchPage() {
  return (
    <Suspense fallback={<Spinner label="Loading search…" />}>
      <SearchContent />
    </Suspense>
  );
}
