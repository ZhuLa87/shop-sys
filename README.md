# Shop-Sys: B2C 電商購物平台專案

## 1. 專案簡介 (Project Overview)

這是一個基於 **前後端分離 (Frontend-Backend Separation)** 架構的單一廠商 B2C 購物網站。
系統專為「單一賣家對多位消費者」的商業模式設計，提供完整的使用者購物流程（瀏覽、加入購物車、結帳）以及後台管理功能（商品上架、訂單管理、庫存追蹤）。

## 2. 技術堆疊 (Tech Stack)

### 後端 (Backend)
- **核心框架**: Spring Boot 3.5.8
- **程式語言**: Java 21
- **資料庫**: MariaDB 11.8
- **安全性**: Spring Security + JWT (Stateless Authentication)
- **ORM**: Spring Data JPA (Hibernate)
- **建置工具**: Maven
- **密碼加密**: Argon2 (Spring Security Defaults)

### 前端 (Frontend)
- **核心框架**: Vue 3 (Composition API)
- **架構**: SPA (Single Page Application)

## 3. 系統功能 (Features)

目前後端已實作以下核心模組：

### 🔐 A. 認證與授權 (Authentication & Security)
- **JWT 驗證**: 使用無狀態 (Stateless) 的 JSON Web Token 進行身份驗證。
- **RBAC 權限控管**: 支援多種角色，包括 `CUSTOMER` (顧客)、`SUPER_ADMIN` (超級管理員)、`PRODUCT_MANAGER` (商品管理員)。
- **安全防護**: 包含密碼加密儲存、帳號鎖定機制 (登入失敗 5 次鎖定 15 分鐘)。

### 👤 B. 會員系統 (User System)
- **註冊與登入**: 支援 Email 註冊與登入。
- **個人資料管理**: 使用者可更新自身資料 (姓名、電話、Email)。
- **管理員功能**: 超級管理員可檢視、更新所有會員狀態 (含停權/啟用)。

### 📦 C. 商品系統 (Product System)
- **商品管理**: 支援商品的新增、修改、刪除 (軟刪除/下架)。
- **多圖支援**: 單一商品可關聯多張圖片 (ProductImage)。
- **庫存追蹤**: 系統自動記錄庫存異動 (InventoryLog)，包含進貨、出貨、調整等原因。
- **搜尋功能**: 支援商品名稱模糊搜尋與狀態過濾 (上架中/下架)。

### 🛒 D. 購物與訂單 (Cart & Order)
- **購物車**:
    - 加入購物車 (自動檢查庫存)。
    - 檢視與移除購物車商品。
- **訂單流程**:
    - **建立訂單 (結帳)**: 交易式 (Transactional) 處理，確保「建立訂單、扣除庫存、產生庫存紀錄、清空購物車」的一致性。
    - **訂單查詢**: 顧客可查詢自己的歷史訂單；管理員可檢視所有訂單。
    - **訂單明細**: 紀錄下單當下的商品價格 (Price At Purchase)，不受後續調價影響。

## 4. 資料庫設計 (Database Schema)

系統採用正規化關聯設計，主要實體如下：

| 實體 (Table) | 說明 | 關聯性 |
| :--- | :--- | :--- |
| **users** | 儲存會員帳號、密碼 Hash、角色與狀態 | 1 對多 Orders, 1 對多 Carts |
| **products** | 商品基本資訊 (價格、庫存、狀態) | 1 對多 ProductImages, 1 對多 InventoryLogs |
| **product_images** | 商品圖片網址 | 多 對 1 Product |
| **carts** | 購物車暫存資料 | 多 對 1 User, 多 對 1 Product |
| **orders** | 訂單主檔 (總金額、收件資訊、狀態) | 多 對 1 User, 1 對多 OrderItems |
| **order_items** | 訂單明細 (紀錄購買當下的快照) | 多 對 1 Order, 多 對 1 Product |
| **inventory_logs** | 庫存異動紀錄 (稽核用) | 多 對 1 Product |
| **payments** | 付款資訊 (金流) | 1 對 1 Order |
| **shipments** | 物流資訊 (出貨單號) | 1 對 1 Order |
| **coupons** | 優惠券系統 | (預留功能) |
| **reviews** | 商品評價 | (預留功能) |

## 5. API 快速導覽 (API Reference)

所有 API 均位於 `/api` 路徑下 (Context Path)，且 API 版本前綴為 `/v1`。

### 認證 (Auth)
* `POST /v1/auth/register` - 註冊新帳號
* `POST /v1/auth/login` - 登入並取得 JWT Token

### 商品 (Product)
* `GET /v1/products` - 取得上架商品列表 (Public)
* `GET /v1/products/{id}` - 取得商品詳情 (Public)
* `POST /v1/products` - 新增商品 (Admin/Manager)
* `PUT /v1/products/{id}` - 修改商品 (Admin/Manager)
* `DELETE /v1/products/{id}` - 刪除商品 (Admin/Manager)
* `GET /v1/products/inventory-logs` - 查詢所有庫存紀錄 (Admin/Manager)

### 購物車 (Cart)
* `GET /v1/carts` - 查詢我的購物車
* `POST /v1/carts` - 加入商品至購物車
* `DELETE /v1/carts/{id}` - 移除購物車項目

### 訂單 (Order)
* `POST /v1/orders` - 結帳 (建立訂單)
* `GET /v1/orders` - 查詢我的訂單列表
* `GET /v1/orders/{id}` - 查詢特定訂單詳情

### 會員 (User)
* `GET /v1/users/me` - 取得個人資訊
* `PUT /v1/users/me` - 更新個人資訊
* `GET /v1/users` - 取得所有會員列表 (Super Admin)

## 6. 專案結構 (Project Structure)

```text
com.zzowo.shop_sys
├── config/             # Spring Security, Exception Handler 配置
├── controller/         # REST API 控制層 (處理 HTTP 請求)
├── dto/                # 資料傳輸物件 (Request/Response)
│   ├── request/        # 接收前端的 JSON 格式
│   └── response/       # 回傳給前端的 JSON 格式
├── entity/             # 資料庫實體 (JPA Entity)
├── enums/              # 列舉 (Role, ProductStatus)
├── exception/          # 自定義異常類別
├── filter/             # JWT 認證過濾器
├── mapper/             # Entity 與 DTO 轉換邏輯
├── repository/         # 資料庫存取層 (Spring Data JPA)
├── service/            # 核心業務邏輯 (Transaction, 邏輯運算)
└── util/               # 工具類 (JwtUtil)