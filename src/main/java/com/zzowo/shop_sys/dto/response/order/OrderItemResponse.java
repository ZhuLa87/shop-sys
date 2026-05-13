package com.zzowo.shop_sys.dto.response.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "訂單明細項目")
@Data
public class OrderItemResponse {

    @Schema(description = "商品 ID", example = "5")
    private Long productId;

    @Schema(description = "商品名稱", example = "iPhone 16 Pro")
    private String productName;

    @Schema(description = "下單時的商品單價（新台幣）", example = "39900")
    private BigDecimal priceAtPurchase;

    @Schema(description = "購買數量", example = "1")
    private Integer quantity;

    @Schema(description = "小計（單價 × 數量）", example = "39900")
    private BigDecimal subtotal;
}
