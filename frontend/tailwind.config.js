/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        // Palette SkillMap
        brand: {
          DEFAULT: "#2563eb", // blue-600
          dark: "#1e40af",    // blue-800
        },
      },
    },
  },
  plugins: [],
};
