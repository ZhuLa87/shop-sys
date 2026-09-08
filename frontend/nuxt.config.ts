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

// 在 docker 中執行時,TLS 由 nginx 終結,dev server 只需跑純 HTTP;
// 直接在主機上 pnpm dev 時維持原本的 HTTPS + 8087 行為
const devPort = Number(process.env.NUXT_DEV_PORT ?? 8087)
const devHttps = process.env.NUXT_DEV_HTTPS === 'false' ? undefined : loadHttps()

// 經由 nginx 轉發進來時 Host 是對外網域,Vite 預設會擋掉
const allowedHost = process.env.NUXT_DEV_ALLOWED_HOST ?? 'tu-zhu.soay-fish.ts.net'

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',

  future: {
    compatibilityVersion: 4
  },

  devtools: { enabled: true },

  devServer: {
    host: '0.0.0.0',
    port: devPort,
    https: devHttps,
  },

  modules: [
    '@pinia/nuxt',
    '@element-plus/nuxt',
  ],

  css: ['~/assets/css/main.css', 'cropperjs/dist/cropper.css'],

  vite: {
    plugins: [tailwindcss()],
    server: {
      allowedHosts: [allowedHost]
    },
    optimizeDeps: {
      include: [
        'dayjs',
        'dayjs/plugin/*.js',
        'lodash-unified',
        'cropperjs',
      ]
    },
  },

  // SSR 期間對後端的轉發改由 server/routes/api/[...].ts 處理
  // (在執行時讀 NUXT_API_TARGET,build 產出才不會綁死某個環境)

  runtimeConfig: {
    public: {
      apiBase: '/api/v1',
    },
  },
})
