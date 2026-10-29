import type { Config } from 'tailwindcss';

/**
 * "Projection room" identity. Colours are defined once as CSS variables in globals.css;
 * Tailwind points at them so the palette can change in one place.
 */
const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
    './src/lib/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // Raw tokens, so the named palette (booth, paper, projector, gate, film-grey) reads the same in markup.
        booth: 'rgb(var(--booth) / <alpha-value>)',
        paper: 'rgb(var(--paper) / <alpha-value>)',
        projector: 'rgb(var(--projector) / <alpha-value>)',
        screen: {
          DEFAULT: 'rgb(var(--screen) / <alpha-value>)',
          raised: 'rgb(var(--screen-raised) / <alpha-value>)',
        },
        gate: {
          DEFAULT: 'rgb(var(--gate) / <alpha-value>)',
          subtle: 'rgb(var(--gate-subtle) / <alpha-value>)',
        },
        'film-grey': 'rgb(var(--film-grey) / <alpha-value>)',
        background: 'rgb(var(--booth) / <alpha-value>)',
        foreground: 'rgb(var(--paper) / <alpha-value>)',
        surface: {
          DEFAULT: 'rgb(var(--screen) / <alpha-value>)',
          elevated: 'rgb(var(--screen-raised) / <alpha-value>)',
          hover: 'rgb(var(--gate) / <alpha-value>)',
        },
        border: {
          DEFAULT: 'rgb(var(--gate) / <alpha-value>)',
          subtle: 'rgb(var(--gate-subtle) / <alpha-value>)',
        },
        // The projector amber is a function colour: play, active state, focus, progress.
        primary: {
          DEFAULT: 'rgb(var(--projector) / <alpha-value>)',
          foreground: 'rgb(var(--booth) / <alpha-value>)',
        },
        muted: {
          DEFAULT: 'rgb(var(--screen-raised) / <alpha-value>)',
          foreground: 'rgb(var(--film-grey) / <alpha-value>)',
        },
        subtle: 'rgb(var(--film-grey) / <alpha-value>)',
        success: '#6FBF73',
        warning: '#E0B452',
        danger: 'rgb(var(--danger) / <alpha-value>)',
        destructive: {
          DEFAULT: 'rgb(var(--danger) / <alpha-value>)',
          foreground: 'rgb(var(--paper) / <alpha-value>)',
        },
      },
      fontFamily: {
        // Titles and headings: Fraunces via next/font (see layout.tsx).
        heading: ['var(--font-serif)', 'Georgia', 'serif'],
        display: ['var(--font-serif)', 'Georgia', 'serif'],
        // Interface and body text: Instrument Sans via next/font.
        sans: ['var(--font-sans)', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        // Film-gate posters are nearly square; panels are softer.
        card: '6px',
        pill: '9999px',
      },
      spacing: {
        nav: 'var(--nav-h)',
      },
      maxWidth: {
        container: '1440px',
      },
    },
  },
  plugins: [],
};

export default config;
