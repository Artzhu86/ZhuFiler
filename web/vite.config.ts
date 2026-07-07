import { defineConfig } from 'vite'
import { resolve } from 'path'

export default defineConfig({
  base: '/ZhuFiler/',
  build: {
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'index.html'),
        download: resolve(__dirname, 'download.html'),
        features: resolve(__dirname, 'features.html'),
        guide: resolve(__dirname, 'guide.html'),
        changelog: resolve(__dirname, 'changelog.html'),
        about: resolve(__dirname, 'about.html'),
        development: resolve(__dirname, 'development.html'),
      },
    },
  },
})
