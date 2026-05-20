// https://nuxt.com/docs/api/configuration/nuxt-config
import tailwindcss from '@tailwindcss/vite'

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',

  devtools: { enabled: true },

  devServer: {
    host: '0.0.0.0',
    port: 8087,
  },

  modules: [
    '@pinia/nuxt',
    '@element-plus/nuxt',
  ],

  vite: {
    plugins: [tailwindcss()],
  },

  nitro: {
    devProxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
    },
  },

  runtimeConfig: {
    public: {
      apiBase: '/api/v1',
    },
  },
})
