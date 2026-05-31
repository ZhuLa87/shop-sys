# ShopSys Frontend - 開發規格文件

> 本文件供 AI 協作開發使用,描述前端專案的技術選型,架構決策與開發規範.
> 所有新功能開發與修改都應遵循此文件規範.

**版本**: 1.0.0
**最後更新**: 2026-05-31
**對應後端規格**: [SPEC.md](../SPEC.md)

---

## 目錄

1. [技術堆疊](#1-技術堆疊)
2. [專案結構](#2-專案結構)
3. [路由設計](#3-路由設計)
4. [狀態管理](#4-狀態管理-pinia)
5. [API 整合](#5-api-整合)
6. [認證流程](#6-認證流程)
7. [UI / UX 設計規範](#7-ui--ux-設計規範)
8. [使用者回饋與狀態](#8-使用者回饋與狀態)
9. [效能優化](#9-效能優化)
10. [SEO 規範](#10-seo-規範)
11. [安全性](#11-安全性)
12. [深色模式](#12-深色模式)
13. [測試規範](#13-測試規範)
14. [開發規範](#14-開發規範)

---

## 1. 技術堆疊

| 類別 | 技術 | 版本 / 說明 |
| :--- | :--- | :--- |
| 框架 | Nuxt 3 | compatibility version 4, `future.compatibilityVersion: 4` |
| 語言 | TypeScript | 嚴格模式 |
| UI 元件庫 | Element Plus | 透過 `@element-plus/nuxt` 自動匯入 |
| CSS 框架 | Tailwind CSS 4 | 透過 `@tailwindcss/vite` 插件驅動 |
| 狀態管理 | Pinia | 透過 `@pinia/nuxt` 自動匯入 |
| HTTP 客戶端 | Nuxt 內建 `$fetch` / `useFetch` | 封裝於 `useApi` composable |
| 開發伺服器 | port 8087 | API Proxy → `http://localhost:8088/api` |

### 版本說明

專案使用 Nuxt 4 相容模式 (`future.compatibilityVersion: 4`),因此:
- 原始碼置於 `app/` 目錄下 (非傳統 Nuxt 3 的根目錄)
- 自動匯入規則與 Nuxt 4 相同

---

## 2. 專案結構

```
frontend/
├── app/
│   ├── app.vue                  # 根元件,掛載 <NuxtLayout> + <NuxtPage>
│   ├── assets/
│   │   └── css/
│   │       └── main.css         # Tailwind 入口 + 全域樣式 / 動畫
│   ├── composables/
│   │   ├── useApi.ts            # HTTP 客戶端封裝 (所有 API 呼叫都走這裡)
│   │   └── useNotify.ts         # ElNotification 封裝 (統一通知入口)
│   ├── layouts/
│   │   └── default.vue          # 共用版面:Header (導覽列) + Footer
│   ├── pages/
│   │   ├── index.vue            # 商品列表 (首頁)
│   │   ├── login.vue            # 登入
│   │   ├── register.vue         # 註冊
│   │   ├── cart.vue             # 購物車
│   │   ├── profile.vue          # 個人設定 (需登入)
│   │   ├── orders/
│   │   │   ├── index.vue        # 我的訂單列表 (需登入)
│   │   │   └── [id].vue         # 訂單詳情 (需登入)
│   │   ├── products/
│   │   │   └── [id].vue         # 商品詳情
│   │   └── admin/
│   │       ├── products.vue     # 商品管理後台 (需 PRODUCT_MANAGER+)
│   │       ├── users.vue        # 會員管理 (需 SUPER_ADMIN)
│   │       └── logs.vue         # 庫存稽核日誌 (需 PRODUCT_MANAGER+)
│   └── stores/
│       ├── auth.ts              # 認證狀態 + 使用者資料
│       └── cart.ts              # 購物車狀態
├── nuxt.config.ts
└── package.json
```

### 目錄職責規則

| 目錄 | 放什麼 | 不放什麼 |
| :--- | :--- | :--- |
| `pages/` | 路由頁面元件,只負責組合 layout 與呼叫 store/composable | 直接呼叫 `$fetch`、業務邏輯 |
| `composables/` | 可複用的狀態邏輯,API 封裝,工具函式 | 純 UI 元件 |
| `stores/` | 需要跨元件共享的全局狀態 | 僅單一頁面使用的區域狀態 |
| `layouts/` | Header / Footer / Sidebar 等持久化版面元件 | 頁面特定的 UI 邏輯 |
| `assets/css/` | 全域樣式,Tailwind 設定,CSS 動畫 | 元件級樣式 (用 scoped 或 Tailwind) |

---

## 3. 路由設計

### 路由總覽

| 路徑 | 頁面 | 權限 |
| :--- | :--- | :--- |
| `/` | 商品列表 (首頁) | 公開 |
| `/products/:id` | 商品詳情 | 公開 |
| `/login` | 登入 | 公開 (已登入者應重導至首頁) |
| `/register` | 會員註冊 | 公開 |
| `/cart` | 購物車 | 需登入 |
| `/orders` | 我的訂單列表 | 需登入 |
| `/orders/:id` | 訂單詳情 | 需登入 |
| `/profile` | 個人設定 | 需登入 |
| `/admin/products` | 商品管理後台 | `PRODUCT_MANAGER` 或 `SUPER_ADMIN` |
| `/admin/logs` | 庫存稽核日誌 | `PRODUCT_MANAGER` 或 `SUPER_ADMIN` |
| `/admin/users` | 會員權限管理 | `SUPER_ADMIN` |

### 路由守衛

目前權限守衛在頁面元件內透過 `authStore` 判斷並呼叫 `navigateTo()`.
未來若加入 Nuxt Middleware,應建立 `middleware/auth.ts` 與 `middleware/admin.ts`.

```ts
// 頁面內權限守衛範例 (目前採用的模式)
onMounted(() => {
  if (!authStore.isAuthenticated) {
    navigateTo('/login')
    return
  }
  if (!authStore.isProductManager) {
    navigateTo('/')
    return
  }
})
```

---

## 4. 狀態管理 (Pinia)

### 設計原則

- **只有跨頁面共享的狀態才放 store**,單一頁面的 UI 狀態用 `ref` / `reactive` 維持在元件內
- Store 使用 **Composition API 風格** (`defineStore('name', () => { ... })`)
- Store 間可以互相引用 (例如 `auth` 與 `cart` 互相依賴),但需注意循環依賴

### `useAuthStore` (`stores/auth.ts`)

| 狀態 / 計算 | 型別 | 說明 |
| :--- | :--- | :--- |
| `token` | `Cookie<string \| null>` | Access Token,存於 Cookie |
| `refreshToken` | `Cookie<string \| null>` | Refresh Token,存於 Cookie |
| `userProfile` | `Cookie<UserProfile \| null>` | 使用者資料快取 |
| `isAuthenticated` | `computed<boolean>` | `!!token.value` |
| `user` | `computed<UserProfile \| null>` | `userProfile.value` |
| `role` | `computed<string \| null>` | 角色字串 |
| `isCustomer` | `computed<boolean>` | role === 'CUSTOMER' |
| `isProductManager` | `computed<boolean>` | PRODUCT_MANAGER 或 SUPER_ADMIN |
| `isSuperAdmin` | `computed<boolean>` | role === 'SUPER_ADMIN' |

| 方法 | 說明 |
| :--- | :--- |
| `login(email, password)` | 登入,寫入 token,拉取 profile,初始化購物車 |
| `register(body)` | 會員註冊 |
| `fetchProfile()` | 刷新個人資料快取 |
| `updateProfile(body)` | 更新資料後重新拉取 profile |
| `logout()` | 通知後端黑名單,清除本地狀態,跳轉 `/login` |

### `useCartStore` (`stores/cart.ts`)

| 狀態 / 計算 | 型別 | 說明 |
| :--- | :--- | :--- |
| `items` | `ref<CartItem[]>` | 購物車項目清單 |
| `totalCount` | `computed<number>` | 所有商品數量加總 (用於 Header badge) |
| `totalAmount` | `computed<number>` | 總金額 |

| 方法 | 說明 |
| :--- | :--- |
| `fetchCart()` | 從後端拉取最新購物車 |
| `addToCart(productId, quantity)` | 加入商品,成功後重新拉取 |
| `removeFromCart(cartItemId)` | 刪除項目 |
| `updateCartItemQuantity(productId, target, cartItemId)` | 透過刪除/重加的方式更新數量 (後端不支援 PATCH) |
| `clearCartState()` | 清除本地狀態 (登出時使用) |

### 新增 Store 規則

```ts
// 正確: Composition API 風格
export const useXxxStore = defineStore('xxx', () => {
  const items = ref<XxxItem[]>([])
  const totalCount = computed(() => items.value.length)

  const fetchItems = async () => { ... }

  return { items, totalCount, fetchItems }
})

// 禁止: Options API 風格 (專案不使用)
export const useXxxStore = defineStore('xxx', {
  state: () => ({ ... }),
  getters: { ... },
  actions: { ... }
})
```

---

## 5. API 整合

### `useApi` composable (`composables/useApi.ts`)

所有 API 呼叫都必須透過此 composable,禁止在頁面或 store 中直接使用裸 `$fetch`.

#### 兩種呼叫方式

```ts
const api = useApi()

// 方式 1: request() - 用於使用者觸發的操作 (按鈕點擊,表單送出)
// 回傳 Promise<ApiResponse<T>>,需自行 try/catch
const submit = async () => {
  try {
    const res = await api.request<ProductResponse>('/products/1')
    // res.data 已是 T 型別
  } catch (error: any) {
    // error.message 已被 useApi 轉換為後端訊息
    notify({ title: '錯誤', message: error.message, type: 'error' })
  }
}

// 方式 2: fetch() - 用於頁面資料載入 (支援 SSR)
// 回傳 useFetch 的 { data, pending, error } 響應式物件
const { data, pending } = api.fetch<ProductResponse>('/products/1')
```

#### 選用原則

| 場景 | 使用方式 |
| :--- | :--- |
| 頁面初始化資料,需要 SEO / SSR | `fetch()` (wraps `useFetch`) |
| 按鈕點擊,表單送出,刪除操作 | `request()` (wraps `$fetch`) |
| 管理後台 (不需 SEO) 的列表載入 | `request()` + `onMounted` |

#### 統一回應格式

後端所有 API 回傳 `ApiResponse<T>`:

```ts
interface ApiResponse<T> {
  success: boolean  // true = 成功
  message: string   // 操作說明或錯誤訊息
  data: T           // 實際資料
}
```

### Token 自動刷新

`useApi` 內建 401 自動重試機制:
1. 請求失敗 (status 401) → 嘗試用 `refresh_token` 換新 token
2. 換新成功 → 自動重試原始請求一次
3. 換新失敗 → 清除所有 Cookie,跳轉 `/login`
4. 防止並發刷新:singleton `refreshingPromise` 確保同時多個 401 只觸發一次換新

---

## 6. 認證流程

### Token 儲存策略

所有憑證以 **Cookie** 儲存,不使用 `localStorage`.原因:SSR 環境下 Cookie 可由 server-side 讀取,localStorage 無法.

```ts
// Cookie 設定 (sameSite + secure)
const token = useCookie<string | null>('auth_token', {
  sameSite: 'lax',
  secure: true,      // 僅在 HTTPS 下傳輸
})
```

| Cookie 名稱 | 內容 | 用途 |
| :--- | :--- | :--- |
| `auth_token` | JWT Access Token | API 請求授權 Header |
| `refresh_token` | Refresh Token | 換發新 Access Token |
| `user_profile` | JSON 序列化的使用者資料 | 避免每次重新載入都呼叫 `/users/me` |

### 登入流程

```
POST /auth/login
  → 寫入 auth_token + refresh_token cookie
  → GET /users/me → 寫入 user_profile cookie
  → GET /carts → 初始化購物車 store
  → navigateTo('/')
```

### 登出流程

```
POST /auth/logout (帶 refreshToken body)
  → 後端黑名單 Access Token,刪除 Refresh Token
  → 清除 auth_token, refresh_token, user_profile cookie
  → cartStore.clearCartState()
  → window.location.href = '/login'
```

### 角色判斷

不要直接比較 `role` 字串,使用 store 提供的 computed:

```ts
// 正確
if (authStore.isProductManager) { ... }
if (authStore.isSuperAdmin) { ... }
if (authStore.isAuthenticated) { ... }

// 禁止
if (authStore.role === 'PRODUCT_MANAGER') { ... }
```

---

## 7. UI / UX 設計規範

### 品牌色彩

| 角色 | 色彩 | Tailwind class |
| :--- | :--- | :--- |
| 主色 (Primary) | Indigo 600 | `bg-indigo-600`, `text-indigo-600`, `border-indigo-600` |
| 主色 Hover | Indigo 700 | `hover:bg-indigo-700` |
| 背景 | Slate 50 | `bg-slate-50` (全域 body) |
| 卡片背景 | White | `bg-white` |
| 主文字 | Slate 900 | `text-slate-900` |
| 次要文字 | Slate 600 | `text-slate-600` |
| 提示文字 | Slate 400 | `text-slate-400` |
| 邊框 | Slate 200 | `border-slate-200` |
| 成功 | Emerald 700 | `text-emerald-700`, `bg-emerald-50` |
| 警告 | Amber 700 | `text-amber-700`, `bg-amber-50` |
| 危險 | Rose 600 | `text-rose-600` |

### 版面容器

所有頁面內容都應在統一容器內:

```html
<!-- 全域容器 (定義於 layouts/default.vue) -->
<div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
  <!-- 頁面內容 -->
</div>
```

禁止在頁面元件自行定義不同寬度的容器.

### RWD 斷點

使用 Tailwind 預設斷點:

| 前綴 | 寬度 | 對應裝置 |
| :--- | :--- | :--- |
| (無) | 0px+ | 手機 (mobile-first) |
| `sm:` | 640px+ | 大手機 / 小平板 |
| `md:` | 768px+ | 平板 |
| `lg:` | 1024px+ | 桌機 |
| `xl:` | 1280px+ | 寬螢幕 |

#### 響應式常用模式

```html
<!-- 商品格線 -->
<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">

<!-- 雙欄版面 (詳情頁) -->
<div class="grid grid-cols-1 lg:grid-cols-2 gap-10 lg:gap-16">

<!-- 桌機顯示,手機隱藏 -->
<nav class="hidden md:flex items-center gap-6">

<!-- 手機顯示,桌機隱藏 -->
<button class="md:hidden ...">

<!-- 響應式文字大小 -->
<h1 class="text-3xl sm:text-4xl md:text-5xl font-extrabold">
```

### 導覽列規範

- 導覽列為 **sticky top-0 z-50**,搭配毛玻璃效果 (`backdrop-blur-md bg-white/75`)
- 高度固定 64px (`h-16`)
- 手機版: Hamburger 按鈕切換展開式選單
- 登入狀態: 顯示使用者 avatar (名字第一字) + Dropdown 選單
- 未登入: 顯示「登入」連結 + 「註冊」主要按鈕

### 按鈕規範

| 類型 | 樣式 | 使用場景 |
| :--- | :--- | :--- |
| 主要 (Primary) | `bg-indigo-600 text-white hover:bg-indigo-700 rounded-lg` | 主要 CTA |
| 次要 (Secondary) | `border border-slate-200 text-slate-600 hover:bg-slate-50 rounded-lg` | 次要操作 |
| 危險 (Danger) | `text-rose-600 border-rose-200` | 刪除/停權等破壞性操作 |

- 非同步操作期間必須設定 `disabled` 並顯示 loading 指示器
- 禁用狀態:`disabled:opacity-50 disabled:cursor-not-allowed`

### 卡片規範

```html
<!-- 標準商品卡片 -->
<div class="bg-white border border-slate-200/80 rounded-xl overflow-hidden
            hover:-translate-y-1 hover:shadow-lg transition-all duration-300">
```

---

## 8. 使用者回饋與狀態

### 載入狀態 (Skeleton Loading)

**規則:禁止顯示空白頁面.** 資料載入中必須顯示骨架屏.

```html
<!-- Skeleton 範例 -->
<div v-if="loading" class="animate-pulse space-y-4">
  <div class="bg-slate-100 rounded h-48 w-full"></div>
  <div class="bg-slate-100 rounded h-4 w-2/3"></div>
  <div class="bg-slate-100 rounded h-4 w-1/2"></div>
</div>

<div v-else-if="items.length === 0">
  <!-- 空資料狀態 -->
</div>

<div v-else>
  <!-- 正常資料 -->
</div>
```

### 空資料狀態

資料為空時必須顯示友善的空白頁面,不得顯示空容器:

```html
<!-- 空資料範例 -->
<div class="flex flex-col items-center justify-center py-20 text-center space-y-4">
  <div class="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center text-slate-400">
    <!-- 相關 Icon (SVG) -->
  </div>
  <div class="space-y-1">
    <h3 class="font-bold text-slate-800">查無資料</h3>
    <p class="text-sm text-slate-400">描述原因或操作提示</p>
  </div>
</div>
```

### 通知系統 (Notify)

**所有使用者操作反饋都必須透過 `notify()` 函式**,禁止直接呼叫 `ElNotification` 或 `ElMessage`.

```ts
import { notify } from '~/composables/useNotify'

// 成功
notify({ title: '操作成功', message: '商品已加入購物車.', type: 'success', duration: 3000 })

// 錯誤
notify({ title: '操作失敗', message: error.message, type: 'error', duration: 4000 })

// 警告
notify({ title: '請注意', message: '請先登入才能繼續.', type: 'warning', duration: 3000 })

// 資訊
notify({ title: '提醒', message: '訂單已送出,請等待處理.', type: 'info', duration: 3000 })
```

`notify()` 已自動計算 offset,使通知不被 sticky 導覽列遮擋.

### 通知時機規範

| 事件 | 是否通知 | 類型 | duration |
| :--- | :--- | :--- | :--- |
| 成功送出表單 | 是 | success | 3000ms |
| API 錯誤 | 是 | error | 4000ms |
| 需要登入才能操作 | 是 | warning | 3000ms |
| 刪除操作前確認 | 使用 `ElMessageBox.confirm` | - | - |
| 頁面載入失敗 | 是 | error | 4000ms |
| 頁面資料更新 (自動刷新) | 否 | - | - |

### 非同步按鈕

```html
<button
  :disabled="submitting"
  @click="handleSubmit"
  class="... disabled:opacity-50 disabled:cursor-not-allowed"
>
  <!-- 載入指示器 -->
  <span v-if="submitting" class="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full" />
  {{ submitting ? '處理中...' : '送出' }}
</button>
```

---

## 9. 效能優化

### 圖片優化

```html
<!-- 所有 <img> 必須包含: -->
<img
  :src="product.coverImageUrl || '/fallback.jpg'"
  :alt="product.name"
  loading="lazy"
  class="w-full h-full object-cover"
/>
```

- 必須設定 `loading="lazy"` (非首屏圖片)
- 必須提供 `:alt` 文字 (無障礙 + SEO)
- 必須提供備用圖片 fallback

### 搜尋 Debounce

使用者輸入觸發 API 請求時必須加入 debounce:

```ts
// 搜尋防抖 (最小 300ms)
let debounceTimer: ReturnType<typeof setTimeout>
const handleSearch = () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    currentPage.value = 1
    loadData()
  }, 350)
}
```

### 分頁策略

- 預設每頁 20 筆
- 使用 Element Plus `<el-pagination>` 元件
- **注意**: 後端 page 參數為 0-indexed,Element Plus 為 1-indexed,需做轉換:
  ```ts
  const pageIndex = currentPage.value - 1  // 傳給後端時 -1
  ```

### 依賴預優化

大型依賴已在 `nuxt.config.ts` 中加入 `optimizeDeps.include`:
```ts
optimizeDeps: {
  include: ['dayjs', 'dayjs/plugin/*.js', 'lodash-unified']
}
```

新增大型依賴時一併加入此清單.

---

## 10. SEO 規範

Nuxt 3 SSR 提供完整 SEO 支援.所有面向顧客的公開頁面必須設定 meta:

```ts
// pages/products/[id].vue 範例
useSeoMeta({
  title: () => product.value ? `${product.value.name} - ShopSys` : 'ShopSys',
  description: () => product.value?.description ?? '探索 ShopSys 精選商品',
  ogTitle: () => product.value?.name,
  ogDescription: () => product.value?.description,
  ogImage: () => product.value?.coverImageUrl,
})
```

### SEO 規則

| 頁面 | 必要 meta |
| :--- | :--- |
| 首頁 `/` | title, description, og:title, og:description |
| 商品詳情 `/products/:id` | title (含商品名), description, og:image |
| 管理後台 `/admin/*` | 不需要 (robots noindex) |
| 登入/註冊 | 基本 title 即可 |

- `title` 格式:`{頁面名稱} - ShopSys`
- 管理後台頁面加上:`useHead({ meta: [{ name: 'robots', content: 'noindex' }] })`

---

## 11. 安全性

### XSS 防護

- 使用 Vue 模板 `{{ }}` 自動 HTML 跳脫,**禁止使用 `v-html`** (除非內容來自可信任的 Markdown 渲染器)
- 不在 URL 參數中傳遞敏感資料 (token, password)

### Token 安全

- Token **只存 Cookie**,禁止存入 `localStorage` 或 `sessionStorage`
- Cookie 設定:`sameSite: 'lax'`, `secure: true` (HTTPS-only)
- 不在 `console.log` / 錯誤訊息中輸出 Token 內容

### 輸入驗證

- 前端驗證只是體驗優化,後端驗證才是安全底線
- 使用 Element Plus 表單驗證 (`el-form` + `rules`) 提供即時回饋
- 搜尋關鍵字透過 `encodeURIComponent()` 後再加入 URL 參數

```ts
// 正確
path += `&keyword=${encodeURIComponent(searchQuery.value.trim())}`
```

### 路由權限

- 管理後台頁面在 `onMounted` 中驗證角色,未授權者立即跳轉首頁
- 即使有前端守衛,後端 API 仍有獨立的授權驗證

---

## 12. 深色模式

**狀態: 計畫中 (尚未實作)**

### 計畫實作方式

使用 Nuxt 內建 `@nuxtjs/color-mode` 模組:
- 偵測系統偏好 (`prefers-color-scheme`)
- 支援使用者手動切換 (Light / Dark / System)
- 偏好設定存於 `localStorage`

### 實作注意事項

- Tailwind CSS 4 使用 `dark:` variant
- Element Plus 需要設定 `dark mode class`: `html.dark`
- 所有顏色類別需同步補充 `dark:` 版本
- 避免 hardcode `#ffffff` / `#000000`,統一使用 Tailwind CSS 語意變數

```html
<!-- 實作後的顏色範例 -->
<div class="bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100">
```

---

## 13. 測試規範

**狀態: 規劃中 (尚未設置測試環境)**

### 測試工具選型

| 層級 | 工具 | 用途 |
| :--- | :--- | :--- |
| 單元測試 | Vitest | Composable 邏輯,Store 方法,工具函式 |
| 元件測試 | Vitest + Vue Test Utils | UI 元件互動行為 |
| E2E 測試 | Playwright | 關鍵使用者流程 (登入,購物,結帳) |

### 目錄結構 (待建立)

```
frontend/
├── tests/
│   ├── unit/
│   │   ├── composables/
│   │   │   └── useApi.test.ts
│   │   └── stores/
│   │       ├── auth.test.ts
│   │       └── cart.test.ts
│   └── e2e/
│       ├── auth.spec.ts        # 登入/登出/註冊
│       ├── product.spec.ts     # 商品瀏覽/搜尋
│       └── checkout.spec.ts    # 加入購物車/結帳
```

### 單元測試範例 (Vitest)

```ts
// tests/unit/stores/auth.test.ts
import { setActivePinia, createPinia } from 'pinia'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useAuthStore } from '~/stores/auth'

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('isAuthenticated 在 token 存在時為 true', () => {
    const auth = useAuthStore()
    // 設定 mock token
    auth.token = 'mock-token'
    expect(auth.isAuthenticated).toBe(true)
  })

  it('logout 應清除所有 cookie 狀態', async () => {
    const auth = useAuthStore()
    vi.spyOn(auth, 'logout')
    await auth.logout()
    expect(auth.token).toBeNull()
    expect(auth.userProfile).toBeNull()
  })
})
```

### E2E 測試範例 (Playwright)

```ts
// tests/e2e/auth.spec.ts
import { test, expect } from '@playwright/test'

test('使用者可以登入並看到購物車圖示', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[type="email"]', 'test01@example.com')
  await page.fill('input[type="password"]', 'mypassword123')
  await page.click('button[type="submit"]')

  await expect(page).toHaveURL('/')
  await expect(page.locator('[data-testid="cart-icon"]')).toBeVisible()
})
```

### 關鍵流程測試清單 (E2E)

- [ ] 會員註冊
- [ ] 登入 / 登出
- [ ] 商品列表搜尋與分頁
- [ ] 加入購物車 (含庫存不足情境)
- [ ] 結帳流程 (完整購物車 → 建立訂單)
- [ ] 訂單詳情查看
- [ ] 管理後台:新增/編輯商品
- [ ] Token 過期自動刷新

---

## 14. 開發規範

### 寫作規則

- **禁止使用全形中文標點符號**,一律使用半形 ASCII 標點
  - 禁止: `，。：；！？（）「」【】、`
  - 使用: `,.:;!?()"'[]`

### 命名慣例

| 類型 | 慣例 | 範例 |
| :--- | :--- | :--- |
| 元件 (`.vue`) | PascalCase | `ProductCard.vue` |
| Composable | camelCase,以 `use` 開頭 | `useProductList.ts` |
| Store | camelCase,以 `use` 開頭 | `useAuthStore` |
| 頁面 (`pages/`) | kebab-case 或 Nuxt 慣例 | `[id].vue` |
| CSS class | Tailwind utility class | 直接寫於模板 |
| 事件 handler | `handle` 前綴 | `handleSubmit`, `handleSearch` |
| async 載入函式 | `load` 或 `fetch` 前綴 | `loadProducts`, `fetchCart` |

### Vue 元件結構順序

```vue
<template>
  <!-- 模板 -->
</template>

<script setup lang="ts">
// 1. imports
// 2. composables / stores
// 3. props / emits (if any)
// 4. reactive state (ref / reactive)
// 5. computed
// 6. methods / handlers
// 7. lifecycle hooks (onMounted, watch)
</script>

<style scoped>
/* 只放無法用 Tailwind 實作的樣式,例如 :deep() Element Plus 覆蓋 */
</style>
```

### API 呼叫規則

1. 使用 `useApi()` composable,禁止裸 `$fetch`
2. 所有 `request()` 呼叫都必須包裹 `try/catch`
3. `catch` 區塊必須呼叫 `notify()` 告知使用者
4. 操作期間設定 `loading` / `submitting` flag 防止重複點擊

```ts
// 標準操作模式
const submitting = ref(false)

const handleSubmit = async () => {
  submitting.value = true
  try {
    const res = await api.request('/endpoint', { method: 'POST', body: payload })
    if (res.success) {
      notify({ title: '成功', message: res.message, type: 'success', duration: 3000 })
    }
  } catch (error: any) {
    notify({ title: '失敗', message: error.message, type: 'error', duration: 4000 })
  } finally {
    submitting.value = false
  }
}
```

### 禁止事項

| 禁止行為 | 原因 |
| :--- | :--- |
| 直接呼叫 `$fetch` | 繞過 token 注入和 401 自動刷新 |
| 將 Token 存入 `localStorage` | SSR 無法讀取,且有 XSS 風險 |
| 使用 `v-html` | XSS 攻擊向量 |
| 直接比較 `role` 字串 | 應使用 store computed (可讀性,可維護性) |
| 元件內直接呼叫 `ElNotification` / `ElMessage` | 應統一透過 `notify()` 保持 offset 一致 |
| 圖片省略 `loading="lazy"` 或 `alt` | 效能與無障礙要求 |
| 搜尋輸入不加 debounce | 過度觸發 API 請求 |
| 管理後台頁面忘記權限守衛 | 安全漏洞 |
| `<img>` 無 fallback src | 圖片載入失敗時畫面破版 |
