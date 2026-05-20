package com.zzowo.shop_sys.dto.request.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "加入購物車請求")
@Data
public class AddToCartRequest {

    @Schema(description = "商品 ID", example = "1")
    @NotNull(message = "商品 ID 不可為空")
    private Long productId;

    @Schema(description = "購買數量 (最少 1 件) ", example = "2")
    @NotNull(message = "數量不可為空")
    @Min(value = 1, message = "至少需要購買 1 件")
    private Integer quantity;
}
