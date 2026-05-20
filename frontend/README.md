# shop-sys Frontend

Nuxt 4 前端專案,對接 Spring Boot REST API (`:8088/api/v1`) .

- 顧客端:SSR 公開頁面 + 認證後互動
- 後台管理:SPA 模式,依角色動態選單
- 套件管理:pnpm

---

## 技術棧

| 類別 | 技術 | 說明 |
|------|------|------|
| 框架 | **Nuxt 4** | Node.js 22.x 以上 |
| 語言 | TypeScript | 全專案強型別 |
| UI 元件庫 | Element Plus | 顧客端 + 後台共用,Auto-import |
| CSS | Tailwind CSS v4 | `@tailwindcss/vite` 插件,CSS-first,無須 config 檔 |
| 狀態管理 | Pinia | Nuxt 官方推薦,SSR 安全 |
| 表單驗證 | VeeValidate + Zod | schema-based 驗證 |
| 套件管理 | pnpm | 速度快,Monorepo 相容 |

---

## 快速開始

### 1. 初始化專案

```bash
# 需要 Node.js 22.x 以上
node -v

# 在 shop-sys/frontend/ 目錄下執行
pnpm create nuxt@latest .

# 互動選項建議:
# ✔ Package manager: pnpm
# ✔ TypeScript: Yes
# ✔ Git: No (由上層 repo 管控) 
```

### 2. 安裝額外依賴

```bash
# UI
pnpm add element-plus @element-plus/icons-vue @element-plus/nuxt

# Tailwind CSS v4 (透過 Vite 插件整合,無需 Nuxt module) 
pnpm add -D tailwindcss @tailwindcss/vite

# 狀態管理
pnpm add @pinia/nuxt pinia

# 表單驗證 (注意:@vee-validate/zod 目前僅支援 zod v3) 
pnpm add vee-validate @vee-validate/zod zod@^3.24.0
```

### 3. 啟動開發伺服器

```bash
# 確保 Spring Boot 已在 :8088 運行
pnpm dev
# → http://localhost:8087
```

---

## nuxt.config.ts 核心設定

```ts
import tailwindcss from '@tailwindcss/vite'

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',

  devtools: { enabled: true },

  modules: [
    '@pinia/nuxt',
    '@element-plus/nuxt',
  ],

  // Tailwind CSS v4 透過 Vite 插件整合 (不需 Nuxt module) 
  vite: {
    plugins: [tailwindcss()],
  },

  // 開發環境 API Proxy (避免 CORS) 
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
```

---

## 目錄結構

Nuxt 4 預設將應用程式碼放在 `app/` 子目錄:

```
frontend/
├── app/                              ← 應用層根目錄
│   ├── assets/
│   │   └── css/
│   │       └── main.css              # Tailwind v4 配置點 (@theme)
│   ├── pages/
│   │   ├── index.vue                 # 首頁 (SSR) 
│   │   ├── products/
│   │   │   ├── index.vue             # 商品列表
│   │   │   └── [id].vue              # 商品詳情
│   │   ├── admin/                    # 後台 (ssr: false) 
│   │   │   ├── index.vue
│   │   │   └── login.vue
│   │   └── ...
│   │
│   ├── layouts/
│   │   ├── default.vue
│   │   └── admin.vue
│   │
│   ├── components/
│   │   ├── product/
│   │   └── admin/
│   │
│   ├── composables/
│   │   ├── useAuth.ts
│   │   └── useApiFetch.ts
│   │
│   ├── stores/
│   │   └── cart.ts
│   │
│   ├── middleware/
│   │   ├── auth.ts
│   │   └── admin.ts
│   │
│   ├── utils/                        # 純邏輯函數 (如 formatPrice) 
│   ├── constants/                    # 枚舉與常數 (如訂單狀態) 
│   ├── types/
│   │   └── api.ts                    # API 介面定義
│   │
│   └── app.vue
│
├── public/
├── server/                           # Nitro API Routes / Middleware
├── nuxt.config.ts
├── tsconfig.json
└── package.json
```

---

## 認證設計

### Token 儲存策略

| Token | 儲存位置 | 理由 |
|-------|---------|------|
| Access Token (短效)  | `useState()` | SSR 友好,防止 XSS |
| Refresh Token (長效)  | `useCookie()` | 跨請求持久化,支援 HttpOnly |

### useApiFetch 流程與 SSR 安全性

```
useApiFetch('/v1/orders')
  ├── 附加 Authorization: Bearer <accessToken>
  ├── 若 401:
  │    ├── Client-side: 呼叫 /refresh,成功則更新 Token 並重試
  │    └── Server-side: 直接導向登入頁 (防止伺服器端 Token 刷新導致的 Header 衝突)
  └── 回傳 data / error
```

---

## Composables

所有 composable 放在 `app/composables/`,Nuxt 自動 import,**頁面不需手動引入**.

### `useApi`

封裝 `$fetch` / `useFetch`,自動附加 JWT,處理 401 → token 刷新 → 重試.

| 方法 | 適用場景 |
|------|---------|
| `useApi().request(path, options)` | 按鈕點擊,表單送出等主動操作 |
| `useApi().fetch(path, options)` | 頁面初始載入 (支援 SSR)  |

```ts
const api = useApi()

// 主動操作
const res = await api.request('/orders', { method: 'POST', body: { ... } })

// SSR 資料載入
const { data } = await api.fetch('/products')
```

### `useNotify`

統一包裝 `ElNotification`,**自動將 top 方向通知下移 72px** (nav 高度 64px + 8px 間距) ,避免通知遮住頂部導覽列.

```ts
// ✅ 正確:使用 notify,自動套用 offset
notify({ title: '成功', message: '...', type: 'success' })

// ❌ 禁止:直接呼叫 ElNotification,通知會蓋住 nav
ElNotification({ title: '成功', message: '...' })
```

| `position` | offset 行為 |
|------------|------------|
| `top-right` (預設) | 72px from top (nav 下方)  |
| `top-left` | 72px from top |
| `bottom-right` | 16px from bottom (正常)  |
| `bottom-left` | 16px from bottom (正常)  |

> **規則**:任何頁面或 composable 一律使用 `notify()`,不直接呼叫 `ElNotification`.

---

## 環境變數

```bash
# .env (不提交 git) 
NUXT_PUBLIC_API_BASE=http://localhost:8088/api/v1
```

---

## 開發指令

```bash
pnpm dev          # 啟動開發伺服器 → http://0.0.0.0:8087
pnpm build        # 生產建置
pnpm typecheck    # 型別檢查
```