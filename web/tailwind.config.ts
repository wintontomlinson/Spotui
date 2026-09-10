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
        // Royal Edition — deep royal purple base, gold accents.
        'app-bg': '#130824',
        'grid-bg': '#2B1D4A',
        'app-palette': '#B794F6',
        'royal-gold': '#D4AF37',
        royal: {
          200: '#DDD6FE',
          300: '#C4B5FD',
          400: '#A78BFA',
          500: '#8B5CF6',
          600: '#7C3AED',
          700: '#6D28D9',
          800: '#5B21B6',
          900: '#4C1D95',
        },
        gold: {
          200: '#F5E09A',
          300: '#EFC960',
          400: '#E9B537',
          500: '#D4AF37',
          600: '#B8892B',
        },
        'text-secondary': '#C4B5E0',
        surface: '#1A0F30',
        'surface-dialog': '#241540',
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
