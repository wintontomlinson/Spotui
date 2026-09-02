'use client';

import { Suspense, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';

import { completeLogin, consumeReturnTo } from '@/lib/auth';
import { primeTokens } from '@/lib/token';
import { useAuthStore } from '@/store/useAuthStore';

function CallbackInner() {
  const params = useSearchParams();
  const router = useRouter();
  const bootstrap = useAuthStore((s) => s.bootstrap);
  const [error, setError] = useState<string | null>(null);
  // React 18 Strict Mode double-invokes effects; the code is single-use.
  const exchanged = useRef(false);

  useEffect(() => {
    if (exchanged.current) return;
    exchanged.current = true;

    const denied = params.get('error');
    if (denied) {
      setError(
        denied === 'access_denied'
          ? 'You declined the Spotify permission request.'
          : `Spotify returned an error: ${denied}`,
      );
      return;
    }

    const code = params.get('code');
    if (!code) {
      setError('No authorization code was returned. Start the login again.');
      return;
    }

    void (async () => {
      try {
        const tokens = await completeLogin(code, params.get('state'));
        primeTokens(tokens);
        await bootstrap();
        router.replace(consumeReturnTo());
      } catch (cause) {
        setError(cause instanceof Error ? cause.message : 'Sign in failed.');
      }
    })();
  }, [params, router, bootstrap]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-app-bg px-6">
      <div className="w-full max-w-md text-center">
        {error ? (
          <>
            <p className="text-[15px] font-bold text-app-error">Sign in failed</p>
            <p className="mt-2 break-words text-[13px] leading-relaxed text-white/70">{error}</p>
            <Link
              href="/login"
              className="mt-6 inline-block rounded-full bg-white px-5 py-2 text-[14px] font-bold text-black"
            >
              Try again
            </Link>
          </>
        ) : (
          <p className="text-[14px] text-text-secondary">Finishing sign in…</p>
        )}
      </div>
    </main>
  );
}

export default function CallbackPage() {
  return (
    <Suspense
      fallback={
        <main className="flex min-h-screen items-center justify-center bg-app-bg">
          <p className="text-[14px] text-text-secondary">Finishing sign in…</p>
        </main>
      }
    >
      <CallbackInner />
    </Suspense>
  );
}
