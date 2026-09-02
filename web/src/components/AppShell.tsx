'use client';

import { useEffect } from 'react';
import { usePathname, useRouter } from 'next/navigation';

import { useArtworkPalette } from '@/hooks/usePalette';
import { useWebPlayback } from '@/hooks/useWebPlayback';
import { useAuthStore } from '@/store/useAuthStore';

import { BottomNav } from './BottomNav';
import { PlayerOverlay } from './PlayerOverlay';

/** Routes that must render without a Spotify session. */
const PUBLIC_ROUTES = ['/login', '/callback'];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const status = useAuthStore((s) => s.status);
  const bootstrap = useAuthStore((s) => s.bootstrap);

  const isPublic = PUBLIC_ROUTES.some((route) => pathname.startsWith(route));

  useEffect(() => {
    void bootstrap();
  }, [bootstrap]);

  useEffect(() => {
    if (status === 'signed_out' && !isPublic) router.replace('/login');
  }, [status, isPublic, router]);

  // The SDK is only booted once there is a session to hand it a token.
  useWebPlayback(status === 'signed_in');
  useArtworkPalette();

  if (isPublic) return <>{children}</>;

  if (status !== 'signed_in') {
    return (
      <main className="flex min-h-screen items-center justify-center bg-app-bg px-6">
        <p className="text-[14px] text-text-secondary">
          {status === 'unknown' ? 'Connecting to Spotify…' : 'Redirecting to sign in…'}
        </p>
      </main>
    );
  }

  return (
    <>
      {/* Bottom padding clears the mini player + nav, which are fixed. */}
      <main className="min-h-screen pb-40">{children}</main>
      <BottomNav />
      <PlayerOverlay />
    </>
  );
}
