package com.zzowo.shop_sys.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.zzowo.shop_sys.config.SecurityConfig;
import com.zzowo.shop_sys.dto.response.payment.EcpayCheckoutResponse;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.service.PaymentService;
import com.zzowo.shop_sys.service.PaymentService.EcpayResult;
import com.zzowo.shop_sys.service.PaymentService.Outcome;
import com.zzowo.shop_sys.service.TokenBlacklistService;
import com.zzowo.shop_sys.util.JwtUtil;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean PaymentService paymentService;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenBlacklistService tokenBlacklistService;

    // ── POST /v1/payments/ecpay/checkout ─────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void checkout_authenticated_returnsForm() throws Exception {
        EcpayCheckoutResponse response = new EcpayCheckoutResponse(
                "https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5",
                Map.of("MerchantTradeNo", "SS1001TABC", "CheckMacValue", "ABC"));
        when(paymentService.createEcpayCheckout(any(), eq(1001L))).thenReturn(response);

        mockMvc.perform(post("/v1/payments/ecpay/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":1001}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actionUrl").value("https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5"))
                .andExpect(jsonPath("$.data.params.MerchantTradeNo").value("SS1001TABC"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void checkout_missingOrderId_returns422() throws Exception {
        // GlobalExceptionHandler 將 Bean Validation 錯誤統一回 422
        mockMvc.perform(post("/v1/payments/ecpay/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());

        verify(paymentService, never()).createEcpayCheckout(any(), any());
    }

    @Test
    @WithAnonymousUser
    void checkout_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/v1/payments/ecpay/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":1001}"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /v1/payments/ecpay/notify (ReturnURL) ──────────────────────────

    @Test
    @WithAnonymousUser
    void notify_verified_returnsExactPlainTextAck() throws Exception {
        when(paymentService.handlePaymentResult(anyMap())).thenReturn(new EcpayResult(Outcome.PAID, 1001L));

        mockMvc.perform(post("/v1/payments/ecpay/notify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("MerchantTradeNo", "SS1001TABC")
                        .param("RtnCode", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("1|OK"));
    }

    // 回歸測試:綠界 server 實際送出 Accept: text/html,曾因 produces = text/plain 被回 406 -> 401
    @Test
    @WithAnonymousUser
    void notify_ecpayAcceptTextHtml_stillReturnsPlainTextAck() throws Exception {
        when(paymentService.handlePaymentResult(anyMap())).thenReturn(new EcpayResult(Outcome.PAID, 1001L));

        mockMvc.perform(post("/v1/payments/ecpay/notify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.TEXT_HTML)
                        .param("MerchantTradeNo", "SS1001TABC"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("1|OK"));
    }

    @Test
    @WithAnonymousUser
    void notify_ecpayAcceptTextHtml_invalidCheckMacValue_returns400() throws Exception {
        when(paymentService.handlePaymentResult(anyMap())).thenReturn(new EcpayResult(Outcome.INVALID_SIGNATURE, null));

        mockMvc.perform(post("/v1/payments/ecpay/notify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.TEXT_HTML)
                        .param("MerchantTradeNo", "SS1001TABC"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("0|CheckMacValue Error"));
    }

    @Test
    @WithAnonymousUser
    void notify_ignoredButVerified_stillAcknowledges() throws Exception {
        when(paymentService.handlePaymentResult(anyMap())).thenReturn(new EcpayResult(Outcome.IGNORED, 1001L));

        mockMvc.perform(post("/v1/payments/ecpay/notify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("MerchantTradeNo", "SS1001TABC"))
                .andExpect(status().isOk())
                .andExpect(content().string("1|OK"));
    }

    @Test
    @WithAnonymousUser
    void notify_invalidCheckMacValue_returns400WithoutAck() throws Exception {
        when(paymentService.handlePaymentResult(anyMap())).thenReturn(new EcpayResult(Outcome.INVALID_SIGNATURE, null));

        mockMvc.perform(post("/v1/payments/ecpay/notify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("MerchantTradeNo", "SS1001TABC"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("0|CheckMacValue Error"));
    }

    @Test
    @WithAnonymousUser
    void notify_unexpectedError_returns500SoEcpayRetries() throws Exception {
        when(paymentService.handlePaymentResult(anyMap())).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(post("/v1/payments/ecpay/notify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("MerchantTradeNo", "SS1001TABC"))
                .andExpect(status().isInternalServerError());
    }

    // ── POST /v1/payments/ecpay/result (OrderResultURL) ─────────────────────

    @Test
    @WithAnonymousUser
    void result_redirectsBrowserToOrderPage() throws Exception {
        EcpayResult result = new EcpayResult(Outcome.PAID, 1001L);
        when(paymentService.handlePaymentResult(anyMap())).thenReturn(result);
        when(paymentService.resultRedirectUrl(result)).thenReturn("https://shop.example.com/orders/1001?payment=success");

        mockMvc.perform(post("/v1/payments/ecpay/result")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("MerchantTradeNo", "SS1001TABC"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://shop.example.com/orders/1001?payment=success"));
    }

    @Test
    @WithAnonymousUser
    void result_unexpectedError_redirectsToErrorPage() throws Exception {
        when(paymentService.handlePaymentResult(anyMap())).thenThrow(new IllegalStateException("db down"));
        when(paymentService.resultRedirectUrl(new EcpayResult(Outcome.IGNORED, null)))
                .thenReturn("https://shop.example.com/orders?payment=error");

        mockMvc.perform(post("/v1/payments/ecpay/result")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("MerchantTradeNo", "SS1001TABC"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://shop.example.com/orders?payment=error"));
    }
}
