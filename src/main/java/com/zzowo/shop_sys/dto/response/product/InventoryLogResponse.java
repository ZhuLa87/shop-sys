package com.zzowo.shop_sys.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "庫存變動紀錄")
@Data
public class InventoryLogResponse {

    @Schema(description = "紀錄 ID", example = "1")
    private Long id;

    @Schema(description = "庫存變動數量（正數為增加，負數為扣減）", example = "-2")
    private Integer changeAmount;

    @Schema(description = "變動原因：RESTOCK（進貨）、ORDER（出貨）、ADJUSTMENT（人工調整）、CANCEL（訂單取消回庫）、RETURN（退貨）", example = "ORDER")
    private String reason;

    @Schema(description = "操作人員使用者 ID", example = "3")
    private Long operatorId;

    @Schema(description = "紀錄建立時間", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "商品 ID", example = "5")
    private Long productId;

    @Schema(description = "商品名稱", example = "iPhone 16 Pro")
    private String productName;
}
