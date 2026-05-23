/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}'
  ],
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
        }
      }
    }
  },
  plugins: []
}
