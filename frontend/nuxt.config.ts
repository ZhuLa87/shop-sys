// https://nuxt.com/docs/api/configuration/nuxt-config
import tailwindcss from '@tailwindcss/vite'
import { readFileSync } from 'node:fs'
import { homedir } from 'node:os'
import { join } from 'node:path'

const certDir = join(homedir(), 'tailscale-cert')

function loadHttps() {
  try {
    return {
      key: readFileSync(join(certDir, 'tu-zhu.soay-fish.ts.net.key'), 'utf8'),
      cert: readFileSync(join(certDir, 'tu-zhu.soay-fish.ts.net.crt'), 'utf8'),
    }
  } catch {
    return undefined
  }
}

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',

  future: {
    compatibilityVersion: 4
  },

  devtools: { enabled: true },

  devServer: {
    host: '0.0.0.0',
    port: 8087,
    https: loadHttps(),
  },

  modules: [
    '@pinia/nuxt',
    '@element-plus/nuxt',
  ],

  css: ['~/assets/css/main.css'],

  vite: {
    plugins: [tailwindcss()],
    server: {
      allowedHosts: ['tu-zhu.soay-fish.ts.net']
    },
    optimizeDeps: {
      include: [
        'dayjs',
        'dayjs/plugin/*.js',
        'lodash-unified',
      ]
    },
  },

  nitro: {
    devProxy: {
      '/api': {
        target: 'http://localhost:8088/api',
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
