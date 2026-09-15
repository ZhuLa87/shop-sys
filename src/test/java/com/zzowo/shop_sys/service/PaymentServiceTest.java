package com.zzowo.shop_sys.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.zzowo.shop_sys.config.EcpayConfig;
import com.zzowo.shop_sys.dto.response.payment.EcpayCheckoutResponse;
import com.zzowo.shop_sys.entity.Order;
import com.zzowo.shop_sys.entity.OrderItem;
import com.zzowo.shop_sys.entity.Payment;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.OrderStatus;
import com.zzowo.shop_sys.enums.PaymentStatus;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.repository.OrderRepository;
import com.zzowo.shop_sys.repository.PaymentRepository;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.service.PaymentService.EcpayResult;
import com.zzowo.shop_sys.service.PaymentService.Outcome;
import com.zzowo.shop_sys.util.EcpayCheckMacValue;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String EMAIL = "buyer@test.com";
    private static final String MERCHANT_ID = "3002607";
    private static final String HASH_KEY = "pwFHCqoQZGmho4w6";
    private static final String HASH_IV = "EkRm7iFT261dpevs";
    private static final Long ORDER_ID = 1001L;

    @Mock OrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @Mock PaymentRepository paymentRepository;
    @Spy EcpayConfig ecpayConfig = testConfig();
    @InjectMocks PaymentService paymentService;

    // ── createEcpayCheckout ──────────────────────────────────────────────────

    @Test
    void createEcpayCheckout_success_returnsSignedCreditCardForm() {
        User buyer = buildUser(1L);
        Order order = buildOrder(buyer, "14000.00");
        order.getItems().add(buildItem("手機", 2));
        order.getItems().add(buildItem("耳機#藍芽<b>", 1));
        stubCheckout(buyer, order);
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        EcpayCheckoutResponse response = paymentService.createEcpayCheckout(EMAIL, ORDER_ID);

        Map<String, String> params = response.getParams();
        assertThat(response.getActionUrl()).isEqualTo("https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5");
        assertThat(params)
                .containsEntry("MerchantID", MERCHANT_ID)
                .containsEntry("PaymentType", "aio")
                .containsEntry("TotalAmount", "14000")
                .containsEntry("ChoosePayment", "Credit")
                .containsEntry("EncryptType", "1")
                .containsEntry("CustomField1", "1001")
                .containsEntry("ReturnURL", "https://shop.example.com/api/v1/payments/ecpay/notify")
                .containsEntry("OrderResultURL", "https://shop.example.com/api/v1/payments/ecpay/result")
                .containsEntry("ClientBackURL", "https://shop.example.com/orders/1001")
                // # 是多品項分隔符,HTML 標籤要移除
                .containsEntry("ItemName", "手機 x2#耳機 藍芽 x1");
        assertThat(params.get("MerchantTradeNo")).matches("SS1001T[0-9A-Z]+").hasSizeLessThanOrEqualTo(20);
        assertThat(params.get("MerchantTradeDate")).matches("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}");
        assertThat(EcpayCheckMacValue.verify(params, HASH_KEY, HASH_IV)).isTrue();

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment payment = captor.getValue();
        assertThat(payment.getOrder()).isSameAs(order);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentService.PAYMENT_METHOD_ECPAY_CREDIT);
        assertThat(payment.getAmount()).isEqualByComparingTo("14000");
        assertThat(payment.getMerchantTradeNo()).isEqualTo(params.get("MerchantTradeNo"));
    }

    @Test
    void createEcpayCheckout_retryAfterFailure_reusesPaymentRowWithNewTradeNo() {
        User buyer = buildUser(1L);
        Order order = buildOrder(buyer, "500");
        order.getItems().add(buildItem("滑鼠", 1));
        stubCheckout(buyer, order);
        Payment existing = buildPayment(order, PaymentStatus.FAILED);
        existing.setMerchantTradeNo("SS1001TOLD");
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existing));

        EcpayCheckoutResponse response = paymentService.createEcpayCheckout(EMAIL, ORDER_ID);

        verify(paymentRepository).save(existing);
        assertThat(existing.getStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(existing.getMerchantTradeNo())
                .isNotEqualTo("SS1001TOLD")
                .isEqualTo(response.getParams().get("MerchantTradeNo"));
    }

    @Test
    void createEcpayCheckout_longItemName_isTruncatedTo200Chars() {
        User buyer = buildUser(1L);
        Order order = buildOrder(buyer, "100");
        for (int i = 0; i < 30; i++) {
            order.getItems().add(buildItem("超長的商品名稱第" + i + "號", 1));
        }
        stubCheckout(buyer, order);
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        EcpayCheckoutResponse response = paymentService.createEcpayCheckout(EMAIL, ORDER_ID);

        assertThat(response.getParams().get("ItemName")).hasSize(200);
    }

    @Test
    void createEcpayCheckout_otherUsersOrder_throwsForbidden() {
        User buyer = buildUser(1L);
        Order order = buildOrder(buildUser(2L), "100");
        stubCheckout(buyer, order);

        assertThatThrownBy(() -> paymentService.createEcpayCheckout(EMAIL, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createEcpayCheckout_orderNotPending_throwsBusinessException() {
        User buyer = buildUser(1L);
        Order order = buildOrder(buyer, "100");
        order.setStatus(OrderStatus.PAID);
        stubCheckout(buyer, order);

        assertThatThrownBy(() -> paymentService.createEcpayCheckout(EMAIL, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無法付款");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createEcpayCheckout_fractionalAmount_throwsBusinessException() {
        User buyer = buildUser(1L);
        Order order = buildOrder(buyer, "99.50");
        stubCheckout(buyer, order);

        assertThatThrownBy(() -> paymentService.createEcpayCheckout(EMAIL, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("小數");
        verify(paymentRepository, never()).save(any());
    }

    // ── handlePaymentResult ──────────────────────────────────────────────────

    @Test
    void handlePaymentResult_success_marksOrderPaidAndRecordsTransaction() {
        Order order = buildOrder(buildUser(1L), "14000.00");
        Payment payment = buildPayment(order, PaymentStatus.UNPAID);
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

        EcpayResult result = paymentService.handlePaymentResult(signedCallback("14000", "1", "0", "2509151234567890"));

        assertThat(result).isEqualTo(new EcpayResult(Outcome.PAID, ORDER_ID));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
        verify(paymentRepository).save(payment);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionId()).isEqualTo("2509151234567890");
        assertThat(payment.getMerchantTradeNo()).isEqualTo("SS1001TABC123");
        assertThat(payment.getPaymentMethod()).isEqualTo("Credit_CreditCard");
        assertThat(payment.getPaidAt()).isEqualTo(LocalDateTime.of(2026, 9, 15, 12, 5, 0));
    }

    @Test
    void handlePaymentResult_duplicateNotification_isIdempotent() {
        Order order = buildOrder(buildUser(1L), "14000");
        order.setStatus(OrderStatus.PAID);
        Payment payment = buildPayment(order, PaymentStatus.SUCCESS);
        payment.setTransactionId("2509151234567890");
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

        EcpayResult result = paymentService.handlePaymentResult(signedCallback("14000", "1", "0", "2509151234567890"));

        assertThat(result.outcome()).isEqualTo(Outcome.PAID);
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handlePaymentResult_invalidCheckMacValue_rejectsWithoutTouchingOrder() {
        Map<String, String> callback = signedCallback("14000", "1", "0", "2509151234567890");
        callback.put("TradeAmt", "1");

        EcpayResult result = paymentService.handlePaymentResult(callback);

        assertThat(result).isEqualTo(new EcpayResult(Outcome.INVALID_SIGNATURE, null));
        verifyNoInteractions(orderRepository, paymentRepository);
    }

    @Test
    void handlePaymentResult_otherMerchantId_isIgnored() {
        Map<String, String> callback = signedCallback("14000", "1", "0", "2509151234567890");
        callback.put("MerchantID", "9999999");
        resign(callback);

        EcpayResult result = paymentService.handlePaymentResult(callback);

        assertThat(result.outcome()).isEqualTo(Outcome.IGNORED);
        verifyNoInteractions(orderRepository, paymentRepository);
    }

    @Test
    void handlePaymentResult_amountMismatch_isIgnored() {
        Order order = buildOrder(buildUser(1L), "14000");
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        EcpayResult result = paymentService.handlePaymentResult(signedCallback("100", "1", "0", "2509151234567890"));

        assertThat(result).isEqualTo(new EcpayResult(Outcome.IGNORED, ORDER_ID));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handlePaymentResult_simulatedPayment_isIgnored() {
        Order order = buildOrder(buildUser(1L), "14000");
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        EcpayResult result = paymentService.handlePaymentResult(signedCallback("14000", "1", "1", "2509151234567890"));

        assertThat(result.outcome()).isEqualTo(Outcome.IGNORED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handlePaymentResult_paymentFailed_marksPaymentFailedAndKeepsOrderPending() {
        Order order = buildOrder(buildUser(1L), "14000");
        Payment payment = buildPayment(order, PaymentStatus.UNPAID);
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

        EcpayResult result = paymentService.handlePaymentResult(signedCallback("14000", "10100248", "0", "2509151234567890"));

        assertThat(result).isEqualTo(new EcpayResult(Outcome.NOT_PAID, ORDER_ID));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handlePaymentResult_cancelledOrder_isIgnoredForManualRefund() {
        Order order = buildOrder(buildUser(1L), "14000");
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(buildPayment(order, PaymentStatus.UNPAID)));

        EcpayResult result = paymentService.handlePaymentResult(signedCallback("14000", "1", "0", "2509151234567890"));

        assertThat(result.outcome()).isEqualTo(Outcome.IGNORED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository, never()).save(any());
    }

    // ── resultRedirectUrl ────────────────────────────────────────────────────

    @Test
    void resultRedirectUrl_mapsOutcomeToOrderPage() {
        assertThat(paymentService.resultRedirectUrl(new EcpayResult(Outcome.PAID, ORDER_ID)))
                .isEqualTo("https://shop.example.com/orders/1001?payment=success");
        assertThat(paymentService.resultRedirectUrl(new EcpayResult(Outcome.NOT_PAID, ORDER_ID)))
                .isEqualTo("https://shop.example.com/orders/1001?payment=failed");
        assertThat(paymentService.resultRedirectUrl(new EcpayResult(Outcome.INVALID_SIGNATURE, null)))
                .isEqualTo("https://shop.example.com/orders?payment=error");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static EcpayConfig testConfig() {
        EcpayConfig config = new EcpayConfig();
        config.setMerchantId(MERCHANT_ID);
        config.setHashKey(HASH_KEY);
        config.setHashIv(HASH_IV);
        config.setCheckoutUrl("https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5");
        config.setCallbackBaseUrl("https://shop.example.com/api/");
        config.setFrontendBaseUrl("https://shop.example.com");
        config.setTradeNoPrefix("SS");
        return config;
    }

    private void stubCheckout(User buyer, Order order) {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    }

    // 模擬綠界 ReturnURL 的 Form POST 內容 (含正確的 CheckMacValue)
    private Map<String, String> signedCallback(String tradeAmt, String rtnCode, String simulatePaid, String tradeNo) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("MerchantID", MERCHANT_ID);
        params.put("MerchantTradeNo", "SS1001TABC123");
        params.put("StoreID", "");
        params.put("RtnCode", rtnCode);
        params.put("RtnMsg", "1".equals(rtnCode) ? "交易成功" : "授權失敗");
        params.put("TradeNo", tradeNo);
        params.put("TradeAmt", tradeAmt);
        params.put("PaymentDate", "2026/09/15 12:05:00");
        params.put("PaymentType", "Credit_CreditCard");
        params.put("PaymentTypeChargeFee", "0");
        params.put("TradeDate", "2026/09/15 12:00:00");
        params.put("SimulatePaid", simulatePaid);
        params.put("CustomField1", String.valueOf(ORDER_ID));
        params.put("CustomField2", "");
        params.put("CustomField3", "");
        params.put("CustomField4", "");
        resign(params);
        return params;
    }

    private void resign(Map<String, String> params) {
        params.put("CheckMacValue", EcpayCheckMacValue.generate(params, HASH_KEY, HASH_IV));
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail(EMAIL);
        user.setRole(Role.CUSTOMER);
        return user;
    }

    private Order buildOrder(User owner, String totalAmount) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUser(owner);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal(totalAmount));
        order.setItems(new ArrayList<>(List.of()));
        return order;
    }

    private OrderItem buildItem(String productName, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductName(productName);
        item.setQuantity(quantity);
        return item;
    }

    private Payment buildPayment(Order order, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrder(order);
        payment.setPaymentMethod(PaymentService.PAYMENT_METHOD_ECPAY_CREDIT);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(status);
        return payment;
    }
}
