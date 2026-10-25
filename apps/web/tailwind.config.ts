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
        background: '#0A0A0F',
        foreground: '#F5F5F7',
        surface: {
          DEFAULT: '#14141C',
          elevated: '#1C1C27',
          hover: '#242433',
        },
        border: {
          DEFAULT: '#2A2A38',
          subtle: '#1F1F2C',
        },
        primary: {
          DEFAULT: '#7C3AED',
          foreground: '#FFFFFF',
          violet: '#7C3AED',
          magenta: '#DB2777',
        },
        cyan: {
          DEFAULT: '#22D3EE',
        },
        accent: {
          DEFAULT: '#1C1C27',
          cyan: '#22D3EE',
        },
        muted: {
          DEFAULT: '#1C1C27',
          foreground: '#A1A1AA',
        },
        subtle: '#71717A',
        success: '#10B981',
        warning: '#F59E0B',
        danger: '#EF4444',
        destructive: {
          DEFAULT: '#EF4444',
          foreground: '#FFFFFF',
        },
        text: {
          primary: '#F5F5F7',
          secondary: '#A1A1AA',
          muted: '#71717A',
        },
      },
      fontFamily: {
        heading: ['Outfit', 'sans-serif'],
        display: ['Outfit', 'sans-serif'],
        sans: ['Inter', 'sans-serif'],
      },
      borderRadius: {
        card: '12px',
        pill: '9999px',
      },
      boxShadow: {
        glow: '0 10px 30px -10px rgba(124, 58, 237, 0.5)',
        'glow-lg': '0 18px 50px -12px rgba(219, 39, 119, 0.5)',
        card: '0 24px 48px -16px rgba(0, 0, 0, 0.85)',
      },
    },
  },
  plugins: [],
};

export default config;
