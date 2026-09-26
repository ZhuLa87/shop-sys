package com.zzowo.shop_sys.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 庫存異動原因列舉 (inventory_logs.reason 為 varchar, 以名稱字串儲存)
 */
@Schema(description = "庫存異動原因")
public enum InventoryChangeReason {
    RESTOCK("進貨"),
    ORDER("出貨"),
    ADJUSTMENT("人工調整"),
    CANCEL("訂單取消回庫");

    private final String description;

    InventoryChangeReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
