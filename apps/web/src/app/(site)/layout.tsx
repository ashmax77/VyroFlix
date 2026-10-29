import Navbar from '@/components/Navbar';
import Footer from '@/components/Footer';

// Public storefront: browse, search, titles, my list and the landing page.
export default function SiteLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <Navbar />
      <main className="flex-1 pt-nav">{children}</main>
      <Footer />
    </>
  );
}
