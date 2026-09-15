-- 綠界 MerchantTradeNo 永久唯一,每次重新付款都會產生新的一筆,這裡記錄最後一次 (或實際付款成功) 的編號,
-- 供對帳與 QueryTradeInfo 查詢使用
ALTER TABLE `payments`
    ADD COLUMN `merchant_trade_no` varchar(20) DEFAULT NULL,
    ADD UNIQUE KEY `uk_payments_merchant_trade_no` (`merchant_trade_no`);
