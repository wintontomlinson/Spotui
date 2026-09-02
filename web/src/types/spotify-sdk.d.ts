/**
 * Minimal ambient types for the Spotify Web Playback SDK
 * (https://sdk.scdn.co/spotify-player.js), covering only what this app uses.
 */

declare namespace Spotify {
  interface Image {
    url: string;
    height?: number | null;
    width?: number | null;
  }

  interface Entity {
    uri: string;
    name: string;
  }

  interface WebPlaybackTrack {
    id: string | null;
    uri: string;
    name: string;
    duration_ms: number;
    is_playable: boolean;
    album: { uri: string; name: string; images: Image[] };
    artists: Entity[];
  }

  interface WebPlaybackState {
    context: { uri: string | null; metadata: unknown };
    paused: boolean;
    position: number;
    duration: number;
    loading: boolean;
    repeat_mode: 0 | 1 | 2;
    shuffle: boolean;
    track_window: {
      current_track: WebPlaybackTrack;
      previous_tracks: WebPlaybackTrack[];
      next_tracks: WebPlaybackTrack[];
    };
  }

  interface Error {
    message: string;
  }

  interface PlayerInit {
    name: string;
    getOAuthToken: (callback: (token: string) => void) => void;
    volume?: number;
    enableMediaSession?: boolean;
  }

  class Player {
    constructor(options: PlayerInit);

    connect(): Promise<boolean>;
    disconnect(): void;

    addListener(event: 'ready' | 'not_ready', callback: (data: { device_id: string }) => void): boolean;
    addListener(event: 'player_state_changed', callback: (state: WebPlaybackState | null) => void): boolean;
    addListener(
      event: 'initialization_error' | 'authentication_error' | 'account_error' | 'playback_error',
      callback: (error: Error) => void,
    ): boolean;

    removeListener(event: string, callback?: (...args: never[]) => void): boolean;

    getCurrentState(): Promise<WebPlaybackState | null>;
    setName(name: string): Promise<void>;
    getVolume(): Promise<number>;
    setVolume(volume: number): Promise<void>;
    pause(): Promise<void>;
    resume(): Promise<void>;
    togglePlay(): Promise<void>;
    seek(positionMs: number): Promise<void>;
    previousTrack(): Promise<void>;
    nextTrack(): Promise<void>;
    activateElement(): Promise<void>;
  }
}

interface Window {
  onSpotifyWebPlaybackSDKReady: () => void;
  Spotify: typeof Spotify;
}
