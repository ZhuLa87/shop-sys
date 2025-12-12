package com.zzowo.shop_sys.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class Order {

    // 訂單主鍵 ID（UNSIGNED，自動遞增）
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    // 下單者（多對一）
    // user_id：外鍵，指向 User.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User user;

    // 訂單總金額（UNSIGNED）
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) UNSIGNED")
    private BigDecimal totalAmount;

    // 訂單狀態，例如：
    // "PENDING"（待付款）
    // "PAID"（已付款）
    // "SHIPPED"（已出貨）
    // "COMPLETED"（完成）
    // "CANCELLED"（取消）
    private String status;

    // 使用的優惠券
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", columnDefinition = "BIGINT UNSIGNED")
    private Coupon coupon;

    // 收件人姓名
    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    // 收件人電話
    @Column(name = "recipient_phone", nullable = false)
    private String recipientPhone;

    // 收件地址
    @Column(name = "recipient_address", nullable = false)
    private String recipientAddress;

    // 訂單項目清單（與 OrderItem 建立一對多）
    // cascade = ALL：建立訂單時自動建立子項目
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

    // 訂單建立時間（不可修改）
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        // 建立訂單時自動設置建立時間
        createdAt = LocalDateTime.now();
    }
}

