package com.zzowo.shop_sys.dto.request.product;

import com.zzowo.shop_sys.enums.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {

    @NotBlank(message = "商品名稱不可為空")
    private String name;

    private String description;

    @NotNull(message = "價格不可為空")
    @Min(value = 0, message = "價格不可小於 0")
    private BigDecimal price;

    @NotNull(message = "庫存不可為空")
    @Min(value = 0, message = "庫存不可小於 0")
    private Integer stockQuantity;

    @NotNull(message = "商品狀態不可為空")
    private ProductStatus status;

    // 封面圖片 URL
    private String coverImageUrl;

    // 接收多張圖片的 URL (例如輪播圖)
    private List<String> imageUrls;
}