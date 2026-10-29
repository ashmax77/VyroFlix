import CatalogPage from '@/components/CatalogPage';

export const metadata = {
  title: 'Movies — VyroFlix',
  description: 'Explore full-length feature films and original cinematic premieres on VyroFlix.',
};

export default function MoviesPage() {
  return (
    <CatalogPage
      heading="Feature Films"
      description="Stream our curated collection of original sci-fi, intense thrillers, and award-winning dramas."
      type="MOVIE"
    />
  );
}
