# Shop-Sys - B2C 電商購物平台

> 單一廠商 B2C 電商系統,前後端分離架構,提供完整購物流程與後台管理功能.

---

## 技術堆疊

| 類別 | 技術 | 版本 |
| :--- | :--- | :--- |
| 後端框架 | Spring Boot | 3.5.14 |
| 程式語言 | Java | 21 |
| 資料庫 | MariaDB | 11.4 |
| Schema 版控 | Flyway | - |
| 快取 / Token | Redis | 7 |
| 物件儲存 | MinIO | S3 相容 |
| 安全框架 | Spring Security + JWT | HS512 (access 30 分,refresh 7 天) |
| 密碼雜湊 | Argon2 | v5.8 defaults |
| ORM | Spring Data JPA (Hibernate) | - |
| 建置工具 | Maven Wrapper (`./mvnw`) | 3.9 |
| 前端框架 | Nuxt (Vue 3, SSR) | 4.x |
| 反向代理 | nginx (TLS 終結) | 1.27 |
| 容器編排 | Docker Compose | - |

---

## 開發環境

整套環境以 docker compose 啟動:nginx 作為統一入口,後方是 Spring Boot,Nuxt SSR,MariaDB,Redis 與 MinIO,前後端都支援 hot reload.

### 前置需求

只需要 `docker` 與 `docker compose`.
**不需要**在主機安裝 Java / Maven / Node,全部跑在容器內.

> 例外:後端的 hot reload 靠"主機端編譯"觸發,所以你仍會用到 `./mvnw`
> (Maven wrapper 會自動下載 Maven,主機只要有 JDK 21 即可) .

### 首次啟動

```bash
cd /home/zhu/project/shop-sys

# 1. 建立 env 檔
cp env/.env.dev.example env/.env.dev
chmod 600 env/.env.dev

# 2. 把檔案裡的 CHANGE_ME 全部換掉
openssl rand -base64 18 | tr -dc 'A-Za-z0-9'   # 給各組密碼
openssl rand -base64 48                        # 給 JWT_SECRET

# 3. 啟動
./stack.sh up
```

> 範本不附任何可用憑證,連開發環境也一樣.
> 因為除錯用埠 (13306 / 16379) 綁在 `0.0.0.0`,同網段或 tailnet 上的其他裝置
> 連得到你的 dev 資料庫,擋在前面的就只有這組密碼 —— 這種東西不該進版控.
>
> 若 `CHANGE_ME` 沒換完,`./stack.sh up` 會直接擋下並列出漏掉哪幾個變數.

`./stack.sh up` 等同於:

```bash
docker compose --env-file env/.env.dev -f compose.yaml -f compose.dev.yaml up -d
```

三個參數缺一不可,所以包成腳本避免打錯.第一次要 build image 並下載相依,約 **5-10 分鐘**.

> 若你習慣打 `make`,根目錄也有一份 `Makefile` (`make dev-up` 等) ,
> 它只是 `./stack.sh` 的薄包裝,行為完全一致.本專案不強制安裝 `make`.

指令返回時,`mariadb` / `redis` / `minio` / `backend` / `nginx` 都已通過健康檢查
(compose 的 `depends_on: condition: service_healthy` 會等) .
**只有 `frontend` 例外** — 它要先跑 `pnpm install` 與首次編譯,通常再等 **幾十秒**
(網路慢時較久) ,期間開首頁會是 502:

```bash
./stack.sh logs        # 看到 "Local: http://0.0.0.0:3000/" 就是前端好了
```

> `env/.env.dev` 的 `DEV_UID` / `DEV_GID` 預設 1000,必須與你的 uid 相符
> (用 `id -u` / `id -g` 確認) .不符的話後端容器寫進 `target/` 的檔案會變成 root 所有,
> 主機端的 `./mvnw` 隨即因權限不足而失敗.

### 確認啟動成功

```bash
./stack.sh ps          # 六個服務,除 frontend 外應皆為 (healthy)
```

```bash
H=tu-zhu.soay-fish.ts.net

curl https://$H:8443/api/health                                    # {"status":"UP",...}
curl https://$H:8443/api/v1/products                               # 商品列表 JSON
curl -o /dev/null -w "%{http_code}\n" https://$H:8443/              # 前端首頁 200
curl -o /dev/null -w "%{http_code}\n" https://$H:8444/minio/health/live   # S3 200
```

瀏覽器開 `https://tu-zhu.soay-fish.ts.net:8443/`.
TLS 由 nginx 終結,用的是主機上既有的 tailscale 憑證,不會跳憑證警告.

### 服務位址

| 位址 | 用途 |
| :--- | :--- |
| `https://tu-zhu.soay-fish.ts.net:8443/` | 前端 |
| `https://tu-zhu.soay-fish.ts.net:8443/api/v1/...` | 後端 API |
| `https://tu-zhu.soay-fish.ts.net:8443/api/swagger-ui/index.html` | Swagger |
| `https://tu-zhu.soay-fish.ts.net:8443/api/health` | 健康檢查 |
| `https://tu-zhu.soay-fish.ts.net:8444/` | MinIO S3 端點 |
| `https://tu-zhu.soay-fish.ts.net:8445/` | MinIO Console (帳密見 `env/.env.dev` 的 `MINIO_ROOT_*`) |
| `https://tu-zhu.soay-fish.ts.net:15540/` | RedisInsight (Redis 網頁管理介面) |
| `tu-zhu.soay-fish.ts.net:13306` | MariaDB (給 DBeaver 等資料庫工具) |
| `tu-zhu.soay-fish.ts.net:16379` | Redis (給 CLI 或其他工具) |

上面這些埠都綁在 `0.0.0.0`,所以**從 tailnet 上的其他電腦也連得進來**,不必待在這台機器前面.
也因為如此,**兩個管理網頁一律走 HTTPS**,不讓帳密與資料明文經過網路:

- RedisInsight 把憑證掛進容器,由它自己終結 TLS.
- MinIO Console 做不到這件事 (MinIO 的憑證是 server 層級,一開會連 S3 API 一起變 HTTPS,
  會弄壞 nginx 的上游) ,因此改為收在 nginx 後面,走 `NGINX_MINIO_CONSOLE_PORT` (8445) .
  對應的 nginx 設定只在 dev 掛載,staging / prod 不會對外開放 Console.

MariaDB 與 Redis 這兩個埠是原生協定,沒有 TLS,擋在前面的只有密碼.

> RedisInsight 首次開啟要手動新增一次連線:host `redis`,port `6379`,
> 密碼見 `env/.env.dev` 的 `REDIS_PASSWORD`.設定存在 volume 裡,之後不會再問.

埠號全部定義在 `env/.env.dev`,與其他服務衝突時直接改該檔即可
(注意 `PUBLIC_S3_URL` 的埠必須與 `NGINX_S3_PORT` 一致) .

### 測試帳號 (dev profile)

`DataInitializer` 只在 `dev` profile 下執行,偵測到資料庫為空時匯入測試資料:

| Email | 密碼 | 角色 |
| :--- | :--- | :--- |
| admin@test.com | admin123 | SUPER_ADMIN |
| manager@test.com | manager123 | PRODUCT_MANAGER |
| test01@example.com | mypassword123 | CUSTOMER |
| test02@example.com | mypassword123 | CUSTOMER |

同時建立 8 筆商品 (6 件上架,1 件下架,1 件缺貨) 及測試購物車資料.
資料已存在時自動跳過,不重複插入;要重置用 `./stack.sh reset`.

### 日常開發

**改後端** — 在主機端編譯,容器內的 `spring-boot-devtools` 監看到 `target/classes` 變動就會自動重啟 (實測約 4 秒) :

```bash
./mvnw compile       # 或讓 IDE 自動編譯
```

**改前端** — 直接存檔,Vite HMR 立即生效,不需要任何指令.

**改 entity** — 必須新增對應的 migration,否則 `ddl-auto: validate` 會讓後端啟動失敗:

```bash
# 新增 src/main/resources/db/migration/V2__add_xxx.sql
./mvnw compile       # 後端重啟時 Flyway 會自動套用
```

開發階段若不想寫 migration,可以 `./stack.sh reset` 清掉資料 volume 從頭來過.

**跑測試** — 測試用 H2 記憶體資料庫,不依賴任何容器:

```bash
./mvnw test
```

### 常用指令

```bash
./stack.sh --help      # 列出全部指令
./stack.sh up          # 啟動
./stack.sh down        # 停止並移除容器 (保留資料 volume)
./stack.sh reset       # 停止並刪除資料 volume,下次啟動會重建空資料庫
./stack.sh logs        # 追蹤全部服務的 log
./stack.sh logs backend    # 只看單一服務
./stack.sh ps          # 服務狀態與健康檢查結果
./stack.sh restart     # 只重啟後端容器
./stack.sh build       # 重新 build dev image
./stack.sh config      # 展開後的完整 compose 設定,用來確認變數有正確代入
./stack.sh exec mariadb sh # 進到容器內
```

第一個參數可指定環境 (`dev` / `staging` / `prod`,預設 `dev`) :

```bash
./stack.sh staging up
./stack.sh prod logs
```

### 從零重來

```bash
./stack.sh reset       # 停止並清空所有資料
rm env/.env.dev

cp env/.env.dev.example env/.env.dev
./stack.sh up
```

### 其他環境

`staging` 與 `prod` 使用 build 產出 (fat jar + Nuxt `.output`) ,不掛載原始碼,無 hot reload:

```bash
cp env/.env.staging.example env/.env.staging   # 把 CHANGE_ME 全部換掉
./stack.sh staging up
```

架構說明,環境差異,設計取捨與疑難排解見 **[docker/README.md](docker/README.md)**.

### 不使用 Docker

前置需求:Java 21+,Maven 3.8+,MariaDB 11.4+,Redis,MinIO.

連線資訊與機密一律由環境變數提供 (`application.yaml` 內不寫死任何密碼,
且機密類變數沒有預設值,缺少時會直接啟動失敗) :

```bash
export DB_URL=jdbc:mariadb://{host}:{port}/shop_sys
export DB_USERNAME={username}
export DB_PASSWORD={password}
export REDIS_HOST={host} REDIS_PASSWORD={password}
export JWT_SECRET=$(openssl rand -base64 48)
export MINIO_ACCESS_KEY={key} MINIO_SECRET_KEY={secret}

./mvnw spring-boot:run      # http://localhost:8088/api
```

資料表由 **Flyway** 建立 (`src/main/resources/db/migration/`) ,`ddl-auto` 固定為 `validate`.

---

## 系統架構

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

對外只發佈 nginx 的埠,MariaDB / Redis / MinIO 都留在內部網路
(開發環境另外開出除錯用埠號,見上方"服務位址") .

**Package 結構**:

```
com.zzowo.shop_sys/
├── config/        # Security 設定,全域例外處理,DataInitializer (dev 測試資料) 
├── controller/    # REST 控制層 (5 個 Controller) 
├── dto/           # Request / Response DTO
├── entity/        # JPA 實體 (11 個 Table) 
├── enums/         # Role, ProductStatus, OrderStatus
├── exception/     # 自定義例外
├── filter/        # JWT 認證過濾器
├── mapper/        # Entity ↔ DTO 轉換
├── repository/    # Spring Data JPA (9 個 Repository) 
├── service/       # 核心業務邏輯 (5 個 Service) 
└── util/          # JWT 工具類
```

---

## 功能模組

### 認證與安全

- JWT Stateless 認證 (HS512;access token 30 分鐘,refresh token 7 天,支援 Token Rotation) 
- Argon2 密碼雜湊,不可逆儲存
- 帳號鎖定:連續失敗 5 次,鎖定 15 分鐘 (自動解鎖) 
- RBAC 角色權限控制 (7 種角色) 

### 會員系統

- Email 註冊 / 登入
- 個人資料自助更新 (姓名,電話,Email,密碼) 
- 超級管理員可管理所有會員 (含停權,角色調整) 

### 商品系統

- 商品 CRUD (含多圖管理) 
- 商品狀態:上架 / 下架 / 缺貨
- 樂觀鎖 (`@Version`) 防止並發超賣
- 庫存異動稽核紀錄 (InventoryLog) 

### 購物與訂單

- 購物車:加入,查詢,移除 (即時庫存檢查) 
- 結帳:原子性事務保護
  - 驗證庫存 → 扣除庫存 → 建立庫存紀錄 → 建立訂單 → 清空購物車
- 訂單明細快照:記錄下單當下的價格,不受後續調價影響

---

## API 總覽

Base URL: `https://tu-zhu.soay-fish.ts.net:8443/api/v1` (Docker) 或 `http://localhost:8088/api/v1` (直接執行)  
認證方式: `Authorization: Bearer {token}`

### 認證

| 方法 | 路徑 | 說明 | 權限 |
| :--- | :--- | :--- | :--- |
| POST | `/v1/auth/register` | 會員註冊 | 公開 |
| POST | `/v1/auth/login` | 登入,回傳 Access + Refresh Token | 公開 |
| POST | `/v1/auth/refresh` | 換發新 Token (Token Rotation) | 公開 |
| POST | `/v1/auth/logout` | 登出 (黑名單 Access Token + 刪除 Refresh Token) | 已登入 |

### 商品

| 方法 | 路徑 | 說明 | 權限 |
| :--- | :--- | :--- | :--- |
| GET | `/v1/products` | 取得上架商品列表 (分頁 + 搜尋 + 排序)  | 公開 |
| GET | `/v1/products/{id}` | 取得商品詳情 | 公開 |
| POST | `/v1/products` | 新增商品 | PRODUCT_MANAGER, SUPER_ADMIN |
| PUT | `/v1/products/{id}` | 修改商品 | PRODUCT_MANAGER, SUPER_ADMIN |
| DELETE | `/v1/products/{id}` | 刪除商品 | PRODUCT_MANAGER, SUPER_ADMIN |
| GET | `/v1/products/inventory-logs` | 查詢所有庫存紀錄 | PRODUCT_MANAGER, SUPER_ADMIN |
| GET | `/v1/products/{id}/inventory-logs` | 查詢單一商品庫存紀錄 | PRODUCT_MANAGER, SUPER_ADMIN |

### 購物車

| 方法 | 路徑 | 說明 | 權限 |
| :--- | :--- | :--- | :--- |
| GET | `/v1/carts` | 查詢我的購物車 | 已登入 |
| POST | `/v1/carts` | 加入商品至購物車 | 已登入 |
| DELETE | `/v1/carts/{id}` | 移除購物車項目 | 已登入 |

### 訂單

| 方法 | 路徑 | 說明 | 權限 |
| :--- | :--- | :--- | :--- |
| POST | `/v1/orders` | 結帳 (建立訂單)  | 已登入 |
| GET | `/v1/orders` | 查詢我的訂單列表 | 已登入 |
| GET | `/v1/orders/{id}` | 查詢特定訂單詳情 | 已登入 |

### 會員

| 方法 | 路徑 | 說明 | 權限 |
| :--- | :--- | :--- | :--- |
| GET | `/v1/users/me` | 取得個人資料 | 已登入 |
| PUT | `/v1/users/me` | 更新個人資料 | 已登入 |
| GET | `/v1/users` | 取得所有會員列表 | SUPER_ADMIN |
| GET | `/v1/users/{id}` | 取得指定會員資料 | SUPER_ADMIN |
| PUT | `/v1/users/{id}` | 管理員更新指定會員 | SUPER_ADMIN |

---

## 資料庫設計

| 資料表 | 說明 | 關聯 |
| :--- | :--- | :--- |
| `users` | 會員帳號,角色,狀態 | 1→N orders, 1→N carts |
| `products` | 商品資訊,庫存,狀態 | 1→N product_images, 1→N inventory_logs |
| `product_images` | 商品多圖 | N→1 products |
| `carts` | 購物車暫存 | N→1 users, N→1 products |
| `orders` | 訂單主檔 | N→1 users, 1→N order_items |
| `order_items` | 訂單明細 (含快照價格) | N→1 orders, N→1 products |
| `inventory_logs` | 庫存異動稽核紀錄 | N→1 products |
| `payments` | 付款紀錄 (待整合) | 1→1 orders |
| `shipments` | 物流紀錄 (待整合) | 1→1 orders |
| `coupons` | 優惠券 (預留) | - |
| `reviews` | 商品評價 (預留) | - |

---

## 角色說明

| 角色 | 代碼 | 說明 |
| :--- | :--- | :--- |
| 一般消費者 | `CUSTOMER` | 預設角色,可購物下訂單 |
| 商品管理員 | `PRODUCT_MANAGER` | 管理商品上下架與庫存 |
| 訂單管理員 | `ORDER_MANAGER` | 預留 |
| 客服人員 | `CUSTOMER_SERVICE` | 預留 |
| 行銷人員 | `MARKETING` | 預留 |
| 財務人員 | `FINANCE` | 預留 |
| 超級管理員 | `SUPER_ADMIN` | 擁有全部權限,管理會員 |

---

## 詳細規格

完整的 API 請求/回應範例,業務規則,資料欄位定義,錯誤處理規格,請參閱 [SPEC.md](./SPEC.md).
