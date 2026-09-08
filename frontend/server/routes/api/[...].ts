import { proxyRequest } from 'h3'

/**
 * SSR 期間 nitro 內部把 /api/** 轉發到後端.
 *
 * 為什麼不用 nuxt.config 的 routeRules / devProxy:
 *   - devProxy 只在 nuxt dev 生效,build 之後 SSR 對相對路徑 /api/v1/** 的請求
 *     會打回 nitro 自己而變成 404
 *   - routeRules 的 proxy 目標在 build 當下就被寫死進產出,同一個 image 沒辦法
 *     依環境改變後端位址
 * 這裡在"每次請求"時讀環境變數,dev 與 production build 行為一致,
 * 同一份 image 也能部署到不同環境.
 *
 * 瀏覽器發出的請求由 nginx 直接轉給後端,不會經過這裡.
 */
export default defineEventHandler((event) => {
    const target = process.env.NUXT_API_TARGET ?? 'http://localhost:8088'
    return proxyRequest(event, `${target}${event.path}`)
})
