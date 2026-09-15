-- payments.status 改由 PaymentStatus enum 對應,與 orders/products 的 status 欄位一致使用原生 enum
-- (payments 表在此之前沒有任何寫入路徑,不會有既有資料轉換問題)
ALTER TABLE `payments`
    MODIFY COLUMN `status` enum('FAILED','SUCCESS','UNPAID') DEFAULT NULL;
