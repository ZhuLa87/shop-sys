package com.zzowo.shop_sys.dto.response.product;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InventoryLogResponse {
    private Long id;
    private Integer changeAmount; // 變動數量
    private String reason;        // 變動原因
    private Long operatorId;      // 操作人員 ID
    private LocalDateTime createdAt; // 建立時間

    private Long productId;
    private String productName;
}