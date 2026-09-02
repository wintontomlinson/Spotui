'use client';

import { create } from 'zustand';

import { getMe } from '@/lib/spotify';
import { hasSession, signOut as clearSession } from '@/lib/token';
import type { UserProfile } from '@/lib/types';

export type AuthStatus = 'unknown' | 'signed_out' | 'signed_in';

interface AuthState {
  status: AuthStatus;
  profile: UserProfile | null;
  error: string | null;
  bootstrap: () => Promise<void>;
  signOut: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  status: 'unknown',
  profile: null,
  error: null,

  bootstrap: async () => {
    if (!hasSession()) {
      set({ status: 'signed_out', profile: null });
      return;
    }
    try {
      const profile = await getMe();
      set({ status: 'signed_in', profile, error: null });
    } catch (error) {
      clearSession();
      set({
        status: 'signed_out',
        profile: null,
        error: error instanceof Error ? error.message : 'Failed to load your Spotify profile.',
      });
    }
  },

  signOut: () => {
    clearSession();
    set({ status: 'signed_out', profile: null, error: null });
  },
}));
