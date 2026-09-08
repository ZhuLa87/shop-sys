# 正式 / staging 用:build 出 Nuxt 的 .output 後以 node 執行
# build context 是 frontend/

# ── build ────────────────────────────────────────────────────────────────────
FROM node:22-alpine AS build
WORKDIR /app

RUN npm install -g pnpm@10.33.0

# 先只複製 lockfile 安裝相依,讓這層在原始碼變動時仍能命中 layer cache
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
# postinstall 會執行 nuxt prepare,此時尚無原始碼,先跳過
RUN pnpm install --frozen-lockfile --ignore-scripts

COPY . .
RUN pnpm exec nuxt prepare && pnpm run build

# ── runtime ──────────────────────────────────────────────────────────────────
FROM node:22-alpine AS runtime
WORKDIR /app

# Alpine 預設不含時區資料庫,少了它 TZ 環境變數完全無效,容器會停在 UTC.
# 對 SSR 來說這是實質 bug:頁面用 toLocaleString('zh-TW') 格式化時間,
# 伺服器端算出 UTC,瀏覽器端算出當地時間,兩邊對不上就是 hydration mismatch.
RUN apk add --no-cache tzdata

ENV NODE_ENV=production \
    NITRO_HOST=0.0.0.0 \
    NITRO_PORT=3000

# .output 已包含所有需要的相依,不必再帶 node_modules
COPY --from=build --chown=node:node /app/.output ./.output

USER node
EXPOSE 3000

CMD ["node", ".output/server/index.mjs"]
