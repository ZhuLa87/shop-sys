package com.zzowo.shop_sys.mapper;

import com.zzowo.shop_sys.dto.response.product.InventoryLogResponse;
import com.zzowo.shop_sys.entity.InventoryLog;
import org.springframework.stereotype.Component;

@Component
public class InventoryLogMapper {

    public InventoryLogResponse toResponse(InventoryLog log) {
        if (log == null) {
            return null;
        }

        InventoryLogResponse response = new InventoryLogResponse();
        response.setId(log.getId());
        response.setChangeAmount(log.getChangeAmount());
        response.setReason(log.getReason());
        response.setOperatorId(log.getOperatorId());
        response.setCreatedAt(log.getCreatedAt());

        return response;
    }
}