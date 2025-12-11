# B2C 電商購物平台專案 (B2C E-commerce Platform)

## 1. 專案簡介 (Introduction)

這是一個基於 **前後端分離 (Frontend-Backend Separation)** 架構的單一廠商 B2C 購物網站。
本專案旨在建立一個完整的電子商務流程，包含使用者瀏覽、購物車管理、結帳流程，以及後台的商品與權限管理。

## 2. 技術棧 (Tech Stack)

### 後端 (Backend) - 已建立基礎架構

- **框架 (Framework)**: Spring Boot 3.5.8
- **語言 (Language)**: Java 21
- **安全性 (Security)**: Spring Security + JWT (Stateless)
- **資料庫存取 (ORM)**: Spring Data JPA (Hibernate)
- **建置工具 (Build Tool)**: Maven
- **API 文件**: Swagger

### 前端 (Frontend) - 預計採用

- **核心框架 (Framework)**: Vue 3 (Composition API)
- **建置工具 (Build Tool)**:
- **狀態管理 (State Management)**:
- **路由管理 (Routing)**:
- **HTTP 請求 (HTTP Client)**:
- **UI 框架 (UI Library)**:

### 資料庫 (Database)

- **資料庫 (Database)**: MariaDB 11.8
- **版本控制 (Migration)**:

## 3. 功能模組 (Features) & 開發進度

### A. 會員系統 (User System) [Backend: ✅ Ready]
- [x] **註冊與登入**: 支援 Email 註冊，使用 JWT 進行身份驗證 (AuthController)。
- [x] **權限控管 (RBAC)**: 區分 CUSTOMER, SUPER_ADMIN, PRODUCT_MANAGER 等角色。
- [x] **個人資料管理**:
    - 使用者可修改自身資料 (UserService)。
    - 管理員可管理所有使用者。

### B. 商品系統 (Product System) [Backend: ✅ Ready]
- [x] **商品列表**: 取得上架商品 (ProductController)。
- [x] **商品詳情**: 包含圖片、價格、庫存描述。
- [x] **後台管理**:
    - 新增/修改商品 (支援多圖 URL)。
    - 商品上下架狀態管理 (ON_SHELF, OFF_SHELF)。

### C. 購物與訂單 (Shopping & Order) [Backend: ⚠️ Partially Ready]
- [x] **購物車 (Cart)**: 加入商品、查看購物車、移除商品 (CartController)。
- [ ] **結帳流程 (Checkout)**: 建立訂單 (Order) 與 訂單明細 (OrderItem)。
- [ ] **庫存扣減**: 確保下單時同步扣除庫存 (Transaction)。
- [ ] **訂單狀態管理**: 待付款 -> 已出貨 -> 完成/取消。

### D. 其他 (Others)
- [ ] **庫存紀錄**: 記錄進出貨歷程 (InventoryLog)。
- [ ] **金流與物流**: Payment 與 Shipment 實體已建立，待實作邏輯。

## 4. 資料庫設計 (Schema Overview)

核心資料表關聯如下：

1.  **Users**: 帳號、密碼 (Argon2/BCrypt)、角色。
2.  **Products**: 商品基本資訊、價格、庫存。
    - **Product_Images**: 商品關聯圖片 (一對多)。
3.  **Carts**: 使用者購物車 (暫存選購商品)。
4.  **Orders**: 訂單主檔 (總金額、收件資訊)。
    - **OrderItems**: 訂單明細 (紀錄購買當下的價格與數量)。
5.  **Payments**: 付款資訊 (一對一 Order)。
6.  **Shipments**: 物流資訊 (一對一 Order)。
7.  **Inventory_Logs**: 庫存異動紀錄。

## 5. API 快速導覽 (Quick Reference)

| Method | Endpoint | Description | Auth |
|:---|:---|:---|:---|
| POST | `/v1/auth/register` | 註冊使用者 | Public |
| POST | `/v1/auth/login` | 登入取得 JWT | Public |
| GET | `/v1/products` | 瀏覽商品列表 | Public |
| GET | `/v1/products/{id}` | 瀏覽商品詳情 | Public |
| POST | `/v1/carts` | 加入購物車 | User |
| GET | `/v1/carts` | 查看我的購物車 | User |
| GET | `/v1/users/me` | 查看個人資訊 | User |

## 6. 開發環境與安裝 (Setup)

### 前置需求 (Prerequisites)
- JDK 21
- Node.js 18+ (For Frontend)
- MariaDB 11.8

### 啟動步驟 (Getting Started)

1.  **資料庫 (Database)**
    ```sql
    CREATE DATABASE `shop-sys` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    ```
    *請確認 `application.yaml` 中的帳號密碼與您的本地環境一致。*

2.  **後端 (Backend)**
    - 進入專案根目錄。
    - 執行 `mvnw spring-boot:run`。

3.  **前端 (Frontend)**
    - (待初始化 Vue 專案後補充)