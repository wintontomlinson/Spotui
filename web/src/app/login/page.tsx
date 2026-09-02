'use client';

import { useState } from 'react';

import { beginLogin } from '@/lib/auth';

export default function LoginPage() {
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const clientId = process.env.NEXT_PUBLIC_SPOTIFY_CLIENT_ID;

  const handleLogin = async () => {
    setBusy(true);
    setError(null);
    try {
      await beginLogin('/');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not start the Spotify login.');
      setBusy(false);
    }
  };

  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-app-bg px-6 py-12">
      <div className="w-full max-w-md">
        <h1 className="text-[32px] font-extrabold leading-tight text-white">Spotui Web</h1>
        <p className="mt-2 text-[14px] leading-relaxed text-text-secondary">
          Sign in with Spotify to browse your library and play it in this browser.
        </p>

        {!clientId ? (
          <div className="mt-6 rounded-[8px] border border-app-error/40 bg-app-error/10 p-4">
            <p className="text-[13px] font-bold text-app-error">Not configured</p>
            <p className="mt-1 text-[13px] leading-relaxed text-white/70">
              <code className="text-white">NEXT_PUBLIC_SPOTIFY_CLIENT_ID</code> is missing. Copy{' '}
              <code className="text-white">web/.env.example</code> to{' '}
              <code className="text-white">web/.env.local</code> and add your Client ID from the
              Spotify developer dashboard, then restart the dev server.
            </p>
          </div>
        ) : (
          <button
            type="button"
            onClick={() => void handleLogin()}
            disabled={busy}
            className="mt-8 w-full rounded-full bg-spotify-green px-6 py-3 text-[15px] font-bold text-black transition-transform hover:scale-[1.02] disabled:opacity-60"
          >
            {busy ? 'Redirecting…' : 'Continue with Spotify'}
          </button>
        )}

        {error ? (
          <p className="mt-4 rounded-[6px] bg-app-error/15 px-3 py-2 text-[13px] text-app-error">
            {error}
          </p>
        ) : null}

        <div className="mt-10 space-y-3 border-t border-white/10 pt-6 text-[12px] leading-relaxed text-white/50">
          <p>
            <strong className="text-white/80">Playback needs Spotify Premium.</strong> The Web
            Playback SDK will not stream audio on a free account — browsing and search still work.
          </p>
          <p>
            <strong className="text-white/80">Development Mode apps allow 5 users.</strong> Every
            listener&apos;s Spotify account has to be added to your app&apos;s user list in the
            developer dashboard, and the app owner needs an active Premium subscription.
          </p>
        </div>
      </div>
    </main>
  );
}
