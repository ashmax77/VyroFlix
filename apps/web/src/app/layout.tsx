import type { Metadata } from 'next';
import { Instrument_Sans, Instrument_Serif } from 'next/font/google';
import './globals.css';
import { Providers } from '@/components/Providers';

// Titles and headings. A single-weight display serif that reads like a programme title card.
const serif = Instrument_Serif({
  subsets: ['latin'],
  weight: '400',
  style: ['normal', 'italic'],
  variable: '--font-serif',
  display: 'swap',
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
