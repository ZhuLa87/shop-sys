package com.zzowo.shop_sys.dto.response.order;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private String status;
    private BigDecimal totalAmount;
    private String recipientName;
    private String recipientPhone;
    private String recipientAddress;
    private Long createdAt;

    // 訂單明細
    private List<OrderItemResponse> items;
}
