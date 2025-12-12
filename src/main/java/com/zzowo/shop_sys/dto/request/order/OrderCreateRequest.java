package com.zzowo.shop_sys.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderCreateRequest {

    @NotBlank(message = "收件人姓名不可為空")
    private String recipientName;

    @NotBlank(message = "收件人電話不可為空")
    private String recipientPhone;

    @NotBlank(message = "收件地址不可為空")
    private String recipientAddress;

    // 目前先省略付款方式，預設流程建立後為 PENDING (待付款)
    // private String paymentMethod;
    // private String couponCode;
}