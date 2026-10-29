'use client';

import React, { useState, useMemo } from 'react';
import { Filter, SearchX } from 'lucide-react';
import { Poster } from '@/components/cards/Poster';
import { EmptyState, ErrorState, PageContainer, PosterGrid, PosterGridSkeleton } from '@/components/common';
import { Button } from '@/components/ui/button';
import { MOCK_GENRES, MOCK_TITLES } from '@/lib/mock-data';
import type { Title, TitleType } from '@/types';

interface CatalogPageProps {
  heading: string;
  description: string;
  type?: TitleType;
}

const YEARS = [2024, 2023, 2022, 2021, 2020];
const RATINGS = ['ALL', 'G', 'PG', 'PG-13', 'R', 'TV-14', 'TV-MA'];

export default function CatalogPage({ heading, description, type }: CatalogPageProps) {
  const [selectedGenre, setSelectedGenre] = useState<string>('ALL');
  const [selectedYear, setSelectedYear] = useState<string>('ALL');
  const [selectedRating, setSelectedRating] = useState<string>('ALL');
  const [sort, setSort] = useState<'popular' | 'newest' | 'az'>('popular');

  const filteredTitles = useMemo(() => {
    return MOCK_TITLES.filter((t) => {
      if (type && t.type !== type) return false;
      if (selectedGenre !== 'ALL' && !t.genres?.some((g) => g.slug === selectedGenre)) return false;
      if (selectedYear !== 'ALL' && t.releaseYear !== Number(selectedYear)) return false;
      if (selectedRating !== 'ALL' && t.maturityRating !== selectedRating) return false;
      return true;
    }).sort((a, b) => {
      if (sort === 'newest') return b.releaseYear - a.releaseYear;
      if (sort === 'az') return a.title.localeCompare(b.title);
      return 0; // default popular
    });
  }, [type, selectedGenre, selectedYear, selectedRating, sort]);

  const resetFilters = () => {
    setSelectedGenre('ALL');
    setSelectedYear('ALL');
    setSelectedRating('ALL');
    setSort('popular');
  };

  return (
    <PageContainer className="pt-28 pb-24">
      {/* Page Heading */}
      <div className="max-w-3xl">
        <h1 className="text-4xl font-extrabold tracking-tight sm:text-5xl text-foreground">
          {heading}
        </h1>
        <p className="mt-3 text-base sm:text-lg text-muted-foreground leading-relaxed">
          {description}
        </p>
      </div>

      {/* Filter Toolbar */}
      <div className="mt-8 flex flex-wrap items-center gap-3" role="toolbar" aria-label="Filters">
        {/* Genre Pill Selector */}
        <div className="flex flex-wrap items-center gap-1.5">
          <button
            type="button"
            onClick={() => setSelectedGenre('ALL')}
            className={`rounded-full px-3.5 py-1.5 text-xs font-semibold transition-all ${
              selectedGenre === 'ALL'
                ? 'bg-gradient-brand text-white shadow-glow'
                : 'border border-border bg-surface text-muted-foreground hover:text-foreground hover:border-border-subtle'
            }`}
          >
            All Genres
          </button>
          {MOCK_GENRES.map((g) => (
            <button
              key={g.slug}
              type="button"
              onClick={() => setSelectedGenre(g.slug)}
              className={`rounded-full px-3.5 py-1.5 text-xs font-semibold transition-all ${
                selectedGenre === g.slug
                  ? 'bg-gradient-brand text-white shadow-glow'
                  : 'border border-border bg-surface text-muted-foreground hover:text-foreground hover:border-border-subtle'
              }`}
            >
              {g.name}
            </button>
          ))}
        </div>

        {/* Year Filter Select */}
        <select
          value={selectedYear}
          onChange={(e) => setSelectedYear(e.target.value)}
          aria-label="Year"
          className="h-8 rounded-full border border-border bg-surface px-3 text-xs text-foreground outline-none focus:border-cyan cursor-pointer"
        >
          <option value="ALL">All Years</option>
          {YEARS.map((y) => (
            <option key={y} value={String(y)}>{y}</option>
          ))}
        </select>

        {/* Rating Filter Select */}
        <select
          value={selectedRating}
          onChange={(e) => setSelectedRating(e.target.value)}
          aria-label="Maturity Rating"
          className="h-8 rounded-full border border-border bg-surface px-3 text-xs text-foreground outline-none focus:border-cyan cursor-pointer"
        >
          {RATINGS.map((r) => (
            <option key={r} value={r}>{r === 'ALL' ? 'All Ratings' : r}</option>
          ))}
        </select>

        {/* Sort Select */}
        <select
          value={sort}
          onChange={(e) => setSort(e.target.value as 'popular' | 'newest' | 'az')}
          aria-label="Sort By"
          className="ml-auto h-8 rounded-full border border-border bg-surface px-3 text-xs text-foreground outline-none focus:border-cyan cursor-pointer"
        >
          <option value="popular">Most Popular</option>
          <option value="newest">Newest First</option>
          <option value="az">A–Z Alphabetical</option>
        </select>
      </div>

      {/* Results Count & Grid */}
      <div className="mt-8">
        <p className="mb-4 text-xs font-semibold uppercase tracking-wider text-subtle">
          Showing {filteredTitles.length} {filteredTitles.length === 1 ? 'title' : 'titles'}
        </p>

        {filteredTitles.length === 0 ? (
          <EmptyState
            icon={SearchX}
            title="No matching titles found"
            description="Try clearing or broadening your filters to explore more."
            action={
              <Button variant="outline" onClick={resetFilters}>
                Reset Filters
              </Button>
            }
          />
        ) : (
          <PosterGrid>
            {filteredTitles.map((item) => (
              <Poster key={item.id} title={item} variant="poster" />
            ))}
          </PosterGrid>
        )}
      </div>
    </PageContainer>
  );
}
