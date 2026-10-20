'use client';

import React, { useState, useEffect, useCallback, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Navbar from '../../components/Navbar';
import Footer from '../../components/Footer';
import TitleCard from '../../components/TitleCard';
import { Title } from '../../types';
import { searchTitles, getSearchSuggestions } from '../../lib/api';
import { Search, X, Film, Tv, Sparkles, Filter, Loader2 } from 'lucide-react';

function SearchContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const initialQuery = searchParams.get('q') || '';
  const initialType = searchParams.get('type') || '';

  const [query, setQuery] = useState(initialQuery);
  const [selectedType, setSelectedType] = useState<string>(initialType);
  const [titles, setTitles] = useState<Title[]>([]);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [totalResults, setTotalResults] = useState(0);

  const fetchResults = useCallback(async (searchQuery: string, typeFilter: string) => {
    setIsLoading(true);
    try {
      const page = await searchTitles(searchQuery, typeFilter, 0, 30);
      const mappedTitles: Title[] = page.content.map((item) => ({
        id: item.id,
        type: item.type,
        title: item.title,
        synopsis: item.synopsis,
        releaseYear: item.releaseYear,
        maturityRating: item.maturityRating,
        durationMinutes: 110,
        posterUrl: item.posterUrl,
        backdropUrl: item.posterUrl,
        publicationState: 'PUBLISHED',
        tags: item.genres || [],
      }));
      setTitles(mappedTitles);
      setTotalResults(page.totalElements);
    } catch (err) {
      console.error('Search failed:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Sync with URL query on mount or back/forward navigation
  useEffect(() => {
    const q = searchParams.get('q') || '';
    const t = searchParams.get('type') || '';
    setQuery(q);
    setSelectedType(t);
    fetchResults(q, t);
  }, [searchParams, fetchResults]);

  // Debounced live suggestions when typing
  useEffect(() => {
    if (!query.trim() || query.length < 2) {
      setSuggestions([]);
      return;
    }

    const timer = setTimeout(async () => {
      const res = await getSearchSuggestions(query, 5);
      setSuggestions(res);
    }, 250);

    return () => clearTimeout(timer);
  }, [query]);

  // Handle query input commit
  const handleSearchSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const params = new URLSearchParams();
    if (query.trim()) params.append('q', query.trim());
    if (selectedType) params.append('type', selectedType);
    router.push(`/search?${params.toString()}`);
    fetchResults(query, selectedType);
  };

  const handleTypeChange = (type: string) => {
    const nextType = selectedType === type ? '' : type;
    setSelectedType(nextType);
    const params = new URLSearchParams();
    if (query.trim()) params.append('q', query.trim());
    if (nextType) params.append('type', nextType);
    router.push(`/search?${params.toString()}`);
    fetchResults(query, nextType);
  };

  const handleSelectSuggestion = (suggestion: string) => {
    setQuery(suggestion);
    setSuggestions([]);
    const params = new URLSearchParams();
    params.append('q', suggestion);
    if (selectedType) params.append('type', selectedType);
    router.push(`/search?${params.toString()}`);
    fetchResults(suggestion, selectedType);
  };

  const clearSearch = () => {
    setQuery('');
    setSuggestions([]);
    const params = new URLSearchParams();
    if (selectedType) params.append('type', selectedType);
    router.push(`/search?${params.toString()}`);
    fetchResults('', selectedType);
  };

  const popularSearches = ['Sci-Fi', 'Cyberpunk', 'Space', 'Dune', 'Kyoto'];

  return (
    <div className="min-h-screen bg-background text-text-primary flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-16 w-full">
        {/* Header & Search Bar */}
        <div className="max-w-3xl mx-auto mb-10">
          <form onSubmit={handleSearchSubmit} className="relative">
            <div className="relative flex items-center bg-surface-elevated/80 border border-border/80 rounded-2xl px-5 py-4 focus-within:border-accent-cyan focus-within:shadow-lg focus-within:shadow-cyan-500/10 transition-all">
              <Search className="w-5 h-5 text-accent-cyan mr-3 flex-shrink-0" />
              <input
                type="text"
                placeholder="Search movies, TV shows, genres, actors..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                className="bg-transparent text-base sm:text-lg text-text-primary placeholder:text-text-muted focus:outline-none w-full"
                autoFocus
              />
              {query && (
                <button
                  type="button"
                  onClick={clearSearch}
                  className="p-1 rounded-full text-text-muted hover:text-text-primary hover:bg-white/5 transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>

            {/* Suggestions Dropdown */}
            {suggestions.length > 0 && (
              <div className="absolute top-full left-0 right-0 mt-2 bg-surface-elevated/95 backdrop-blur-xl border border-border rounded-xl shadow-2xl overflow-hidden z-30">
                <div className="px-4 py-2 text-xs font-semibold uppercase tracking-wider text-text-muted border-b border-border/50">
                  Instant Suggestions
                </div>
                {suggestions.map((item) => (
                  <button
                    key={item}
                    type="button"
                    onClick={() => handleSelectSuggestion(item)}
                    className="w-full text-left px-4 py-2.5 text-sm hover:bg-white/5 hover:text-accent-cyan flex items-center space-x-2 transition-colors"
                  >
                    <Search className="w-3.5 h-3.5 text-text-muted" />
                    <span>{item}</span>
                  </button>
                ))}
              </div>
            )}
          </form>

          {/* Filter Pills */}
          <div className="flex flex-wrap items-center justify-between gap-3 mt-4">
            <div className="flex items-center space-x-2">
              <button
                type="button"
                onClick={() => handleTypeChange('')}
                className={`px-3.5 py-1.5 rounded-full text-xs font-medium transition-all ${
                  selectedType === ''
                    ? 'bg-accent-cyan text-black font-semibold shadow-md shadow-cyan-500/20'
                    : 'bg-surface-elevated border border-border text-text-secondary hover:text-white'
                }`}
              >
                All
              </button>
              <button
                type="button"
                onClick={() => handleTypeChange('MOVIE')}
                className={`flex items-center space-x-1.5 px-3.5 py-1.5 rounded-full text-xs font-medium transition-all ${
                  selectedType === 'MOVIE'
                    ? 'bg-purple-600 text-white font-semibold shadow-md shadow-purple-500/20'
                    : 'bg-surface-elevated border border-border text-text-secondary hover:text-white'
                }`}
              >
                <Film className="w-3 h-3" />
                <span>Movies</span>
              </button>
              <button
                type="button"
                onClick={() => handleTypeChange('SERIES')}
                className={`flex items-center space-x-1.5 px-3.5 py-1.5 rounded-full text-xs font-medium transition-all ${
                  selectedType === 'SERIES'
                    ? 'bg-pink-600 text-white font-semibold shadow-md shadow-pink-500/20'
                    : 'bg-surface-elevated border border-border text-text-secondary hover:text-white'
                }`}
              >
                <Tv className="w-3 h-3" />
                <span>Series</span>
              </button>
            </div>

            {/* Popular Searches */}
            <div className="hidden sm:flex items-center space-x-2 text-xs text-text-muted">
              <span>Popular:</span>
              {popularSearches.map((tag) => (
                <button
                  key={tag}
                  type="button"
                  onClick={() => handleSelectSuggestion(tag)}
                  className="hover:text-accent-cyan transition-colors"
                >
                  #{tag}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Results Header */}
        <div className="flex items-center justify-between mb-6 border-b border-border/50 pb-3">
          <div className="text-sm text-text-secondary">
            {isLoading ? (
              <span className="flex items-center space-x-2 text-accent-cyan">
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Searching catalog...</span>
              </span>
            ) : query.trim() ? (
              <span>
                Found <strong className="text-white">{totalResults}</strong> results for &ldquo;{query}&rdquo;
              </span>
            ) : (
              <span>Explore all titles ({totalResults})</span>
            )}
          </div>
        </div>

        {/* Results Grid */}
        {isLoading ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4 sm:gap-6">
            {Array.from({ length: 12 }).map((_, i) => (
              <div
                key={i}
                className="aspect-[2/3] rounded-card bg-surface-elevated/40 border border-border/50 animate-pulse"
              />
            ))}
          </div>
        ) : titles.length > 0 ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4 sm:gap-6">
            {titles.map((title) => (
              <div key={title.id} className="flex justify-center">
                <TitleCard title={title} />
              </div>
            ))}
          </div>
        ) : (
          /* Empty State */
          <div className="text-center py-20 bg-surface-elevated/30 rounded-2xl border border-border/50 max-w-xl mx-auto px-6">
            <div className="w-16 h-16 rounded-2xl bg-purple-950/40 border border-purple-500/20 flex items-center justify-center mx-auto mb-4 text-purple-400">
              <Sparkles className="w-8 h-8" />
            </div>
            <h3 className="text-lg font-heading font-bold text-white mb-2">No matching titles found</h3>
            <p className="text-sm text-text-secondary mb-6">
              We couldn&apos;t find anything matching &ldquo;{query}&rdquo;. Try checking for typos or searching for a different keyword or genre.
            </p>
            <div className="flex flex-wrap justify-center gap-2">
              {['Sci-Fi', 'Space', 'Action', 'Drama', 'Thriller'].map((suggestion) => (
                <button
                  key={suggestion}
                  type="button"
                  onClick={() => handleSelectSuggestion(suggestion)}
                  className="px-3 py-1.5 rounded-lg bg-surface-elevated border border-border text-xs text-text-secondary hover:text-white hover:border-accent-cyan transition-colors"
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
}

export default function SearchPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-background flex items-center justify-center text-text-muted">
        <Loader2 className="w-8 h-8 animate-spin text-accent-cyan" />
      </div>
    }>
      <SearchContent />
    </Suspense>
  );
}
