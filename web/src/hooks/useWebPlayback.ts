'use client';

import { useEffect, useRef } from 'react';

import { registerPlayer } from '@/lib/playback';
import { ensureAccessToken } from '@/lib/token';
import { usePlayerStore } from '@/store/usePlayerStore';

const SDK_SRC = 'https://sdk.scdn.co/spotify-player.js';
const SDK_SCRIPT_ID = 'spotify-web-playback-sdk';

/** Position poll cadence — the Android player uses 300 ms / 2000 ms. */
const TICK_PLAYING_MS = 300;
const TICK_PAUSED_MS = 2000;

function loadSdk(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (typeof window === 'undefined') {
      reject(new Error('No window.'));
      return;
    }
    if (window.Spotify) {
      resolve();
      return;
    }

    const existing = document.getElementById(SDK_SCRIPT_ID);
    const previousCallback = window.onSpotifyWebPlaybackSDKReady;
    window.onSpotifyWebPlaybackSDKReady = () => {
      previousCallback?.();
      resolve();
    };

    if (existing) return;

    const script = document.createElement('script');
    script.id = SDK_SCRIPT_ID;
    script.src = SDK_SRC;
    script.async = true;
    script.onerror = () => reject(new Error('Failed to load the Spotify Web Playback SDK.'));
    document.body.appendChild(script);
  });
}

/**
 * Boots the Web Playback SDK and mirrors its state into usePlayerStore.
 *
 * Requires Spotify Premium: the SDK reports `account_error` for free accounts
 * and there is no way to stream without it. Mount once, high in the tree.
 */
export function useWebPlayback(enabled: boolean): void {
  const playerRef = useRef<Spotify.Player | null>(null);
  const setDevice = usePlayerStore((s) => s.setDevice);
  const applySdkState = usePlayerStore((s) => s.applySdkState);
  const setBlocker = usePlayerStore((s) => s.setBlocker);
  const reset = usePlayerStore((s) => s.reset);

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;

    void (async () => {
      try {
        await loadSdk();
        if (cancelled) return;

        const player = new window.Spotify.Player({
          name: 'Spotui Web',
          volume: 0.8,
          enableMediaSession: true,
          getOAuthToken: (callback) => {
            void ensureAccessToken()
              .then(callback)
              .catch(() => setBlocker('auth_failed', 'Could not refresh your Spotify token.'));
          },
        });

        player.addListener('ready', ({ device_id }) => {
          setDevice(device_id, true);
          setBlocker(null);
        });

        player.addListener('not_ready', ({ device_id }) => {
          setDevice(device_id, false);
        });

        player.addListener('player_state_changed', (state) => {
          applySdkState(state);
        });

        player.addListener('account_error', () => {
          setBlocker(
            'premium_required',
            'The Spotify Web Playback SDK requires a Spotify Premium subscription. Browsing works, but audio will not play on this account.',
          );
        });

        player.addListener('authentication_error', ({ message }) => {
          setBlocker('auth_failed', message);
        });

        player.addListener('initialization_error', ({ message }) => {
          setBlocker('init_failed', message);
        });

        player.addListener('playback_error', ({ message }) => {
          // Transient (e.g. a track unavailable in this market) — surface but do not latch.
          // eslint-disable-next-line no-console
          console.warn('Spotify playback error:', message);
        });

        const connected = await player.connect();
        if (!connected && !cancelled) {
          setBlocker('init_failed', 'The Spotify player could not connect.');
        }

        playerRef.current = player;
        registerPlayer(player);
      } catch (error) {
        if (!cancelled) {
          setBlocker(
            'init_failed',
            error instanceof Error ? error.message : 'Failed to start the Spotify player.',
          );
        }
      }
    })();

    return () => {
      cancelled = true;
      registerPlayer(null);
      playerRef.current?.disconnect();
      playerRef.current = null;
      reset();
    };
  }, [enabled, setDevice, applySdkState, setBlocker, reset]);

  // Position ticker — the SDK only pushes state on change, so interpolate.
  useEffect(() => {
    if (!enabled) return;
    let timer: ReturnType<typeof setTimeout> | null = null;

    const tick = () => {
      const { isPaused, isActive } = usePlayerStore.getState();
      const player = playerRef.current;
      if (player && isActive) {
        void player.getCurrentState().then((state) => {
          if (state) usePlayerStore.getState().setPosition(state.position);
        });
      }
      timer = setTimeout(tick, isPaused ? TICK_PAUSED_MS : TICK_PLAYING_MS);
    };

    timer = setTimeout(tick, TICK_PLAYING_MS);
    return () => {
      if (timer) clearTimeout(timer);
    };
  }, [enabled]);
}
