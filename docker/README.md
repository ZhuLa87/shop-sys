# Docker 部署指南

以 docker compose 啟動完整的 shop-sys 環境:nginx 作為統一入口,後方是 Spring Boot,Nuxt SSR,MariaDB,Redis 與 MinIO.

---

## 架構

```
                         ┌──────────────────────────────┐
   瀏覽器 ──── :8443 ────▶│ nginx (TLS 終結)             │
             (HTTPS)     │  /      → frontend:3000      │
                         │  /api/  → backend:8088       │
   瀏覽器 ──── :8444 ────▶│  (獨立 server) → minio:9000  │
             (S3 API)    └──────────────────────────────┘
                                      │ 內部網路 shop-sys-net
                    ┌─────────────────┼──────────────────┐
                    ▼                 ▼                  ▼
              frontend:3000     backend:8088         minio:9000
              (Nuxt SSR)        (Spring Boot)        (物件儲存)
                    │                 │
                    │  SSR 期間       ├──▶ mariadb:3306
                    └────────────────▶│    redis:6379
                       /api/**        │
```

對外只發佈 nginx 的埠.MariaDB / Redis / MinIO 都留在內部網路,開發環境另外開出除錯用埠號.

---

## 快速開始 (開發環境)

```bash
cp env/.env.dev.example env/.env.dev
chmod 600 env/.env.dev
# 把檔案裡的 CHANGE_ME 全部換掉 (openssl rand -base64 18 / -base64 48)

./stack.sh up          # 第一次會 build image,約 5-10 分鐘
./stack.sh logs        # 追蹤啟動過程
```

`./stack.sh` 只是把 `docker compose --env-file ... -f ... -f ...` 包起來,
第一個參數可切換環境 (`./stack.sh staging up`) .根目錄的 `Makefile` 是它的薄包裝,
兩者行為一致;本專案不強制安裝 `make`.

啟動後:

| 位址 | 用途 |
| :--- | :--- |
| `https://tu-zhu.soay-fish.ts.net:8443/` | 前端 |
| `https://tu-zhu.soay-fish.ts.net:8443/api/v1/...` | 後端 API |
| `https://tu-zhu.soay-fish.ts.net:8443/api/swagger-ui/index.html` | Swagger |
| `https://tu-zhu.soay-fish.ts.net:8443/api/health` | 健康檢查 |
| `https://tu-zhu.soay-fish.ts.net:8444/` | MinIO S3 端點 |
| `http://localhost:19001/` | MinIO Console |
| `localhost:13306` | MariaDB (給 DBeaver 等工具) |
| `localhost:16379` | Redis (給 RedisInsight 等工具) |

dev profile 會由 `DataInitializer` 建立測試帳號 (`admin@test.com` / `admin123` 等,詳見啟動日誌) .

常用指令看 `./stack.sh --help`.

---

## Hot reload 怎麼運作

**後端**:容器掛載整個專案並執行 `mvn spring-boot:run`,`spring-boot-devtools` 監看 `target/classes`.
在**主機端**重新編譯就會觸發容器內自動重啟:

```bash
./mvnw compile      # 或讓 IDE 自動編譯
```

實測從編譯完成到容器套用新程式碼約 4 秒.

> 容器以 `DEV_UID` / `DEV_GID` (預設 1000) 執行,與主機使用者相同.
> 若你的 uid 不是 1000,請用 `id -u` / `id -g` 查出後改 `env/.env.dev`,
> 否則容器寫進 `target/` 的檔案會變成 root 所有,主機端的 `./mvnw` 隨即失敗.

**前端**:容器內執行 `nuxt dev`,Vite HMR 的 websocket 經由 nginx 轉發.存檔即生效.

---

## 環境與設定

三個環境共用 `compose.yaml`,差異放在各自的 override:

| 環境 | 檔案 | 特性 |
| :--- | :--- | :--- |
| dev | `compose.dev.yaml` + `env/.env.dev` | 掛載原始碼,hot reload,開除錯埠 |
| staging | `compose.staging.yaml` + `env/.env.staging` | build 產出 (fat jar + `.output`) ,埠號與 dev 錯開 |
| prod | `compose.prod.yaml` + `env/.env.prod` | 同 staging,另加記憶體上限,關閉 Swagger |

所有機密都在 `env/.env.*`,已被 `.gitignore` 排除,版控裡只有 `*.example` 範本.

**三份範本都不含任何可用憑證**,`CHANGE_ME` 必須全部換掉 —— dev 也一樣.
dev 的除錯用埠綁在 `0.0.0.0`,同網段或 tailnet 上的其他裝置連得到資料庫,
那組密碼是真的在擋人,不能當成擺設.
`./stack.sh up` 會在啟動前檢查,有漏填就擋下並列出是哪幾個變數.

```bash
cp env/.env.staging.example env/.env.staging
# 產生金鑰: openssl rand -base64 48
./stack.sh staging up
```

---

## 部署

`./stack.sh` 的第一個參數就是環境,三個環境走同一套指令:

```bash
./stack.sh up              # 等同 ./stack.sh dev up
./stack.sh staging up
./stack.sh prod up
```

### 首次部署到一台新機器

機器上只需要 `docker` 與 `docker compose`.

```bash
git clone <repo> && cd shop-sys

# 1. 建立 env 檔,把所有 CHANGE_ME 換掉
cp env/.env.prod.example env/.env.prod
chmod 600 env/.env.prod
openssl rand -base64 48        # 產生 JWT_SECRET

# 2. 確認設定正確代入 (不會啟動任何東西)
./stack.sh prod config | less

# 3. 啟動 (會在這台機器上 build image)
./stack.sh prod up
./stack.sh prod logs
```

部署前務必確認:

| 項目 | 說明 |
| :--- | :--- |
| `PUBLIC_HOST` | 必須與 TLS 憑證的網域一致 |
| `TLS_CERT_DIR` / `TLS_CERT_FILE` / `TLS_KEY_FILE` | 目錄會以唯讀方式掛進 nginx,檔名要對得上 |
| `PUBLIC_S3_URL` | 埠必須與 `NGINX_S3_PORT` 一致,且是瀏覽器連得到的位址 |
| `NGINX_HTTPS_PORT` / `NGINX_HTTP_PORT` / `NGINX_S3_PORT` | 部署機上不能被其他服務佔用 |
| `env/.env.prod` 權限 | 建議 `600`,且絕對不要 commit |

### 更新既有部署

```bash
git pull
./stack.sh prod up          # staging / prod 的 up 會自動帶 --build
```

Flyway 會在後端啟動時自動套用新的 migration.多副本同時啟動也安全 (見下方設計點) .

資料 volume 不受影響;`./stack.sh prod down` 也只移除容器,**不會**動到資料.
只有 `reset` 會刪除 volume,正式環境請避免使用.

### 改用 registry image

目前 `staging` / `prod` 都是**在部署機器上 build**,適合單機部署.
若要改成 CI 建好 image,部署機只負責拉取,把 `compose.prod.yaml` 的 `build:` 區塊拿掉,
`image:` 指向 registry 位址即可:

```yaml
services:
    backend:
        image: registry.example.com/shop-sys/backend:${IMAGE_TAG}
    frontend:
        image: registry.example.com/shop-sys/frontend:${IMAGE_TAG}
```

`compose.yaml` 與 `./stack.sh` 的用法完全不需要更動,只要在 `env/.env.prod` 指定 `IMAGE_TAG`.

### 目前的限制

- **後端可多副本,前端與 nginx 尚未考慮多副本.** 後端是無狀態的 (JWT + Redis) ,
  `--scale backend=2` 已實測可行且 Flyway 不會衝突;但 compose 內建的 nginx upstream
  是固定的服務名,擴充副本時要自行確認負載分配.
- **沒有自動備份.** MariaDB 與 MinIO 的資料在 docker volume 裡,正式環境需自行安排
  `mariadb-dump` 與物件儲存的備份排程.
- **憑證更新需重啟 nginx.** 憑證是以 volume 掛載的,續期後執行
  `./stack.sh prod restart nginx` 讓 nginx 重新載入.

---

## 幾個容易踩到的設計點

**MinIO 必須用獨立埠,不能用路徑前綴**
presigned URL 的 SigV4 簽章涵蓋 Host 與完整路徑.如果讓 nginx 把 `/s3/xxx` 改寫成 `/xxx` 轉給 MinIO,簽章就對不上.因此 S3 走獨立的 `NGINX_S3_PORT`,nginx 原樣轉發並用 `$http_host` (保留埠號) 而非 `$host`.

**nginx 容器內外用同一個埠號**
`ports` 寫成 `"${NGINX_S3_PORT}:${NGINX_S3_PORT}"`,而不是固定的容器埠.
搭配 nginx 在 compose 網路上的 `aliases: [${PUBLIC_HOST}]`,對外網址 (含埠) 在容器內外完全一致 — 後端因此可以用同一個 URL 產生簽章並實際連線,不必繞回主機的已發佈埠 (主機防火牆通常會擋掉容器往 docker gateway 的連線) .

**SSR 的 /api 轉發寫在 `server/routes/api/[...].ts`,不是 `routeRules`**
`nitro.devProxy` 只在 `nuxt dev` 生效;`routeRules` 的 proxy 目標則在 build 當下就寫死進產出,同一個 image 無法換環境.改用 server route 在每次請求時讀 `NUXT_API_TARGET`,dev 與 production build 行為一致.
瀏覽器發出的 `/api` 請求由 nginx 直接轉給後端,不會經過這裡.

**時區:光設 `TZ` 不夠,映像要有時區資料庫**
`TZ=Asia/Taipei` 只有在映像內含 tzdata 時才有效,否則容器靜靜地停在 UTC.
實測各映像的狀況:

| 映像 | 內含 tzdata | 處理方式 |
| :--- | :--- | :--- |
| `nginx:1.27-alpine` / `redis:7-alpine` | 有 | 不需處理 (Alpine 官方映像有帶) |
| `eclipse-temurin` / `mariadb` | 有 | 不需處理 |
| `node:22-alpine` | **無** | Dockerfile 內 `apk add --no-cache tzdata` |
| `minio/minio` | **無** | compose 掛載 `/usr/share/zoneinfo:ro` |

前端這一項是實質 bug 而非美觀問題:頁面有 5 處用 `toLocaleString('zh-TW')` 格式化訂單/商品時間,
SSR 端算出 UTC,瀏覽器端算出當地時間,兩邊字串不同就會觸發 Vue 的 hydration mismatch.

MinIO 不能只掛 `/etc/localtime`:它是 Go 程式,`TZ` 有值時會去 zoneinfo 目錄查那個名稱,
查不到就退回 UTC,根本不會讀 `/etc/localtime` (已實測) .
所以要掛整個 `/usr/share/zoneinfo`.
註:S3 API 回傳的物件時間戳仍是 UTC,那是協定規定,不受此影響.

**Schema 由 Flyway 管理,`ddl-auto` 固定 `validate`**
`src/main/resources/db/migration/V1__init_schema.sql` 是 baseline.多個副本同時啟動時會競爭 `flyway_schema_history` 的鎖,只有一個實際套用 migration,其餘等待後看到"已是最新"直接通過.

改 entity 之後**必須**新增 migration (`V2__xxx.sql`) ,否則 `validate` 會讓應用啟動失敗.
單元測試不跑 Flyway (H2 用 `create-drop` 直接從 entity 產生 schema) .

> `carts.product_id` / `order_items.product_id` / `inventory_logs.product_id` 刻意沒有外鍵,
> 這是 entity 上 `@NotFound(action = IGNORE)` 造成的 (商品軟刪除後關聯需可為 null) ,
> 請勿在 migration 中補上.

---

## 健康檢查

| 端點 | 內容 |
| :--- | :--- |
| `/api/health` | 整體狀態 (含 db,redis) |
| `/api/health/liveness` | 只看行程是否活著,用於判斷該不該重啟容器 |
| `/api/health/readiness` | 含 db 與 redis,用於判斷能不能接流量 |

這三個端點在 `SecurityConfig` 中是 `permitAll`,但 `show-details: when-authorized`,
匿名請求只會看到 `{"status":"UP"}`,不會洩漏內部服務資訊.

compose 的 `backend` healthcheck 用的是 `readiness`,`frontend` 與 `nginx` 則
`depends_on ... condition: service_healthy`,因此 `./stack.sh up` 回來時整條鏈就已經可用 (前端除外,見疑難排解) .

---

## 疑難排解

**`./stack.sh up` 後前端 502**
Nuxt 容器首次啟動要跑 `pnpm install` 與首次編譯,通常幾十秒 (網路慢時較久) .
用 `./stack.sh logs frontend` 觀察,看到 `Local: http://0.0.0.0:3000/` 就是好了.
若持續 502 且 log 一直重複同樣訊息,那是容器在重啟迴圈,要看錯誤本身而不是繼續等.

**`ERR_PNPM_IGNORED_BUILDS`**
pnpm 10 預設封鎖相依套件的 build script.允許清單在 `frontend/pnpm-workspace.yaml`
的 `onlyBuiltDependencies`,新增需要 build 的套件時要一併加進去.

**後端啟動失敗,`Schema-validation` 錯誤**
entity 與 migration 不一致.新增對應的 `V{n}__xxx.sql`,或在開發階段用
`./stack.sh reset` 清掉資料 volume 重來.

**`target/` 出現 root 所有的檔案**
`env/.env.dev` 的 `DEV_UID` / `DEV_GID` 與你的 uid 不符.修正後
`./stack.sh build && ./stack.sh up`,並用下列指令修復既有檔案:

```bash
docker run --rm -v "$PWD:/w" alpine:3 chown -R $(id -u):$(id -g) /w/target
```

**埠號衝突**
所有對外埠號都在 `env/.env.*` 裡,直接改掉即可.
注意 `PUBLIC_S3_URL` 的埠必須與 `NGINX_S3_PORT` 一致.
