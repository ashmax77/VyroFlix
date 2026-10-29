import { getBrowseCatalog } from '@/lib/catalog-server';
import BrowseView from './BrowseView';

// The public catalog is rendered on the server and cached for REVALIDATE_SECONDS, so visitors do not wait
// on the backend. Personal sections are added in the browser.
// Keep in step with REVALIDATE_SECONDS in lib/catalog-server.ts (Next needs a literal here).
export const revalidate = 300;

export default async function BrowsePage() {
  const initial = await getBrowseCatalog();
  return <BrowseView initial={initial} />;
}
