package com.zzowo.shop_sys.dto.response.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Schema(description = "綠界付款表單:前端以 form POST 將 params 送到 actionUrl")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EcpayCheckoutResponse {

    @Schema(description = "綠界付款頁網址", example = "https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5")
    private String actionUrl;

    @Schema(description = "表單欄位 (已含 CheckMacValue,送出前不可修改任何值)")
    private Map<String, String> params;
}
