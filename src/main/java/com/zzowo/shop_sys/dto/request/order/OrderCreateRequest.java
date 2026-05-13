package com.zzowo.shop_sys.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "建立訂單請求")
@Data
public class OrderCreateRequest {

    @Schema(description = "收件人姓名", example = "王小明")
    @NotBlank(message = "收件人姓名不可為空")
    private String recipientName;

    @Schema(description = "收件人電話", example = "0912345678")
    @NotBlank(message = "收件人電話不可為空")
    private String recipientPhone;

    @Schema(description = "收件地址", example = "台北市中正區忠孝東路一段 1 號")
    @NotBlank(message = "收件地址不可為空")
    private String recipientAddress;
}
