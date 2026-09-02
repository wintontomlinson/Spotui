import type { Metadata, Viewport } from 'next';
import { Figtree } from 'next/font/google';

import { AppShell } from '@/components/AppShell';

import './globals.css';

/**
 * The Android app ships SpotifyMix / SpotifyMixTitle, extracted from the
 * official Spotify app. Those are licensed Spotify assets and are not
 * redistributable, so this port uses Figtree — a free geometric sans with a
 * similar tight, rounded feel.
 */
const uiFont = Figtree({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-ui',
});

export const metadata: Metadata = {
  title: 'Spotui Web',
  description:
    'A web build of Spotui — browse your Spotify library and play it in the browser via the Web Playback SDK.',
};

export const viewport: Viewport = {
  themeColor: '#0B0B0F',
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={uiFont.variable}>
      <head>
        {/* The SDK streams from these hosts; warming them shaves first-play latency. */}
        <link rel="preconnect" href="https://sdk.scdn.co" />
        <link rel="preconnect" href="https://i.scdn.co" />
        <link rel="dns-prefetch" href="https://api.spotify.com" />
      </head>
      <body className="bg-app-bg font-sans antialiased">
        <AppShell>{children}</AppShell>
      </body>
    </html>
  );
}
