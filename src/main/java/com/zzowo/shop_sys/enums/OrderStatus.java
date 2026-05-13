package com.zzowo.shop_sys.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "訂單狀態")
public enum OrderStatus {
    PENDING,    // 待付款
    PAID,       // 已付款
    SHIPPED,    // 已出貨
    COMPLETED,  // 已完成
    CANCELLED   // 已取消
}
