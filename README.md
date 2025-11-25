# B2C 電商購物平台專案 (B2C E-commerce Platform)

## 1. 專案簡介 (Introduction)

這是一個基於前後端分離 (Frontend-Backend Separation) 架構的單一廠商 B2C 購物網站。
本專案旨在建立一個完整的電子商務流程，包含使用者瀏覽、購物車管理、結帳流程，以及後台的商品與權限管理。

## 2. 技術棧 (Tech Stack)

### 後端 (Backend)

- 框架 (Framework): Spring Boot 3.5.8
- 語言 (Language): Java 21
- 安全性 (Security): Spring Security + JWT
- 資料庫存取 (ORM): Spring Data JPA (Hibernate)
- 建置工具 (Build Tool): Maven

### 前端 (Frontend)

- 框架 (Framework):
- 狀態管理 (State Management):
- 路由管理 (Routing):
- HTTP 請求 (HTTP Client):
- UI 庫 (UI Library):

### 資料庫 (Database)

- 資料庫 (Database): MariaDB 11.8
- 版本控制 (Migration): Flyway

## 3. 功能模組 (Features)

### A. 會員系統 (User System)

- 註冊與登入 (Auth): 支援帳號密碼註冊，使用 JWT 進行身份驗證。
- 角色權限 (RBAC): SUPER_ADMIN, PRODUCT_MANAGER, ORDER_MANAGER, CUSTOMER_SERVICE, MARKETING, FINANCE, CUSTOMER
- 個人資料管理: 修改密碼、查看歷史訂單。

### B. 商品系統 (Product System)

- 商品列表 (List): 分頁顯示商品、圖片、關鍵字搜尋。
- 商品詳情 (Detail): 顯示價格、庫存、詳細描述。
- (後台) 商品管理: 新增、修改、下架商品 (CRUD)。

### C. 購物與訂單 (Shopping & Order)

- 購物車 (Cart): 加入商品、調整數量、移除商品。
- 結帳流程 (Checkout): 確認收件資訊、建立訂單。
- 訂單狀態: 待付款 -> 處理中 -> 已出貨 -> 完成 / 取消 -> 退貨 -> 退款。
- 庫存扣減: 確保下單時同步扣除資料庫庫存 (Transaction)。

## 4. 資料庫初步設計 (Database Schema Draft)

本專案核心將圍繞以下幾張資料表 (Table) 進行關聯：

1. Users (用戶表): 存帳號、加密後的密碼、角色 (Role)。
2. Products (商品表): 存名稱、價格、庫存數量、圖片 URL、描述。
3. Orders (訂單主表): 存訂單編號、總金額、下單用戶 ID、訂單狀態、建立時間。
4. OrderItems (訂單明細表): 存該筆訂單買了哪些商品、當時購買的單價、數量。
5. Carts (購物車): 存商品和數量。
6. Inventory_Logs (庫存紀錄表): 記錄庫存怎麼變動 (進貨、出貨、退貨等)。
7. Payments (付款紀錄): 記錄金流回傳、付款狀態。
8. Shipments (物流/出貨紀錄): 記錄物流編號、出貨時間、運送狀態。
9. Product_Images (商品圖片): 一個商品通常有多張照片。
10. Reviews (商品評價): 寫評價與打分數。
11. Coupons / Discounts (折扣券): 優惠券。

## 5. 開發環境與安裝 (Setup)

### 前置需求 (Prerequisites)

- JDK 17+
- MariaDB Server

### 啟動步驟 (Getting Started)

1. 資料庫 (Database)

建立一個名為 shop-sys 的資料庫。

```
CREATE DATABASE shop-sys CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 後端 (Backend)

    1. 進入 backend 目錄。
    2. 修改 application.properties 設定資料庫連線資訊。
    3. 執行 Spring Boot 應用程式。

3. 前端 (Frontend)

    1. 進入 frontend 目錄。
    2. 安裝依賴:
    3. 啟動開發伺服器:
