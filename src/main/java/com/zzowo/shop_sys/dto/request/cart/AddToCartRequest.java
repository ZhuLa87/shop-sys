package com.zzowo.shop_sys.dto.request.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "加入購物車請求")
@Data
public class AddToCartRequest {

    // 購物車單一商品的數量上限 (單次加入與累加後的總數都適用)
    public static final int MAX_QUANTITY = 999;

    @Schema(description = "商品 ID", example = "1")
    @NotNull(message = "商品 ID 不可為空")
    private Long productId;

    @Schema(description = "購買數量 (1 到 999 件) ", example = "2")
    @NotNull(message = "數量不可為空")
    @Min(value = 1, message = "至少需要購買 1 件")
    @Max(value = MAX_QUANTITY, message = "單次最多加入 999 件")
    private Integer quantity;
}
