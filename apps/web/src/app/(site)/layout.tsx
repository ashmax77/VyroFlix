import Navbar from '@/components/Navbar';
import Footer from '@/components/Footer';
import { BottomTabs } from '@/components/layout/BottomTabs';

export default function SiteLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <Navbar />
      <main className="flex-1 pb-16 md:pb-0">{children}</main>
      <BottomTabs />
      <Footer />
    </div>
  );
}
