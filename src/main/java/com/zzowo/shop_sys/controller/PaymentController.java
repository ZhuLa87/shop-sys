package com.zzowo.shop_sys.controller;

import com.zzowo.shop_sys.dto.request.payment.EcpayCheckoutRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.payment.EcpayCheckoutResponse;
import com.zzowo.shop_sys.service.PaymentService;
import com.zzowo.shop_sys.service.PaymentService.EcpayResult;
import com.zzowo.shop_sys.service.PaymentService.Outcome;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@Slf4j
@Tag(name = "Payment", description = "付款 API (綠界 AIO 信用卡)")
@RestController
@RequestMapping("/v1/payments/ecpay")
public class PaymentController {

    // 綠界規定的回應格式:純文字,HTTP 200,不可帶引號/空白/換行,否則會重送
    // Source: https://developers.ecpay.com.tw/2878.md (2026-09-15)
    static final String ECPAY_ACK = "1|OK";

    @Autowired
    private PaymentService paymentService;

    @Operation(summary = "建立綠界付款表單", description = "為自己的待付款 (PENDING) 訂單產生綠界付款參數,前端以 form POST 送到 actionUrl")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "產生成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "訂單不是待付款狀態,或金額無法以新台幣付款")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "嘗試為他人的訂單付款")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<EcpayCheckoutResponse>> checkout(
            @Parameter(hidden = true) @AuthenticationPrincipal String email,
            @Valid @RequestBody EcpayCheckoutRequest request) {
        EcpayCheckoutResponse response = paymentService.createEcpayCheckout(email, request.getOrderId());
        return ResponseEntity.ok(ApiResponse.success("付款表單產生成功", response));
    }

    @Operation(summary = "綠界付款結果通知 (ReturnURL)", description = "僅供綠界 server 呼叫 (Form POST + CheckMacValue),驗證通過回應 1|OK")
    // 不可宣告 produces = text/plain:綠界送的是 Accept: text/html,宣告後會找不到 handler (406),
    // 錯誤轉到 /error 又被 Spring Security 擋成 401,綠界就永遠收不到 1|OK.
    // 改由 plainText() 直接指定 Content-Type,Spring 不會再依 Accept 協商.
    @PostMapping("/notify")
    public ResponseEntity<String> notify(@RequestParam Map<String, String> params) {
        try {
            EcpayResult result = paymentService.handlePaymentResult(params);
            if (result.outcome() == Outcome.INVALID_SIGNATURE) {
                return plainText(HttpStatus.BAD_REQUEST, "0|CheckMacValue Error");
            }
            // 金額不符,模擬付款等異常已記錄 log,仍回 1|OK 避免綠界反覆重送
            return plainText(HttpStatus.OK, ECPAY_ACK);
        } catch (RuntimeException e) {
            // DB 暫時失敗等非預期錯誤:不回 1|OK,讓綠界稍後重送 (每 5-15 分鐘,一天最多 4 次)
            log.error("處理綠界付款通知失敗,MerchantTradeNo={}", params.get("MerchantTradeNo"), e);
            return plainText(HttpStatus.INTERNAL_SERVER_ERROR, "0|Error");
        }
    }

    @Operation(summary = "綠界付款結果導回 (OrderResultURL)", description = "消費者瀏覽器付款完成後由綠界頁面 POST 過來,處理後導回前端訂單頁")
    @PostMapping("/result")
    public ResponseEntity<Void> result(@RequestParam Map<String, String> params) {
        String redirectUrl;
        try {
            redirectUrl = paymentService.resultRedirectUrl(paymentService.handlePaymentResult(params));
        } catch (RuntimeException e) {
            log.error("處理綠界付款導回失敗,MerchantTradeNo={}", params.get("MerchantTradeNo"), e);
            redirectUrl = paymentService.resultRedirectUrl(new EcpayResult(Outcome.IGNORED, null));
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }

    private static ResponseEntity<String> plainText(HttpStatus status, String body) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body(body);
    }
}
