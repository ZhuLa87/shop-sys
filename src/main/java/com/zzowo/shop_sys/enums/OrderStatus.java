package com.zzowo.shop_sys.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "訂單狀態")
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    COMPLETED,
    CANCELLED
}
