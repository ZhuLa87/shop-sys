-- 庫存異動紀錄補上操作者身分快照
-- 原本只存 operator_id,後台稽核日誌只看得到裸 ID,無法快速分辨是誰做的異動.
-- 名稱與角色在寫入當下快照一份 (同 order_items 的 product_name / price_at_purchase 慣例) ,
-- 使用者日後改名,調整角色或軟刪除時,歷史紀錄仍保留當時的事實.

ALTER TABLE `inventory_logs`
  ADD COLUMN `operator_name` varchar(100) DEFAULT NULL AFTER `operator_id`,
  ADD COLUMN `operator_role` varchar(32)  DEFAULT NULL AFTER `operator_name`;

-- 回填既有紀錄: 以目前的 users 資料為準 (已軟刪除的使用者查不到,維持 NULL)
UPDATE `inventory_logs` l
  JOIN `users` u ON u.id = l.operator_id
SET l.operator_name = COALESCE(NULLIF(u.name, ''), u.email),
    l.operator_role = u.role;
