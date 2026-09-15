package com.zzowo.shop_sys.dto.request.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "建立綠界付款請求")
@Data
public class EcpayCheckoutRequest {

    @Schema(description = "要付款的訂單 ID (必須是自己的待付款訂單)", example = "1001")
    @NotNull(message = "訂單 ID 不可為空")
    private Long orderId;
}
