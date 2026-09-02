import type { Config } from 'tailwindcss';

/**
 * Colour tokens mirror the Android app's ui/theme/Color.kt plus the
 * recurring hard-coded literals used across its screens.
 */
const config: Config = {
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // ui/theme/Color.kt
        'app-bg': '#0B0B0F',
        'grid-bg': '#2A2A2A',
        'app-palette': '#618DFF',
        // literals used throughout the Compose screens
        'spotify-green': '#1ED760',
        'text-secondary': '#B3B3B3',
        surface: '#121212',
        'surface-dialog': '#1E1E1E',
        'app-error': '#E22134',
      },
      fontFamily: {
        // SpotifyMix / SpotifyMixTitle are licensed Spotify assets and are not
        // shipped here; Figtree is the closest freely-licensable stand-in.
        sans: ['var(--font-ui)', 'system-ui', 'sans-serif'],
      },
      letterSpacing: {
        // global letterSpacing = -0.2.sp in SpotuiTheme
        tightish: '-0.0125em',
      },
      transitionDuration: {
        150: '150ms',
      },
    },
  },
  plugins: [],
};

export default config;
