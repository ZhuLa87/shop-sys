package com.zzowo.shop_sys.enums;

/**
 * 商品狀態列舉
 */
public enum ProductStatus {
    ON_SHELF("上架中"),    // 0
    OFF_SHELF("已下架"),   // 1
    OUT_OF_STOCK("缺貨中"); // 2

    private final String description;

    ProductStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}