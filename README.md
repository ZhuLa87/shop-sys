# Shop-Sys - B2C 電商購物平台

> 單一廠商 B2C 電商系統,前後端分離架構,提供完整購物流程與後台管理功能.

---

## 技術堆疊

| 類別 | 技術 | 版本 |
| :--- | :--- | :--- |
| 後端框架 | Spring Boot | 3.5.13 |
| 程式語言 | Java | 21 |
| 資料庫 | MariaDB | 11.8 |
| 安全框架 | Spring Security + JWT | HS512, 24h |
| 密碼雜湊 | Argon2 | v5.8 defaults |
| ORM | Spring Data JPA (Hibernate) | - |
| 建置工具 | Maven | 3.8.x |
| 前端框架 | Vue 3 (Composition API) | SPA |

---

## 快速啟動

### 前置需求

- Java 21+
- Maven 3.8+
- MariaDB 11.8 (或相容版本) 

### 設定資料庫

在 `src/main/resources/application.yaml` 修改連線資訊:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://{host}:{port}/shop-sys
    username: {username}
    password: {password}
```

DDL 模式預設為 `update`,首次啟動將自動建立資料表.

### 啟動服務

```bash
./mvnw spring-boot:run
```

服務啟動後:`http://localhost:8088/api`

### 測試資料 (Dev Profile) 

`application.yaml` 已預設啟用 `dev` profile.首次啟動時,`DataInitializer` 會自動偵測資料庫是否為空,並在空白時匯入測試資料.

| Email | 密碼 | 角色 |
| :--- | :--- | :--- |
| admin@test.com | admin123 | SUPER_ADMIN |
| manager@test.com | manager123 | PRODUCT_MANAGER |
| test01@example.com | mypassword123 | CUSTOMER |
| test02@example.com | mypassword123 | CUSTOMER |

同時會建立 8 筆商品 (6 件上架,1 件下架,1 件缺貨) 及測試購物車資料.

> 資料已存在時會自動跳過,不重複插入.若需重置,清空 `carts`,`products`,`users` 三張表後重啟即可.

---

## 系統架構

```
[Vue 3 SPA] ←── HTTP JSON ──→ [Spring Boot :8088/api] ←──→ [MariaDB]
```

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

- JWT Stateless 認證 (HS512, 有效 24 小時) 
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

Base URL: `http://localhost:8088/api/v1`  
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
