'use client';

import { create } from 'zustand';

import { DEFAULT_PALETTE, type ArtworkPalette } from '@/lib/palette';
import type { RepeatMode } from '@/lib/types';

export interface PlayerTrack {
  id: string | null;
  uri: string;
  name: string;
  artistNames: string;
  albumName: string;
  albumUri: string;
  imageUrl: string | null;
  durationMs: number;
}

/**
 * Reason playback is unavailable. `premium_required` is surfaced from the SDK's
 * `account_error`, which is the only reliable Premium signal left — the Feb 2026
 * migration removed the `product` field from GET /me for Development Mode apps.
 */
export type PlaybackBlocker = 'premium_required' | 'auth_failed' | 'init_failed' | null;

interface PlayerState {
  deviceId: string | null;
  isReady: boolean;
  /** True once this browser tab is the active Spotify Connect device. */
  isActive: boolean;
  isPaused: boolean;
  isBuffering: boolean;
  track: PlayerTrack | null;
  positionMs: number;
  durationMs: number;
  shuffle: boolean;
  repeatMode: RepeatMode;
  nextTracks: PlayerTrack[];
  previousTracks: PlayerTrack[];
  palette: ArtworkPalette;
  blocker: PlaybackBlocker;
  errorMessage: string | null;
  /** Full-screen player overlay — the Android app's `player` dialog route. */
  playerOpen: boolean;
  lyricsOpen: boolean;

  setDevice: (deviceId: string | null, isReady: boolean) => void;
  applySdkState: (state: Spotify.WebPlaybackState | null) => void;
  setPosition: (positionMs: number) => void;
  setPalette: (palette: ArtworkPalette) => void;
  setBlocker: (blocker: PlaybackBlocker, message?: string | null) => void;
  setPlayerOpen: (open: boolean) => void;
  setLyricsOpen: (open: boolean) => void;
  reset: () => void;
}

function mapTrack(track: Spotify.WebPlaybackTrack | null | undefined): PlayerTrack | null {
  if (!track) return null;
  const images = track.album?.images ?? [];
  const largest = [...images].sort((a, b) => (b.width ?? 0) - (a.width ?? 0))[0];
  return {
    id: track.id,
    uri: track.uri,
    name: track.name,
    artistNames: (track.artists ?? []).map((a) => a.name).join(', '),
    albumName: track.album?.name ?? '',
    albumUri: track.album?.uri ?? '',
    imageUrl: largest?.url ?? null,
    durationMs: track.duration_ms,
  };
}

function mapRepeat(mode: 0 | 1 | 2): RepeatMode {
  if (mode === 1) return 'context';
  if (mode === 2) return 'track';
  return 'off';
}

export const usePlayerStore = create<PlayerState>((set) => ({
  deviceId: null,
  isReady: false,
  isActive: false,
  isPaused: true,
  isBuffering: false,
  track: null,
  positionMs: 0,
  durationMs: 0,
  shuffle: false,
  repeatMode: 'off',
  nextTracks: [],
  previousTracks: [],
  palette: DEFAULT_PALETTE,
  blocker: null,
  errorMessage: null,
  playerOpen: false,
  lyricsOpen: false,

  setDevice: (deviceId, isReady) => set({ deviceId, isReady }),

  applySdkState: (state) => {
    if (!state) {
      // Null state means another device took over playback.
      set({ isActive: false, isBuffering: false });
      return;
    }
    const track = mapTrack(state.track_window?.current_track);
    set({
      isActive: true,
      isPaused: state.paused,
      isBuffering: state.loading,
      track,
      positionMs: state.position,
      durationMs: state.duration || (track?.durationMs ?? 0),
      shuffle: state.shuffle,
      repeatMode: mapRepeat(state.repeat_mode),
      nextTracks: (state.track_window?.next_tracks ?? [])
        .map(mapTrack)
        .filter((t): t is PlayerTrack => t !== null),
      previousTracks: (state.track_window?.previous_tracks ?? [])
        .map(mapTrack)
        .filter((t): t is PlayerTrack => t !== null),
    });
  },

  setPosition: (positionMs) => set({ positionMs }),
  setPalette: (palette) => set({ palette }),
  setBlocker: (blocker, message = null) => set({ blocker, errorMessage: message }),
  setPlayerOpen: (playerOpen) => set({ playerOpen }),
  setLyricsOpen: (lyricsOpen) => set({ lyricsOpen }),

  reset: () =>
    set({
      deviceId: null,
      isReady: false,
      isActive: false,
      isPaused: true,
      isBuffering: false,
      track: null,
      positionMs: 0,
      durationMs: 0,
      nextTracks: [],
      previousTracks: [],
      palette: DEFAULT_PALETTE,
      playerOpen: false,
      lyricsOpen: false,
    }),
}));
