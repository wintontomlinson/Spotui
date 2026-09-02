'use client';

import { SpotifyApiError } from '@/lib/spotify';

/** Skeleton rows, standing in for LibrarySkeleton. */
export function ListSkeleton({ rows = 8 }: { rows?: number }) {
  return (
    <div className="space-y-3 px-4 py-4" aria-hidden="true">
      {Array.from({ length: rows }).map((_, index) => (
        <div key={index} className="flex items-center gap-3">
          <div className="h-12 w-12 shrink-0 animate-pulse rounded-[6px] bg-white/10" />
          <div className="min-w-0 flex-1 space-y-2">
            <div className="h-3 w-1/2 animate-pulse rounded bg-white/10" />
            <div className="h-3 w-1/3 animate-pulse rounded bg-white/[0.07]" />
          </div>
        </div>
      ))}
    </div>
  );
}

export function ErrorView({ error }: { error: unknown }) {
  const message =
    error instanceof SpotifyApiError
      ? error.message
      : error instanceof Error
        ? error.message
        : 'Something went wrong.';

  const isForbidden = error instanceof SpotifyApiError && error.status === 403;

  return (
    <div className="mx-4 mt-6 rounded-[8px] border border-app-error/30 bg-app-error/10 p-4">
      <p className="text-[13px] font-bold text-app-error">Could not load this</p>
      <p className="mt-1 break-words text-[13px] leading-relaxed text-white/70">{message}</p>
      {isForbidden ? (
        <p className="mt-2 text-[12px] leading-relaxed text-white/50">
          In Development Mode, each listener&apos;s Spotify account must be added under
          <em> Users and access</em> in your app on the developer dashboard.
        </p>
      ) : null}
    </div>
  );
}

export function EmptyView({ title, body }: { title: string; body?: string }) {
  return (
    <div className="px-4 py-10 text-center">
      <p className="text-[16px] font-bold text-white">{title}</p>
      {body ? <p className="mt-2 text-[13px] leading-relaxed text-text-secondary">{body}</p> : null}
    </div>
  );
}
