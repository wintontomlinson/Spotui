/**
 * Same-origin proxy for Spotify artwork.
 *
 * Only exists so the client can draw cover art onto a canvas and call
 * getImageData() for palette extraction — Spotify's image CDN does not send
 * Access-Control-Allow-Origin, which would taint the canvas.
 *
 * The host allowlist keeps this from becoming an open proxy.
 */

import { NextResponse } from 'next/server';

const ALLOWED_HOSTS = new Set([
  'i.scdn.co',
  'mosaic.scdn.co',
  'misc.scdn.co',
  'image-cdn-ak.spotifycdn.com',
  'image-cdn-fa.spotifycdn.com',
  'lineup-images.scdn.co',
  'thisis-images.scdn.co',
  'seeded-session-images.scdn.co',
  'daily-mix.scdn.co',
]);

export async function GET(request: Request): Promise<NextResponse> {
  const target = new URL(request.url).searchParams.get('url');
  if (!target) {
    return NextResponse.json({ error: 'Missing url parameter.' }, { status: 400 });
  }

  let parsed: URL;
  try {
    parsed = new URL(target);
  } catch {
    return NextResponse.json({ error: 'Malformed url parameter.' }, { status: 400 });
  }

  if (parsed.protocol !== 'https:' || !ALLOWED_HOSTS.has(parsed.hostname)) {
    return NextResponse.json({ error: 'Host not allowed.' }, { status: 403 });
  }

  const upstream = await fetch(parsed.toString(), {
    headers: { Accept: 'image/*' },
    cache: 'force-cache',
  });

  if (!upstream.ok || !upstream.body) {
    return NextResponse.json({ error: 'Upstream fetch failed.' }, { status: 502 });
  }

  const contentType = upstream.headers.get('content-type') ?? 'image/jpeg';
  if (!contentType.startsWith('image/')) {
    return NextResponse.json({ error: 'Upstream is not an image.' }, { status: 502 });
  }

  return new NextResponse(upstream.body, {
    status: 200,
    headers: {
      'Content-Type': contentType,
      'Cache-Control': 'public, max-age=604800, immutable',
      'Access-Control-Allow-Origin': '*',
    },
  });
}
