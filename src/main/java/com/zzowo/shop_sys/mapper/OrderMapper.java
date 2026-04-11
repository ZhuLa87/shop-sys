package com.zzowo.shop_sys.mapper;

import com.zzowo.shop_sys.dto.response.order.OrderItemResponse;
import com.zzowo.shop_sys.dto.response.order.OrderResponse;
import com.zzowo.shop_sys.entity.Order;
import com.zzowo.shop_sys.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order) {
        if (order == null) return null;

        OrderResponse res = new OrderResponse();
        res.setId(order.getId());
        res.setStatus(order.getStatus());
        res.setTotalAmount(order.getTotalAmount());
        res.setRecipientName(order.getRecipientName());
        res.setRecipientPhone(order.getRecipientPhone()); // 補上遺失的電話欄位
        res.setRecipientAddress(order.getRecipientAddress());

        if (order.getCreatedAt() != null) {
            res.setCreatedAt(order.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }

        // 轉換明細
        if (order.getItems() != null) {
            res.setItems(order.getItems().stream()
                    .map(this::toOrderItemResponse)
                    .collect(Collectors.toList()));
        }

        return res;
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        OrderItemResponse res = new OrderItemResponse();
        res.setProductId(item.getProduct().getId());
        res.setProductName(item.getProduct().getName());
        res.setPriceAtPurchase(item.getPriceAtPurchase());
        res.setQuantity(item.getQuantity());
        // 計算小計
        res.setSubtotal(item.getPriceAtPurchase().multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
        return res;
    }
}
