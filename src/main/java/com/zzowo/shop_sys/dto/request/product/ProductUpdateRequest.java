package com.zzowo.shop_sys.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 修改商品: 欄位同新增, 另外必須帶回取得商品時的 version.
// 表單會整包覆寫庫存, 沒有 version 就無法發現表單打開後的結帳扣減
@Schema(description = "商品修改請求")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductUpdateRequest extends ProductRequest {

    @Schema(description = "樂觀鎖版本號, 請帶入取得商品時的 version", example = "3")
    @NotNull(message = "版本號不可為空,請重新取得商品後再修改")
    private Long version;
}
