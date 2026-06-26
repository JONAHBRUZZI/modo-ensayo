/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}'
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#6C63FF',
          50: '#f0efff',
          100: '#e0deff',
          200: '#c2beff',
          300: '#a39eff',
          400: '#857eff',
          500: '#6C63FF',
          600: '#554dcc',
          700: '#3e3899',
          800: '#282366',
          900: '#110f33'
        },
        surface: {
          base: 'var(--bg-base)',
          card: 'var(--bg-surface)',
          elevated: 'var(--bg-elevated)',
          overlay: 'var(--bg-overlay)',
          footer: 'var(--bg-footer)',
          input: 'var(--bg-input)',
        },
        border: {
          subtle: 'var(--border-subtle)',
          DEFAULT: 'var(--border-default)',
        },
        text: {
          primary: 'var(--text-primary)',
          secondary: 'var(--text-secondary)',
        }
      }
    }
  },
  plugins: []
}
