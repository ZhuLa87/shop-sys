package com.zzowo.shop_sys.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "商品資訊")
@Data
public class ProductResponse {

    @Schema(description = "商品 ID", example = "5")
    private Long id;

    @Schema(description = "商品名稱", example = "iPhone 16 Pro")
    private String name;

    @Schema(description = "商品描述", example = "最新款 Apple 旗艦手機,搭載 A18 Pro 晶片")
    private String description;

    @Schema(description = "售價 (新台幣) ", example = "39900")
    private BigDecimal price;

    @Schema(description = "庫存數量", example = "50")
    private Integer stockQuantity;

    @Schema(description = "封面圖片 URL", example = "https://example.com/images/cover.jpg")
    private String coverImageUrl;

    @Schema(description = "商品圖片 URL 列表 (輪播圖) ")
    private List<String> imageUrls;

    @Schema(description = "商品狀態:ON_SHELF / OFF_SHELF / OUT_OF_STOCK", example = "ON_SHELF")
    private String status;

    @Schema(description = "軟刪除時間,null 表示未刪除")
    private LocalDateTime deletedAt;
}
