import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        accent: {
          DEFAULT: '#0F766E',
          hover: '#115E59',
          active: '#0A4F49',
          subtle: '#E6F4F2',
          contrast: '#FFFFFF',
        },
        canvas: '#FAFAF9',
        surface: {
          DEFAULT: '#FFFFFF',
          muted: '#F4F4F2',
        },
        border: {
          DEFAULT: '#E7E5E4',
          strong: '#D6D3D1',
        },
        ink: {
          DEFAULT: '#0A0A0A',
          secondary: '#52525B',
          muted: '#A1A1AA',
        },
        'dark-block': '#0B0B0C',
        success: {
          DEFAULT: '#15803D',
          subtle: '#DCFCE7',
        },
        warning: {
          DEFAULT: '#B45309',
          subtle: '#FEF3C7',
        },
        danger: {
          DEFAULT: '#B91C1C',
          subtle: '#FEE2E2',
        },
        info: {
          DEFAULT: '#1D4ED8',
          subtle: '#DBEAFE',
        },
      },
      fontFamily: {
        display: ['Familjen Grotesk', 'Helvetica Neue', 'Arial', 'system-ui', 'sans-serif'],
        body: ['Onest', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'monospace'],
      },
      fontSize: {
        display: ['clamp(2.75rem, 5vw, 4.5rem)', { lineHeight: '1.02', letterSpacing: '-0.02em', fontWeight: '700' }],
        headline: ['clamp(1.875rem, 3vw, 2.75rem)', { lineHeight: '1.1', letterSpacing: '-0.01em', fontWeight: '650' }],
        title: ['1.25rem', { lineHeight: '1.3', fontWeight: '600' }],
        body: ['1rem', { lineHeight: '1.6', fontWeight: '400' }],
        label: ['0.75rem', { lineHeight: '1.2', letterSpacing: '0.12em', fontWeight: '600' }],
        button: ['0.9375rem', { lineHeight: '1', letterSpacing: '0.01em', fontWeight: '600' }],
        data: ['0.875rem', { lineHeight: '1.4', fontWeight: '500' }],
      },
      borderRadius: {
        sm: '6px',
        md: '10px',
        lg: '16px',
        xl: '24px',
        pill: '999px',
      },
      spacing: {
        xs: '4px',
        sm: '8px',
        md: '16px',
        lg: '24px',
        xl: '32px',
        '2xl': '48px',
        '3xl': '80px',
        '4xl': '120px',
      },
      boxShadow: {
        xs: '0 1px 2px rgba(10,10,10,.04)',
        sm: '0 1px 3px rgba(10,10,10,.06), 0 1px 2px rgba(10,10,10,.04)',
        md: '0 4px 12px rgba(10,10,10,.06), 0 2px 4px rgba(10,10,10,.04)',
        lg: '0 12px 28px rgba(10,10,10,.10), 0 4px 8px rgba(10,10,10,.05)',
        xl: '0 24px 48px -12px rgba(10,10,10,.18)',
        focus: '0 0 0 3px rgba(15,118,110,.25)',
      },
    },
  },
  plugins: [],
};
export default config;