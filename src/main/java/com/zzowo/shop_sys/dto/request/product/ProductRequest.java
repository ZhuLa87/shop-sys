package com.zzowo.shop_sys.dto.request.product;

import com.zzowo.shop_sys.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "商品新增 / 修改請求")
@Data
public class ProductRequest {

    @Schema(description = "商品名稱", example = "iPhone 16 Pro")
    @NotBlank(message = "商品名稱不可為空")
    private String name;

    @Schema(description = "商品描述", example = "最新款 Apple 旗艦手機,搭載 A18 Pro 晶片")
    private String description;

    @Schema(description = "售價 (新台幣) ,不可小於 0", example = "39900")
    @NotNull(message = "價格不可為空")
    @Min(value = 0, message = "價格不可小於 0")
    private BigDecimal price;

    @Schema(description = "庫存數量,不可小於 0", example = "50")
    @NotNull(message = "庫存不可為空")
    @Min(value = 0, message = "庫存不可小於 0")
    private Integer stockQuantity;

    @Schema(description = "商品狀態:ON_SHELF (上架) ,OFF_SHELF (下架) ,OUT_OF_STOCK (缺貨) ", example = "ON_SHELF")
    @NotNull(message = "商品狀態不可為空")
    private ProductStatus status;

    @Schema(description = "封面圖片 URL", example = "https://example.com/images/cover.jpg")
    private String coverImageUrl;

    @Schema(description = "商品圖片 URL 列表 (輪播圖) ", example = "[\"https://example.com/1.jpg\", \"https://example.com/2.jpg\"]")
    private List<String> imageUrls;
}
