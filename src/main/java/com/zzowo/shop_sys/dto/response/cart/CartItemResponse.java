package com.zzowo.shop_sys.dto.response.cart;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemResponse {
    private Long id;              // 購物車紀錄 ID
    private Long productId;       // 商品 ID
    private String productName;   // 商品名稱
    private String coverImageUrl; // 商品圖片
    private BigDecimal price;     // 單價
    private Integer quantity;     // 數量
    private BigDecimal subtotal;  // 小計 (單價 * 數量)
}