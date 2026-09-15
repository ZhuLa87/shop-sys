package com.zzowo.shop_sys.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "付款狀態")
public enum PaymentStatus {
    UNPAID,   // 尚未付款 (已導向綠界,等待結果)
    SUCCESS,  // 付款成功
    FAILED    // 付款失敗 (訂單仍為 PENDING,可重新付款)
}
