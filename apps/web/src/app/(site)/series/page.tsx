import CatalogPage from '@/components/CatalogPage';

export const metadata = {
  title: 'Series — VyroFlix',
  description: 'Binge compelling multi-episode original series and stories on VyroFlix.',
};

export default function SeriesPage() {
  return (
    <CatalogPage
      heading="Original Series"
      description="Compelling episodic sagas, immersive multi-season mysteries, and edge-of-your-seat drama."
      type="SERIES"
    />
  );
}
