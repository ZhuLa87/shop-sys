package com.zzowo.shop_sys.dto.response.order;

import com.zzowo.shop_sys.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "訂單回應")
@Data
public class OrderResponse {

    @Schema(description = "訂單 ID", example = "1001")
    private Long id;

    @Schema(description = "訂單狀態：PENDING（待付款）、PAID（已付款）、SHIPPED（已出貨）、COMPLETED（已完成）、CANCELLED（已取消）", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "訂單總金額（新台幣）", example = "79800")
    private BigDecimal totalAmount;

    @Schema(description = "收件人姓名", example = "王小明")
    private String recipientName;

    @Schema(description = "收件人電話", example = "0912345678")
    private String recipientPhone;

    @Schema(description = "收件地址", example = "台北市中正區忠孝東路一段 1 號")
    private String recipientAddress;

    @Schema(description = "訂單建立時間（Unix 毫秒時間戳）", example = "1715000000000")
    private Long createdAt;

    @Schema(description = "訂單明細列表")
    private List<OrderItemResponse> items;
}
