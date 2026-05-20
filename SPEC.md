# Shop-Sys 系統規格書 (System Specification)

> **版本**: 1.1.0  
> **最後更新**: 2026-05-10  
> **狀態**: 開發中 (In Development)

---

## 目錄

1. [系統概覽](#1-系統概覽)
2. [技術架構](#2-技術架構)
3. [資料庫設計](#3-資料庫設計)
4. [業務規則](#4-業務規則)
5. [API 規格](#5-api-規格)
6. [安全機制](#6-安全機制)
7. [錯誤處理](#7-錯誤處理)
8. [角色與權限](#8-角色與權限)
9. [保留功能與擴充規劃](#9-保留功能與擴充規劃)

---

## 1. 系統概覽

### 1.1 專案目的

Shop-Sys 是一套 **單一廠商 B2C 電商平台 (Single-Vendor B2C E-Commerce Platform)**,定位為"一個賣家對多位消費者"的線上購物系統.系統提供完整的購物流程 (商品瀏覽,加入購物車,結帳) 以及後台管理功能 (商品管理,庫存追蹤,會員管理) .

### 1.2 核心目標

| 目標 | 說明 |
| :--- | :--- |
| **前後端分離** | 後端僅提供 RESTful JSON API,不負責渲染畫面 |
| **無狀態認證** | 使用 JWT Token 實現 Stateless 架構,可水平擴展 |
| **資料一致性** | 結帳流程全程使用資料庫事務 (Transaction) 保護 |
| **庫存安全** | 樂觀鎖 (Optimistic Lock) 防止高並發超賣 |
| **稽核可追蹤** | 所有庫存異動均記錄原因與操作者,留有完整紀錄 |

### 1.3 系統邊界

```
[前端 Vue 3 SPA] ←── HTTP/HTTPS ──→ [Spring Boot REST API :8088/api]
                                              │
                                    [MariaDB tu-zhu.soay-fish.ts.net:3306]
```

---

## 2. 技術架構

### 2.1 後端技術棧

| 類別 | 技術 | 版本 |
| :--- | :--- | :--- |
| 核心框架 | Spring Boot | 3.5.13 |
| 程式語言 | Java | 21 |
| 資料庫 | MariaDB | 11.8 |
| ORM | Spring Data JPA (Hibernate) | - |
| 安全框架 | Spring Security | - |
| JWT 函式庫 | jjwt-api | 0.12.6 |
| 密碼雜湊 | Argon2 (via Spring Security) | v5.8 defaults |
| 建置工具 | Maven | 3.8.x |
| 程式碼生成 | Lombok | - |
| 監控 | Spring Boot Actuator | - |

### 2.2 前端技術棧

| 類別 | 技術 |
| :--- | :--- |
| 核心框架 | Vue 3 (Composition API) |
| 架構模式 | SPA (Single Page Application) |

### 2.3 服務設定

| 設定項目 | 值 |
| :--- | :--- |
| 伺服器 Port | `8088` |
| API Context Path | `/api` |
| API 版本前綴 | `/v1` |
| 完整 Base URL | `http://{host}:8088/api/v1` |
| JPA DDL 模式 | `update` (自動維護 Schema) |
| JWT 有效期限 | 86400000 ms (24 小時) |
| JWT 演算法 | HS512 (HMAC-SHA512) |
| 預設 Spring Profile | `dev` (於 `application.yaml` 設定) |

### 2.4 專案套件結構

```
com.zzowo.shop_sys/
├── config/
│   ├── SecurityConfig.java          # Spring Security 設定 + 端點權限規則
│   ├── GlobalExceptionHandler.java  # 全域例外攔截器
│   └── DataInitializer.java         # dev profile 專用測試資料初始化 (首次啟動時自動執行) 
├── controller/
│   ├── AuthController.java          # /v1/auth
│   ├── ProductController.java       # /v1/products
│   ├── CartController.java          # /v1/carts
│   ├── OrderController.java         # /v1/orders
│   └── UserController.java          # /v1/users
├── dto/
│   ├── request/                     # 接收前端輸入的 DTO
│   │   ├── cart/AddToCartRequest.java
│   │   ├── order/OrderCreateRequest.java
│   │   ├── product/ProductRequest.java
│   │   └── user/
│   │       ├── UserRegisterRequest.java
│   │       ├── UserLoginRequest.java
│   │       ├── UserSelfUpdateRequest.java
│   │       └── AdminUpdateUserRequest.java
│   └── response/                    # 回傳給前端的 DTO
│       ├── ApiResponse.java          # 統一回應封裝
│       ├── PageResponse.java         # 分頁回應封裝 (content, page, size, totalElements, totalPages, last)
│       ├── cart/CartItemResponse.java
│       ├── order/
│       │   ├── OrderResponse.java
│       │   └── OrderItemResponse.java
│       ├── product/
│       │   ├── ProductResponse.java
│       │   └── InventoryLogResponse.java
│       └── user/
│           ├── UserResponse.java
│           └── RegisterResponse.java
├── entity/                          # JPA 資料庫實體
│   ├── User.java
│   ├── Product.java
│   ├── ProductImage.java
│   ├── Cart.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── InventoryLog.java
│   ├── Payment.java
│   ├── Shipment.java
│   ├── Coupon.java                  # 預留
│   └── Review.java                  # 預留
├── enums/
│   ├── Role.java
│   ├── ProductStatus.java
│   └── OrderStatus.java
├── exception/
│   ├── BusinessException.java
│   └── ResourceNotFoundException.java
├── filter/
│   └── JwtAuthenticationFilter.java
├── mapper/                          # Entity ↔ DTO 轉換
│   ├── ProductMapper.java
│   ├── UserMapper.java
│   ├── CartMapper.java
│   ├── OrderMapper.java
│   └── InventoryLogMapper.java
├── repository/                      # Spring Data JPA Repositories
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── CartRepository.java
│   ├── OrderRepository.java
│   ├── InventoryLogRepository.java
│   ├── PaymentRepository.java
│   ├── ShipmentRepository.java
│   ├── CouponRepository.java
│   └── ReviewRepository.java
├── service/
│   ├── UserService.java
│   ├── ProductService.java
│   ├── CartService.java
│   ├── OrderService.java
│   └── CustomUserDetailsService.java
├── util/
│   └── JwtUtil.java
└── ShopSysApplication.java
```

### 2.5 開發環境測試資料

`application.yaml` 預設啟用 `dev` profile,應用程式啟動時 `DataInitializer` 會自動執行.若資料庫已有資料則跳過,確保冪等性 (idempotent) .

#### 預設測試帳號

| Email | 密碼 | 角色 | 說明 |
| :--- | :--- | :--- | :--- |
| admin@test.com | admin123 | `SUPER_ADMIN` | 可存取所有功能 |
| manager@test.com | manager123 | `PRODUCT_MANAGER` | 可管理商品與庫存 |
| test01@example.com | mypassword123 | `CUSTOMER` | 測試員小明,購物車有預設商品 |
| test02@example.com | mypassword123 | `CUSTOMER` | 測試員小王 |

#### 預設測試商品

| 商品名稱 | 價格 | 庫存 | 狀態 |
| :--- | ---: | ---: | :--- |
| 藍芽耳機 Pro | 1,990 | 50 | `ON_SHELF` |
| 無線靜音滑鼠 | 890 | 120 | `ON_SHELF` |
| 機械鍵盤 RGB | 2,490 | 30 | `ON_SHELF` |
| 智慧手錶 S3 | 5,990 | 15 | `ON_SHELF` |
| USB-C 七合一 Hub | 1,290 | 200 | `ON_SHELF` |
| 可攜式藍芽音響 | 1,590 | 45 | `ON_SHELF` |
| 舊款有線耳機 | 490 | 0 | `OFF_SHELF` |
| 限量版電競滑鼠 | 3,290 | 0 | `OUT_OF_STOCK` |

#### 重置測試資料

```sql
DELETE FROM carts;
DELETE FROM products;
DELETE FROM users;
```

重啟服務後 `DataInitializer` 將重新匯入.

---

## 3. 資料庫設計

### 3.1 實體關聯圖 (ERD 文字描述)

```
users (1) ──────────── (N) carts
users (1) ──────────── (N) orders
users (1) ──────────── (N) reviews        [預留]

products (1) ─────────── (N) product_images
products (1) ─────────── (N) carts
products (1) ─────────── (N) order_items
products (1) ─────────── (N) inventory_logs
products (1) ─────────── (N) reviews      [預留]

orders (1) ─────────── (N) order_items
orders (1) ─────────── (1) payments
orders (1) ─────────── (1) shipments
orders (N) ─────────── (1) coupons        [預留]
```

### 3.2 資料表詳細規格

#### **users**

| 欄位 | 類型 | 限制 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| `email` | VARCHAR | NOT NULL, UNIQUE | 登入帳號 |
| `password_hash` | VARCHAR | NOT NULL | Argon2 雜湊後的密碼 |
| `name` | VARCHAR | NOT NULL | 顯示名稱 |
| `phone` | VARCHAR | NULLABLE | 手機號碼 |
| `role` | VARCHAR (ENUM String) | NOT NULL | 角色 (見角色列表) |
| `enabled` | BOOLEAN | NOT NULL | 帳號是否啟用 |
| `account_non_locked` | BOOLEAN | NOT NULL | 帳號是否未被鎖定 |
| `account_non_expired` | BOOLEAN | NOT NULL | 帳號是否未過期 |
| `credentials_non_expired` | BOOLEAN | NOT NULL | 憑證是否未過期 |
| `last_login_at` | DATETIME | NULLABLE | 最後登入時間 |
| `last_password_change_at` | DATETIME | NULLABLE | 最後修改密碼時間 |
| `deleted_at` | DATETIME | NULLABLE | 軟刪除時間戳 (預留) |
| `created_at` | DATETIME | NOT NULL | 建立時間 |
| `updated_at` | DATETIME | NOT NULL | 更新時間 |

#### **products**

| 欄位 | 類型 | 限制 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| `name` | VARCHAR | NOT NULL | 商品名稱 |
| `description` | VARCHAR(65535) | NULLABLE | 商品描述 |
| `price` | DECIMAL(10,2) | NOT NULL | 售價 |
| `stock_quantity` | INT | NOT NULL | 目前庫存量 |
| `status` | VARCHAR (ENUM String) | NOT NULL | 商品狀態 |
| `cover_image_url` | VARCHAR | NULLABLE | 封面圖片 URL |
| `version` | BIGINT | NOT NULL | JPA 樂觀鎖版本號 |
| `created_at` | DATETIME | NOT NULL | 建立時間 |
| `updated_at` | DATETIME | NOT NULL | 更新時間 |

#### **product_images**

| 欄位 | 類型 | 限制 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| `product_id` | BIGINT | FK → products.id | 所屬商品 |
| `image_url` | VARCHAR | NOT NULL | 圖片 URL |
| `sort_order` | INT | NOT NULL, DEFAULT 0 | 排列順序 |

#### **carts**

| 欄位 | 類型 | 限制 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| `user_id` | BIGINT | FK → users.id | 購物車所屬會員 |
| `product_id` | BIGINT | FK → products.id | 加入的商品 |
| `quantity` | INT | NOT NULL, DEFAULT 1 | 數量 |
| `created_at` | DATETIME | NOT NULL | 建立時間 |
| `updated_at` | DATETIME | NOT NULL | 更新時間 |

#### **orders**

| 欄位 | 類型 | 限制 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| `user_id` | BIGINT | FK → users.id | 下訂會員 |
| `coupon_id` | BIGINT | FK → coupons.id, NULLABLE | 使用優惠券 (預留) |
| `status` | VARCHAR (ENUM String) | NOT NULL | 訂單狀態 |
| `total_amount` | DECIMAL(10,2) | NOT NULL | 訂單總金額 |
| `recipient_name` | VARCHAR | NOT NULL | 收件人姓名 |
| `recipient_phone` | VARCHAR | NOT NULL | 收件人電話 |
| `recipient_address` | VARCHAR | NOT NULL | 收件地址 |
| `created_at` | DATETIME | NOT NULL | 建立時間 |

#### **order_items**

| 欄位 | 類型 | 限制 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| `order_id` | BIGINT | FK → orders.id | 所屬訂單 |
| `product_id` | BIGINT | FK → products.id | 所購商品 |
| `price_at_purchase` | DECIMAL(10,2) | NOT NULL | **下單當下的快照價格** |
| `quantity` | INT | NOT NULL | 購買數量 |

#### **inventory_logs**

| 欄位 | 類型 | 限制 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| `product_id` | BIGINT | FK → products.id | 異動商品 |
| `change_amount` | INT | NOT NULL | 異動量 (正=增加,負=減少) |
| `reason` | VARCHAR | NOT NULL | 異動原因 (ORDER / RESTOCK / ADJUSTMENT / CANCEL / RETURN) |
| `operator_id` | BIGINT | NULLABLE | 操作者 ID |
| `created_at` | DATETIME | NOT NULL | 記錄時間 |

#### **payments** (已建立 Entity,API 尚未完全整合)

| 欄位 | 類型 | 說明 |
| :--- | :--- | :--- |
| `id` | BIGINT | 主鍵 |
| `order_id` | BIGINT | FK → orders.id (1對1) |
| `payment_method` | VARCHAR | 付款方式 |
| `transaction_id` | VARCHAR | 金流交易 ID |
| `amount` | DECIMAL(10,2) | 實際付款金額 |
| `status` | VARCHAR | 付款狀態 |
| `paid_at` | DATETIME | 付款時間 |

#### **shipments** (已建立 Entity,API 尚未完全整合)

| 欄位 | 類型 | 說明 |
| :--- | :--- | :--- |
| `id` | BIGINT | 主鍵 |
| `order_id` | BIGINT | FK → orders.id (1對1) |
| `tracking_number` | VARCHAR | 物流追蹤號碼 |
| `logistics_provider` | VARCHAR | 物流商名稱 |
| `status` | VARCHAR | 物流狀態 |
| `shipped_at` | DATETIME | 出貨時間 |
| `delivered_at` | DATETIME | 到貨時間 |

### 3.3 列舉值定義

#### Role (使用者角色)

| 值 | 說明 |
| :--- | :--- |
| `CUSTOMER` | 一般消費者 (預設角色) |
| `PRODUCT_MANAGER` | 商品管理員 (可管理商品與庫存) |
| `ORDER_MANAGER` | 訂單管理員 |
| `CUSTOMER_SERVICE` | 客服人員 |
| `MARKETING` | 行銷人員 |
| `FINANCE` | 財務人員 |
| `SUPER_ADMIN` | 超級管理員 (擁有所有權限) |

#### ProductStatus (商品狀態)

| 值 | 說明 |
| :--- | :--- |
| `ON_SHELF` | 上架中 (顧客可見) |
| `OFF_SHELF` | 已下架 (後台保留) |
| `OUT_OF_STOCK` | 缺貨中 |

#### OrderStatus (訂單狀態)

| 值 | 說明 |
| :--- | :--- |
| `PENDING` | 待付款 |
| `PAID` | 已付款 |
| `SHIPPED` | 已出貨 |
| `COMPLETED` | 已完成 |
| `CANCELLED` | 已取消 |

---

## 4. 業務規則

### 4.1 使用者註冊與登入

- 每個 Email 在系統中唯一,重複使用時拋出 `BusinessException`.
- 密碼以 Argon2 演算法 (Spring Security v5.8 預設參數) 雜湊後儲存,不可逆.
- 所有新注冊帳號預設角色為 `CUSTOMER`,`enabled = true`.
- 登入成功後更新 `last_login_at`,並回傳 JWT Token (有效 24 小時) .
- 登入失敗連續 5 次,帳號鎖定 15 分鐘 (設定驅動,`auto-unlock: true`) .

### 4.2 商品管理

- 商品狀態為 `ON_SHELF` 才可被一般顧客透過公開 API 查詢到.
- 商品刪除 (`DELETE /v1/products/{id}`) 為**直接刪除**,非軟刪除.
- 每次商品更新時,images 列表為**全量替換** (先刪後建) .
- 商品使用 JPA `@Version` 樂觀鎖,防止並發修改時覆蓋資料.

### 4.3 購物車

- 若使用者已將同一商品加入購物車,再次加入時**累加數量**,而非新增一筆.
- 加入購物車時即時檢查商品庫存:若加入數量超過剩餘庫存,拋出 `BusinessException`.
- 移除購物車項目時驗證所有權,僅可移除自己的項目.

### 4.4 結帳 (建立訂單) 

結帳是系統最核心的業務流程,以 `@Transactional` 保護整個操作的原子性:

```
1. 確認使用者存在
2. 確認購物車非空
3. 遍歷購物車:逐一確認每項商品庫存是否充足
4. 逐一扣除商品 stockQuantity
5. 為每項商品建立 InventoryLog (reason = "ORDER") 
6. 建立 OrderItem (快照 priceAtPurchase = 商品當下售價) 
7. 計算 totalAmount = Σ(priceAtPurchase × quantity)
8. 建立 Order (初始狀態 PENDING) 
9. 清空使用者購物車
```

若任一步驟失敗,整個交易回滾 (Rollback).

### 4.5 訂單查詢

- 一般顧客只能查詢**自己的**訂單,查詢他人訂單時拋出 `BusinessException`.
- 訂單列表依 `created_at` 降序排列 (最新的在最前) .

### 4.6 庫存紀錄

庫存異動原因 (`reason`) 說明:

| reason | 觸發時機 |
| :--- | :--- |
| `ORDER` | 顧客結帳,系統自動扣減 |
| `RESTOCK` | 後台補貨,庫存增加 |
| `ADJUSTMENT` | 手動庫存盤點調整 |
| `CANCEL` | 訂單取消,庫存歸還 |
| `RETURN` | 退貨,庫存歸還 |

---

## 5. API 規格

### 5.1 通用規範

- **Base URL**: `http://{host}:8088/api/v1`
- **Content-Type**: `application/json`
- **認證方式**: `Authorization: Bearer {jwt_token}`
- **統一回應格式**:

```json
{
  "success": true,
  "message": "操作成功",
  "data": { ... }
}
```

錯誤回應:

```json
{
  "success": false,
  "message": "錯誤原因描述",
  "data": null
}
```

---

### 5.2 認證 (Authentication) - `/v1/auth`

#### POST `/v1/auth/register` - 會員註冊

**權限**: 公開  
**Request Body**:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "王小明",
  "phone": "0912345678"
}
```

| 欄位 | 必填 | 驗證規則 |
| :--- | :---: | :--- |
| `email` | ✅ | 合法 Email 格式 |
| `password` | ✅ | 最少 8 個字元 |
| `name` | ✅ | 不可空白 |
| `phone` | ❌ | - |

**Response** `201 Created`:

```json
{
  "success": true,
  "message": "註冊成功",
  "data": {
    "email": "user@example.com",
    "name": "王小明",
    "role": "CUSTOMER",
    "phone": "0912345678",
    "createdAt": 1746748800000
  }
}
```

---

#### POST `/v1/auth/login` - 會員登入

**權限**: 公開  
**Request Body**:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** `200 OK`:

```json
{
  "success": true,
  "message": "登入成功",
  "data": "eyJhbGciOiJIUzUxMiJ9..."
}
```

> `data` 欄位直接回傳 JWT Token 字串.

---

### 5.3 商品 (Products) - `/v1/products`

#### GET `/v1/products` - 取得上架商品列表 (分頁) 

**權限**: 公開

**Query Parameters**:

| 參數 | 類型 | 必填 | 預設值 | 說明 |
| :--- | :--- | :---: | :--- | :--- |
| `page` | Integer | ❌ | `0` | 頁碼,從 0 開始 |
| `size` | Integer | ❌ | `20` | 每頁筆數,上限 100 |
| `sort` | String | ❌ | `createdAt,desc` | 排序欄位與方向,格式:`欄位,asc\|desc` |
| `keyword` | String | ❌ | - | 商品名稱關鍵字模糊搜尋 |

**可排序欄位**: `name`,`price`,`createdAt`

**範例請求**:
```
GET /api/v1/products
GET /api/v1/products?page=1&size=10
GET /api/v1/products?sort=price,asc
GET /api/v1/products?keyword=耳機&sort=price,asc&size=10
```

**Response** `200 OK`:

```json
{
  "success": true,
  "message": "取得商品列表成功",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "藍芽耳機 Pro",
        "description": "高音質無線耳機",
        "price": 1990.00,
        "stockQuantity": 50,
        "coverImageUrl": "https://cdn.example.com/img/001.jpg",
        "imageUrls": null,
        "status": "ON_SHELF"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "last": false
  }
}
```

> `imageUrls` 在列表頁為 `null`,詳情頁 (`GET /v1/products/{id}`) 才回傳完整圖片列表.

---

#### GET `/v1/products/{id}` - 取得商品詳情

**權限**: 公開  
**Path Variable**: `id` (商品 ID)  
**Response** `200 OK`: 同上,但 `imageUrls` 包含完整圖片列表.

---

#### POST `/v1/products` - 新增商品

**權限**: `PRODUCT_MANAGER`, `SUPER_ADMIN`  
**Request Body**:

```json
{
  "name": "藍芽耳機 Pro",
  "description": "高音質無線耳機",
  "price": 1990.00,
  "stockQuantity": 50,
  "status": "ON_SHELF",
  "coverImageUrl": "https://cdn.example.com/img/cover.jpg",
  "imageUrls": [
    "https://cdn.example.com/img/001.jpg",
    "https://cdn.example.com/img/002.jpg"
  ]
}
```

| 欄位 | 必填 | 驗證規則 |
| :--- | :---: | :--- |
| `name` | ✅ | 不可空白 |
| `price` | ✅ | 不可為 null,最小值 0 |
| `stockQuantity` | ✅ | 不可為 null,最小值 0 |
| `status` | ✅ | 合法的 ProductStatus 值 |
| `description` | ❌ | - |
| `coverImageUrl` | ❌ | - |
| `imageUrls` | ❌ | - |

**Response** `200 OK`: 回傳建立後的 `ProductResponse`

---

#### PUT `/v1/products/{id}` - 修改商品

**權限**: `PRODUCT_MANAGER`, `SUPER_ADMIN`  
**Request Body**: 同 POST,欄位全量更新 (images 為全量替換)   
**Response** `200 OK`: 回傳更新後的 `ProductResponse`

---

#### DELETE `/v1/products/{id}` - 刪除商品

**權限**: `PRODUCT_MANAGER`, `SUPER_ADMIN`  
**Response** `200 OK`:

```json
{
  "success": true,
  "message": "商品已刪除",
  "data": null
}
```

---

#### GET `/v1/products/{id}/inventory-logs` - 查詢單一商品庫存紀錄

**權限**: `PRODUCT_MANAGER`, `SUPER_ADMIN`  
**Response** `200 OK`:

```json
{
  "success": true,
  "message": "查詢成功",
  "data": [
    {
      "id": 5,
      "changeAmount": -2,
      "reason": "ORDER",
      "operatorId": null,
      "createdAt": 1746748900000,
      "productId": 1,
      "productName": "藍芽耳機 Pro"
    }
  ]
}
```

---

#### GET `/v1/products/inventory-logs` - 查詢所有庫存紀錄

**權限**: `PRODUCT_MANAGER`, `SUPER_ADMIN`  
**Response** `200 OK`: 同上,為全系統所有商品的庫存異動列表,依 `created_at` 降序.

---

### 5.4 購物車 (Cart) - `/v1/carts`

> 所有購物車 API 均需 JWT 驗證.

#### GET `/v1/carts` - 查詢我的購物車

**Response** `200 OK`:

```json
{
  "success": true,
  "message": "查詢成功",
  "data": [
    {
      "id": 10,
      "productId": 1,
      "productName": "藍芽耳機 Pro",
      "coverImageUrl": "https://cdn.example.com/img/cover.jpg",
      "price": 1990.00,
      "quantity": 2,
      "subtotal": 3980.00
    }
  ]
}
```

---

#### POST `/v1/carts` - 加入商品至購物車

**Request Body**:

```json
{
  "productId": 1,
  "quantity": 2
}
```

| 欄位 | 必填 | 驗證規則 |
| :--- | :---: | :--- |
| `productId` | ✅ | 不可為 null |
| `quantity` | ✅ | 最小值 1 |

**Response** `200 OK`: 回傳 `CartItemResponse`

---

#### DELETE `/v1/carts/{id}` - 移除購物車項目

**Path Variable**: `id` (Cart 項目 ID)  
**Response** `200 OK`:

```json
{
  "success": true,
  "message": "已移除",
  "data": null
}
```

---

### 5.5 訂單 (Orders) - `/v1/orders`

> 所有訂單 API 均需 JWT 驗證.

#### POST `/v1/orders` - 結帳 (建立訂單) 

**Request Body**:

```json
{
  "recipientName": "王小明",
  "recipientPhone": "0912345678",
  "recipientAddress": "台北市信義區信義路五段7號"
}
```

| 欄位 | 必填 | 驗證規則 |
| :--- | :---: | :--- |
| `recipientName` | ✅ | 不可空白 |
| `recipientPhone` | ✅ | 不可空白 |
| `recipientAddress` | ✅ | 不可空白 |

**Response** `200 OK`:

```json
{
  "success": true,
  "message": "訂單建立成功",
  "data": {
    "id": 20,
    "status": "PENDING",
    "totalAmount": 3980.00,
    "recipientName": "王小明",
    "recipientPhone": "0912345678",
    "recipientAddress": "台北市信義區信義路五段7號",
    "createdAt": 1746749000000,
    "items": [
      {
        "productId": 1,
        "productName": "藍芽耳機 Pro",
        "priceAtPurchase": 1990.00,
        "quantity": 2,
        "subtotal": 3980.00
      }
    ]
  }
}
```

---

#### GET `/v1/orders` - 查詢我的訂單列表

**Response** `200 OK`: 回傳 `List<OrderResponse>`,依建立時間降序排列.

---

#### GET `/v1/orders/{id}` - 查詢特定訂單詳情

**Path Variable**: `id` (Order ID)  
**Response** `200 OK`: 回傳單一 `OrderResponse` (含訂單明細) .

---

### 5.6 會員 (Users) - `/v1/users`

#### GET `/v1/users/me` - 取得個人資料

**權限**: 已登入任何角色  
**Response** `200 OK`:

```json
{
  "success": true,
  "message": "查詢成功",
  "data": {
    "email": "user@example.com",
    "name": "王小明",
    "role": "CUSTOMER",
    "phone": "0912345678",
    "createdAt": 1746748800000,
    "lastLoginAt": 1746749100000,
    "lastPasswordChangeAt": null
  }
}
```

---

#### PUT `/v1/users/me` - 更新個人資料

**權限**: 已登入任何角色  
**Request Body** (所有欄位均為選填,只更新提供的欄位):

```json
{
  "email": "newemail@example.com",
  "password": "newpassword123",
  "name": "新名字",
  "phone": "0987654321"
}
```

**Response** `200 OK`: 回傳更新後的 `UserResponse`

---

#### GET `/v1/users` - 取得所有會員列表

**權限**: `SUPER_ADMIN`  
**Response** `200 OK`: 回傳 `List<UserResponse>`

---

#### GET `/v1/users/{id}` - 取得指定會員資料

**權限**: `SUPER_ADMIN`  
**Response** `200 OK`: 回傳 `UserResponse`

---

#### PUT `/v1/users/{id}` - 管理員更新指定會員

**權限**: `SUPER_ADMIN`  
**Request Body** (所有欄位均為選填):

```json
{
  "email": "user@example.com",
  "password": "newpassword123",
  "name": "王大明",
  "phone": "0912345678",
  "role": "PRODUCT_MANAGER",
  "enabled": false
}
```

**Response** `200 OK`: 回傳更新後的 `UserResponse`

---

## 6. 安全機制

### 6.1 認證流程

```
前端發送 POST /v1/auth/login
    ↓
AuthController → UserService.login()
    ↓
比對 Argon2 密碼 Hash
    ↓
成功 → JwtUtil.generateToken(email)
    ↓
回傳 JWT Token (HS512,有效 24 小時) 

後續每次請求:
Authorization: Bearer {token}
    ↓
JwtAuthenticationFilter 攔截
    ↓
解析 email → 驗證簽名 + 過期時間
    ↓
設定 Spring SecurityContext
    ↓
Controller 執行業務邏輯
```

### 6.2 密碼安全

- 使用 **Argon2** (Spring Security v5.8 預設參數):記憶體硬化雜湊,抵抗暴力破解.
- 密碼明文**永不儲存**,系統中僅存 Hash 值.
- 密碼最小長度 8 碼 (由 DTO 驗證層保障) .

### 6.3 帳號鎖定機制

| 設定 | 值 |
| :--- | :--- |
| 最大失敗次數 | 5 次 |
| 鎖定時長 | 15 分鐘 |
| 自動解鎖 | 是 |

### 6.4 JWT 設定

| 設定 | 值 |
| :--- | :--- |
| 演算法 | HS512 (HMAC-SHA512) |
| 有效期限 | 86400000 ms (24 小時) |
| Payload | email (subject) |

### 6.5 公開 vs 受保護端點

| 端點模式 | 存取層級 |
| :--- | :--- |
| `POST /v1/auth/**` | 公開 |
| `GET /v1/products/**` | 公開 |
| `POST/PUT/DELETE /v1/products/**` | PRODUCT_MANAGER, SUPER_ADMIN |
| `GET /v1/products/*/inventory-logs` | PRODUCT_MANAGER, SUPER_ADMIN |
| `GET /v1/products/inventory-logs` | PRODUCT_MANAGER, SUPER_ADMIN |
| `GET /v1/users`, `GET/PUT /v1/users/{id}` | SUPER_ADMIN |
| 其餘所有端點 | 已登入任何角色 |

---

## 7. 錯誤處理

### 7.1 HTTP 狀態碼對照

| HTTP Code | 觸發情境 | 範例 |
| :--- | :--- | :--- |
| `200 OK` | 所有成功操作 | - |
| `201 Created` |  (目前未使用,統一用 200) | - |
| `400 Bad Request` | 業務邏輯錯誤 | 庫存不足,Email 重複 |
| `400 Bad Request` | 請求格式/驗證錯誤 | 必填欄位空白,格式錯誤 |
| `401 Unauthorized` | 未提供或無效 JWT Token | - |
| `403 Forbidden` | 角色權限不足 | CUSTOMER 存取管理端點 |
| `404 Not Found` | 資源不存在 | 商品 ID 不存在 |
| `500 Internal Server Error` | 未預期的系統錯誤 | - |

### 7.2 錯誤回應格式

```json
{
  "success": false,
  "message": "商品庫存不足",
  "data": null
}
```

### 7.3 自定義例外類別

| 類別 | 用途 | 對應 HTTP Code |
| :--- | :--- | :--- |
| `BusinessException` | 業務邏輯錯誤 (庫存不足,Email 重複等) | 400 |
| `ResourceNotFoundException` | 資源查無 (商品/訂單/使用者不存在) | 404 |

---

## 8. 角色與權限

### 8.1 角色總覽

| 角色 | 代碼 | 主要用途 |
| :--- | :--- | :--- |
| 一般消費者 | `CUSTOMER` | 瀏覽商品,加入購物車,下訂單 |
| 商品管理員 | `PRODUCT_MANAGER` | 管理商品上下架,查詢庫存紀錄 |
| 訂單管理員 | `ORDER_MANAGER` |  (預留) 管理訂單狀態 |
| 客服人員 | `CUSTOMER_SERVICE` |  (預留) 處理客訴 |
| 行銷人員 | `MARKETING` |  (預留) 管理優惠券 |
| 財務人員 | `FINANCE` |  (預留) 查詢金流資料 |
| 超級管理員 | `SUPER_ADMIN` | 擁有所有權限,管理會員 |

### 8.2 功能權限矩陣

| 功能 | CUSTOMER | PRODUCT_MANAGER | SUPER_ADMIN |
| :--- | :---: | :---: | :---: |
| 瀏覽商品 (上架中) | ✅ | ✅ | ✅ |
| 新增/修改/刪除商品 | ❌ | ✅ | ✅ |
| 查詢庫存紀錄 | ❌ | ✅ | ✅ |
| 購物車操作 | ✅ | ✅ | ✅ |
| 建立訂單 | ✅ | ✅ | ✅ |
| 查詢自己的訂單 | ✅ | ✅ | ✅ |
| 查詢個人資料 | ✅ | ✅ | ✅ |
| 更新個人資料 | ✅ | ✅ | ✅ |
| 查詢所有會員 | ❌ | ❌ | ✅ |
| 管理員更新任意會員 | ❌ | ❌ | ✅ |

---

## 9. 保留功能與擴充規劃

### 9.1 已建立 Entity 但尚未整合 API 的功能

| 功能 | Entity | 狀態 |
| :--- | :--- | :--- |
| 金流/付款 | `Payment` | Entity 已建立,API 未實作 |
| 物流/出貨 | `Shipment` | Entity 已建立,API 未實作 |
| 優惠券 | `Coupon` | Entity 已建立,API 未實作 |
| 商品評價 | `Review` | Entity 已建立,API 未實作 |

### 9.2 部分功能預留設計

| 設計 | 說明 |
| :--- | :--- |
| `users.deleted_at` | 軟刪除欄位已建立,Repository Query 尚未排除已刪除帳號 |
| `orders.coupon_id` | 優惠券 FK 欄位已在訂單表中,待優惠券模組完成後接入 |
| `ORDER_MANAGER`, `CUSTOMER_SERVICE`, `MARKETING`, `FINANCE` | 角色已定義,相關端點尚未實作 |

### 9.3 建議優先開發項目

1. **訂單狀態流轉 API** - 讓 `ORDER_MANAGER` 可更新訂單狀態 (付款,出貨,完成,取消) 
2. **金流整合** - 對接 ECPay 或其他金流,確認付款後更新 `Payment` 與 `Order.status`
3. **物流整合** - 出貨後記錄物流追蹤號,更新 `Shipment`
4. **優惠券系統** - 實作優惠券建立,核銷,結帳時套用折扣
5. **商品評價** - 顧客完成訂單後可留下評價
6. **軟刪除** - 完整實作 `users.deleted_at` 的查詢過濾
