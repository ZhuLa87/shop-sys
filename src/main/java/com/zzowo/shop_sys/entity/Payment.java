package com.zzowo.shop_sys.entity;

import com.zzowo.shop_sys.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    // 綠界:TradeNo
    @Column(name = "transaction_id")
    private String transactionId;

    // 送給綠界的特店訂單編號 (MerchantTradeNo),每次重新付款都會換一組
    @Column(name = "merchant_trade_no", length = 20, unique = true)
    private String merchantTradeNo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
