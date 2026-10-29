import type { Metadata } from 'next';
import { Fraunces, Instrument_Sans } from 'next/font/google';
import './globals.css';
import { Providers } from '@/components/Providers';

// Titles and headings. The optical-size axis keeps small serif headings legible.
const serif = Fraunces({
  subsets: ['latin'],
  variable: '--font-serif',
  display: 'swap',
  axes: ['opsz'],
});

// Interface and body text.
const sans = Instrument_Sans({
  subsets: ['latin'],
  variable: '--font-sans',
  display: 'swap',
});

export const metadata: Metadata = {
  title: 'VyroFlix — Cinematic Streaming Experience',
  description: 'A low-cost, cloud-native video-on-demand platform with multi-bitrate HLS streaming.',
};

// Document shell only. Each route group supplies its own chrome (see (site), (auth), (watch), (admin)).
export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className={`${serif.variable} ${sans.variable}`}>
      <body className="min-h-screen antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
