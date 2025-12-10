package com.zzowo.shop_sys.dto.request.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToCartRequest {

    @NotNull(message = "商品 ID 不可為空")
    private Long productId;

    @NotNull(message = "數量不可為空")
    @Min(value = 1, message = "至少需要購買 1 件")
    private Integer quantity;
}