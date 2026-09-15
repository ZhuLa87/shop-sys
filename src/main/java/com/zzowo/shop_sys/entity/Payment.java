package com.zzowo.shop_sys.entity;

import com.zzowo.shop_sys.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
public class Payment {

    // 主鍵 ID,使用自動遞增策略,資料庫欄位型態為 UNSIGNED BIGINT
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 與 Order 表一對一關聯
    // 使用 Lazy Loading,避免每次載入 Payment 都立即載入 Order
    // order_id 為外鍵,且不可為 null
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 支付方式,例如:credit_card,line_pay,bank_transfer 等
    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    // 第三方支付平台或金流服務提供的交易編號,可為 null (尚未付款時)
    // 綠界:TradeNo
    @Column(name = "transaction_id")
    private String transactionId;

    // 送給綠界的特店訂單編號 (MerchantTradeNo),每次重新付款都會換一組
    @Column(name = "merchant_trade_no", length = 20, unique = true)
    private String merchantTradeNo;

    // 付款金額,最多 10 位數其中 2 位小數,不可為負且不可為 null
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // 支付狀態
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    // 支付完成時間,僅在付款成功時有值
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
