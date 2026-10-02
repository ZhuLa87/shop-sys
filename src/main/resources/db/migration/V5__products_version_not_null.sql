-- products.version 原本允許 NULL: NULL + 1 仍是 NULL, 結帳 (deductStock) 不會遞增版本號,
-- 後台修改商品的樂觀鎖比對就失效. 補成 0 並禁止 NULL
UPDATE products SET version = 0 WHERE version IS NULL;
ALTER TABLE products MODIFY `version` bigint(20) NOT NULL DEFAULT 0;
