package com.zzowo.shop_sys.dto.response.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "購物車項目")
@Data
public class CartItemResponse {

    @Schema(description = "購物車項目 ID", example = "1")
    private Long id;

    @Schema(description = "商品 ID", example = "5")
    private Long productId;

    @Schema(description = "商品名稱", example = "iPhone 16 Pro")
    private String productName;

    @Schema(description = "商品封面圖片 URL", example = "https://example.com/images/cover.jpg")
    private String coverImageUrl;

    @Schema(description = "商品單價 (新台幣) ", example = "39900")
    private BigDecimal price;

    @Schema(description = "購買數量", example = "2")
    private Integer quantity;

    @Schema(description = "小計 (單價 × 數量) ", example = "79800")
    private BigDecimal subtotal;
}
