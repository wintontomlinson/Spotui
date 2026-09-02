'use client';

/**
 * Playback actions. Transport (play/pause/seek/skip) goes through the Web
 * Playback SDK instance because it is instant and local; anything that changes
 * *what* is loaded (a new track list, shuffle, repeat) goes through the Player
 * REST API targeted at this tab's device_id.
 */

import { usePlayerStore } from '@/store/usePlayerStore';

import {
  addToQueue,
  playContext,
  playUris,
  setRepeat,
  setShuffle,
  transferPlayback,
} from './spotify';
import type { RepeatMode } from './types';

let playerInstance: Spotify.Player | null = null;

export function registerPlayer(player: Spotify.Player | null): void {
  playerInstance = player;
}

export function getPlayer(): Spotify.Player | null {
  return playerInstance;
}

function requireDevice(): string {
  const { deviceId } = usePlayerStore.getState();
  if (!deviceId) {
    throw new Error('The web player is not ready yet. Wait a moment and try again.');
  }
  return deviceId;
}

/**
 * Chrome and Safari block audio until a user gesture. The SDK exposes
 * activateElement() for exactly this; call it inside the click handler before
 * the first play so the browser attributes the audio to the gesture.
 */
async function activateForGesture(): Promise<void> {
  try {
    await playerInstance?.activateElement();
  } catch {
    // Non-fatal — playback may still start if the tab is already activated.
  }
}

/** Plays an explicit list of track URIs starting at `index`. */
export async function playTrackList(uris: string[], index = 0): Promise<void> {
  if (uris.length === 0) return;
  const deviceId = requireDevice();
  await activateForGesture();

  // The Player API caps the URI list; a window around the chosen track keeps
  // the request small while preserving "play the rest of this list".
  const MAX_URIS = 200;
  let window = uris;
  let offset = index;
  if (uris.length > MAX_URIS) {
    const start = Math.max(0, Math.min(index, uris.length - MAX_URIS));
    window = uris.slice(start, start + MAX_URIS);
    offset = index - start;
  }

  await playUris(deviceId, window, offset);
}

/** Plays a playlist/album/artist context so Spotify manages the queue. */
export async function playContextUri(contextUri: string, index = 0): Promise<void> {
  const deviceId = requireDevice();
  await activateForGesture();
  await playContext(deviceId, contextUri, index);
}

export async function togglePlay(): Promise<void> {
  const state = usePlayerStore.getState();
  if (!playerInstance) return;

  // Nothing loaded in this tab yet — become the active device first.
  if (!state.isActive && state.deviceId) {
    await activateForGesture();
    await transferPlayback(state.deviceId, true);
    return;
  }

  await activateForGesture();
  await playerInstance.togglePlay();
}

export async function nextTrack(): Promise<void> {
  await playerInstance?.nextTrack();
}

export async function previousTrack(): Promise<void> {
  await playerInstance?.previousTrack();
}

export async function seekTo(positionMs: number): Promise<void> {
  usePlayerStore.getState().setPosition(positionMs);
  await playerInstance?.seek(positionMs);
}

export async function toggleShuffle(): Promise<void> {
  const { shuffle } = usePlayerStore.getState();
  await setShuffle(requireDevice(), !shuffle);
}

/** off → context → track → off, matching the Android repeat button. */
export async function cycleRepeat(): Promise<void> {
  const { repeatMode } = usePlayerStore.getState();
  const next: RepeatMode =
    repeatMode === 'off' ? 'context' : repeatMode === 'context' ? 'track' : 'off';
  await setRepeat(requireDevice(), next);
}

export async function queueTrack(uri: string): Promise<void> {
  await addToQueue(requireDevice(), uri);
}

/** Makes this browser tab the active Spotify Connect device. */
export async function becomeActiveDevice(play = false): Promise<void> {
  await activateForGesture();
  await transferPlayback(requireDevice(), play);
}
