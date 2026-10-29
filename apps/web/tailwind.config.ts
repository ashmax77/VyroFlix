import type { Config } from 'tailwindcss';

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
        background: 'rgb(var(--background) / <alpha-value>)',
        foreground: 'rgb(var(--foreground) / <alpha-value>)',
        surface: {
          DEFAULT: 'rgb(var(--surface) / <alpha-value>)',
          elevated: 'rgb(var(--surface-elevated) / <alpha-value>)',
          hover: 'rgb(var(--surface-elevated) / <alpha-value>)',
        },
        border: {
          DEFAULT: 'rgb(var(--border) / <alpha-value>)',
          subtle: 'rgb(var(--border-subtle) / <alpha-value>)',
        },
        'brand-violet': 'rgb(var(--brand-violet) / <alpha-value>)',
        'brand-magenta': 'rgb(var(--brand-magenta) / <alpha-value>)',
        cyan: 'rgb(var(--cyan) / <alpha-value>)',
        primary: {
          DEFAULT: 'rgb(var(--brand-violet) / <alpha-value>)',
          foreground: '#ffffff',
        },
        muted: {
          DEFAULT: 'rgb(var(--surface-elevated) / <alpha-value>)',
          foreground: 'rgb(var(--muted-foreground) / <alpha-value>)',
        },
        subtle: 'rgb(var(--subtle) / <alpha-value>)',
        success: '#10B981',
        warning: '#F59E0B',
        danger: '#EF4444',
        destructive: {
          DEFAULT: '#EF4444',
          foreground: '#ffffff',
        },
        booth: 'rgb(var(--background) / <alpha-value>)',
        screen: {
          DEFAULT: 'rgb(var(--surface) / <alpha-value>)',
          raised: 'rgb(var(--surface-elevated) / <alpha-value>)',
        },
        gate: {
          DEFAULT: 'rgb(var(--border) / <alpha-value>)',
          subtle: 'rgb(var(--border-subtle) / <alpha-value>)',
        },
        'film-grey': 'rgb(var(--subtle) / <alpha-value>)',
        paper: 'rgb(var(--foreground) / <alpha-value>)',
        projector: 'rgb(var(--cyan) / <alpha-value>)',
      },
      fontFamily: {
        display: ['var(--font-display)', 'var(--font-sans)', 'sans-serif'],
        heading: ['var(--font-display)', 'var(--font-serif)', 'sans-serif'],
        sans: ['var(--font-sans)', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        glow: '0 10px 30px -10px rgba(139, 92, 246, 0.5)',
        'glow-lg': '0 18px 50px -12px rgba(236, 72, 153, 0.5)',
        card: '0 24px 48px -16px rgba(0, 0, 0, 0.75)',
      },
      borderRadius: {
        card: '0.75rem',
        pill: '9999px',
      },
      spacing: {
        nav: 'var(--nav-h)',
      },
      maxWidth: {
        container: '1800px',
      },
    },
  },
  plugins: [],
};

export default config;
