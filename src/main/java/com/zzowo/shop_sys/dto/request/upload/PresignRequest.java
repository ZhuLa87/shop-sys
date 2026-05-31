package com.zzowo.shop_sys.dto.request.upload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PresignRequest {

    @NotBlank(message = "上傳類型不可為空")
    @Pattern(regexp = "^(product-cover|product-image|avatar)$", message = "不支援的上傳類型,允許值: product-cover, product-image, avatar")
    private String type;

    @NotNull(message = "資源 ID 不可為空")
    private Long resourceId;

    @NotBlank(message = "檔案名稱不可為空")
    private String filename;

    @NotBlank(message = "檔案類型不可為空")
    @Pattern(regexp = "^image/(jpeg|png|webp|gif)$", message = "僅支援 JPEG, PNG, WebP, GIF 格式")
    private String contentType;
}
