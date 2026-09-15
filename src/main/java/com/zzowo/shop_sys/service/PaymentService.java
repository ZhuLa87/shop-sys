package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.config.EcpayConfig;
import com.zzowo.shop_sys.dto.response.payment.EcpayCheckoutResponse;
import com.zzowo.shop_sys.entity.Order;
import com.zzowo.shop_sys.entity.OrderItem;
import com.zzowo.shop_sys.entity.Payment;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.OrderStatus;
import com.zzowo.shop_sys.enums.PaymentStatus;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.repository.OrderRepository;
import com.zzowo.shop_sys.repository.PaymentRepository;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.util.EcpayCheckMacValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 綠界 AIO 金流 (僅信用卡一次付清)
 * Source: https://developers.ecpay.com.tw/2862.md (產生訂單),
 *         https://developers.ecpay.com.tw/2878.md (付款結果通知),
 *         https://developers.ecpay.com.tw/2858.md (介接注意事項) (2026-09-15)
 */
@Slf4j
@Service
public class PaymentService {

    public static final String PAYMENT_METHOD_ECPAY_CREDIT = "ECPAY_CREDIT";

    static final String NOTIFY_PATH = "/v1/payments/ecpay/notify";
    static final String RESULT_PATH = "/v1/payments/ecpay/result";

    // MerchantTradeDate / PaymentDate 皆為台灣時間,伺服器不在台灣時區也必須轉換
    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter ECPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    // 官方上限 20 字元,僅允許英數字
    private static final int MERCHANT_TRADE_NO_MAX_LENGTH = 20;
    private static final Pattern ALPHANUMERIC = Pattern.compile("[A-Za-z0-9]+");

    // 官方上限 400 字元,超過時綠界截斷會切壞多位元組字元導致 CheckMacValue 不符,保留大量餘裕
    private static final int ITEM_NAME_MAX_LENGTH = 200;
    // HTML 標籤,控制字元,多品項分隔符 #,以及綠界 CDN 會阻擋的 ; | `
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern FORBIDDEN_CHARS = Pattern.compile("[#;|`\\p{Cntrl}]");

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private EcpayConfig ecpayConfig;

    public enum Outcome {
        PAID,              // 訂單已付款 (本次轉為 PAID,或先前已處理過)
        NOT_PAID,          // 付款失敗,訂單維持 PENDING
        IGNORED,           // 驗證通過但內容異常 (金額不符,模擬付款,訂單已取消等),已記錄 log
        INVALID_SIGNATURE  // CheckMacValue 驗證失敗,不可信任任何欄位
    }

    public record EcpayResult(Outcome outcome, Long orderId) {
    }

    // 建立綠界付款表單參數,每次呼叫都產生新的 MerchantTradeNo (重新付款不可沿用舊編號)
    @Transactional
    public EcpayCheckoutResponse createEcpayCheckout(String email, Long orderId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在"));

        // 付款只能由下單者本人進行 (管理員可以查看,但不代付)
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BusinessException("無權限操作此訂單", HttpStatus.FORBIDDEN);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("訂單目前狀態為 " + order.getStatus() + ",無法付款");
        }

        int totalAmount = toTwdAmount(order.getTotalAmount());
        String merchantTradeNo = generateMerchantTradeNo(order.getId());

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseGet(() -> newPayment(order));
        payment.setPaymentMethod(PAYMENT_METHOD_ECPAY_CREDIT);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.UNPAID);
        payment.setMerchantTradeNo(merchantTradeNo);
        paymentRepository.save(payment);

        // 選填參數不使用時直接省略:空字串也會被納入 CheckMacValue 計算
        Map<String, String> params = new LinkedHashMap<>();
        params.put("MerchantID", ecpayConfig.getMerchantId());
        params.put("MerchantTradeNo", merchantTradeNo);
        params.put("MerchantTradeDate", LocalDateTime.now(TAIPEI).format(ECPAY_DATE_FORMAT));
        params.put("PaymentType", "aio");
        params.put("TotalAmount", String.valueOf(totalAmount));
        params.put("TradeDesc", "shopsys order " + order.getId());
        params.put("ItemName", buildItemName(order.getItems()));
        params.put("ReturnURL", trimTrailingSlash(ecpayConfig.getCallbackBaseUrl()) + NOTIFY_PATH);
        params.put("OrderResultURL", trimTrailingSlash(ecpayConfig.getCallbackBaseUrl()) + RESULT_PATH);
        params.put("ClientBackURL", orderPageUrl(order.getId()));
        params.put("ChoosePayment", "Credit");
        params.put("EncryptType", "1");
        // callback 以此找回訂單,不依賴 MerchantTradeNo (舊分頁付款成功時,編號可能已被新的一次覆蓋)
        params.put("CustomField1", String.valueOf(order.getId()));
        params.put(EcpayCheckMacValue.FIELD,
                EcpayCheckMacValue.generate(params, ecpayConfig.getHashKey(), ecpayConfig.getHashIv()));

        return new EcpayCheckoutResponse(ecpayConfig.getCheckoutUrl(), params);
    }

    // 處理綠界付款結果 (ReturnURL 與 OrderResultURL 共用,到達順序不固定,必須冪等)
    @Transactional
    public EcpayResult handlePaymentResult(Map<String, String> params) {
        String merchantTradeNo = params.get("MerchantTradeNo");

        if (!EcpayCheckMacValue.verify(params, ecpayConfig.getHashKey(), ecpayConfig.getHashIv())) {
            log.warn("綠界回傳 CheckMacValue 驗證失敗,MerchantTradeNo={}", merchantTradeNo);
            return new EcpayResult(Outcome.INVALID_SIGNATURE, null);
        }

        if (!ecpayConfig.getMerchantId().equals(params.get("MerchantID"))) {
            log.warn("綠界回傳的 MerchantID 不符,MerchantTradeNo={},MerchantID={}", merchantTradeNo, params.get("MerchantID"));
            return new EcpayResult(Outcome.IGNORED, null);
        }

        Long orderId = parseLong(params.get("CustomField1"));
        Optional<Order> found = orderId == null ? Optional.empty() : orderRepository.findByIdForUpdate(orderId);
        if (found.isEmpty()) {
            log.warn("綠界回傳找不到對應訂單,MerchantTradeNo={},CustomField1={}", merchantTradeNo, params.get("CustomField1"));
            return new EcpayResult(Outcome.IGNORED, null);
        }
        Order order = found.get();

        // SimulatePaid=1 是綠界後台的模擬付款,不會實際撥款,不可當作已付款
        if ("1".equals(params.get("SimulatePaid"))) {
            log.warn("收到綠界模擬付款通知,不更新訂單,orderId={},MerchantTradeNo={}", orderId, merchantTradeNo);
            return new EcpayResult(Outcome.IGNORED, orderId);
        }

        Long tradeAmt = parseLong(params.get("TradeAmt"));
        if (tradeAmt == null || BigDecimal.valueOf(tradeAmt).compareTo(order.getTotalAmount()) != 0) {
            log.warn("綠界回傳金額與訂單不符,orderId={},TradeAmt={},訂單金額={}", orderId, params.get("TradeAmt"), order.getTotalAmount());
            return new EcpayResult(Outcome.IGNORED, orderId);
        }

        Payment payment = paymentRepository.findByOrderId(orderId).orElseGet(() -> newPayment(order));
        String tradeNo = params.get("TradeNo");

        // Form POST 收到的 RtnCode 是字串,1 以外皆為失敗
        if (!"1".equals(params.get("RtnCode"))) {
            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
            log.info("綠界付款失敗,orderId={},MerchantTradeNo={},RtnCode={},RtnMsg={}",
                    orderId, merchantTradeNo, params.get("RtnCode"), params.get("RtnMsg"));
            return new EcpayResult(order.getStatus() == OrderStatus.PENDING ? Outcome.NOT_PAID : Outcome.PAID, orderId);
        }

        switch (order.getStatus()) {
            case PENDING -> {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);

                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setTransactionId(tradeNo);
                payment.setMerchantTradeNo(merchantTradeNo);
                payment.setPaymentMethod(Optional.ofNullable(params.get("PaymentType")).orElse(PAYMENT_METHOD_ECPAY_CREDIT));
                payment.setPaidAt(parsePaymentDate(params.get("PaymentDate")));
                paymentRepository.save(payment);

                log.info("綠界付款成功,orderId={},MerchantTradeNo={},TradeNo={}", orderId, merchantTradeNo, tradeNo);
                return new EcpayResult(Outcome.PAID, orderId);
            }
            case CANCELLED -> {
                log.warn("已取消的訂單收到付款成功通知,需人工退款,orderId={},MerchantTradeNo={},TradeNo={}", orderId, merchantTradeNo, tradeNo);
                return new EcpayResult(Outcome.IGNORED, orderId);
            }
            default -> {
                // 已付款:同一筆交易重送 (或 ReturnURL/OrderResultURL 各到一次) 直接視為成功
                if (tradeNo != null && !tradeNo.equals(payment.getTransactionId())) {
                    log.warn("訂單重複付款,需人工退款,orderId={},既有 TradeNo={},新 TradeNo={},MerchantTradeNo={}",
                            orderId, payment.getTransactionId(), tradeNo, merchantTradeNo);
                }
                return new EcpayResult(Outcome.PAID, orderId);
            }
        }
    }

    // OrderResultURL 處理完後,把消費者的瀏覽器導回前端的網址
    public String resultRedirectUrl(EcpayResult result) {
        if (result.orderId() == null) {
            return trimTrailingSlash(ecpayConfig.getFrontendBaseUrl()) + "/orders?payment=error";
        }
        String payment = result.outcome() == Outcome.PAID ? "success" : "failed";
        return orderPageUrl(result.orderId()) + "?payment=" + payment;
    }

    // 綠界只收新台幣正整數
    private int toTwdAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("訂單金額必須大於 0");
        }
        if (amount.stripTrailingZeros().scale() > 0) {
            throw new BusinessException("訂單金額含小數,無法以新台幣付款");
        }
        return amount.intValueExact();
    }

    // 格式:{prefix}{orderId}T{毫秒時間戳 base36},例如 SS1001TMFKX2A9C
    private String generateMerchantTradeNo(Long orderId) {
        String tradeNo = ecpayConfig.getTradeNoPrefix() + orderId + "T"
                + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT);
        if (tradeNo.length() > MERCHANT_TRADE_NO_MAX_LENGTH || !ALPHANUMERIC.matcher(tradeNo).matches()) {
            throw new IllegalStateException("MerchantTradeNo 格式不符 (最長 20 字元,僅英數字): " + tradeNo);
        }
        return tradeNo;
    }

    // 品名 x數量,多品項以 # 分隔
    private String buildItemName(List<OrderItem> items) {
        String itemName = items == null ? "" : items.stream()
                .map(item -> sanitizeItemName(item.getProductName()) + " x" + item.getQuantity())
                .collect(Collectors.joining("#"));
        if (itemName.isEmpty()) {
            return "商品";
        }
        if (itemName.length() <= ITEM_NAME_MAX_LENGTH) {
            return itemName;
        }
        // 避免把 surrogate pair (如 emoji) 切成一半
        int end = Character.isHighSurrogate(itemName.charAt(ITEM_NAME_MAX_LENGTH - 1))
                ? ITEM_NAME_MAX_LENGTH - 1 : ITEM_NAME_MAX_LENGTH;
        return itemName.substring(0, end);
    }

    private String sanitizeItemName(String name) {
        if (name == null) return "商品";
        String cleaned = FORBIDDEN_CHARS.matcher(HTML_TAG.matcher(name).replaceAll("")).replaceAll(" ").trim();
        return cleaned.isEmpty() ? "商品" : cleaned;
    }

    private Payment newPayment(Order order) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(PAYMENT_METHOD_ECPAY_CREDIT);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.UNPAID);
        return payment;
    }

    private LocalDateTime parsePaymentDate(String paymentDate) {
        if (paymentDate != null) {
            try {
                return LocalDateTime.parse(paymentDate, ECPAY_DATE_FORMAT);
            } catch (DateTimeParseException e) {
                log.warn("無法解析綠界 PaymentDate: {}", paymentDate);
            }
        }
        return LocalDateTime.now(TAIPEI);
    }

    private String orderPageUrl(Long orderId) {
        return trimTrailingSlash(ecpayConfig.getFrontendBaseUrl()) + "/orders/" + orderId;
    }

    private static Long parseLong(String value) {
        if (value == null) return null;
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
