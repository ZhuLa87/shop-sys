package com.zzowo.shop_sys.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// 尚未實作: 目前只對應既有 schema, 沒有任何 service 或 API 使用 (優惠券功能尚在規劃中)
@Getter
@Setter
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    // 優惠類型:"PERCENT" (百分比折扣) 或 "FIXED" (固定金額折扣) 
    @Column(name = "discount_type", nullable = false)
    private String discountType;

    // 折扣數值:若為百分比則為 0-100;若為固定折扣則為金額
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    // 最低消費金額 (0 表示無門檻) 
    @Column(name = "min_spend", precision = 10, scale = 2)
    private BigDecimal minSpend;

    // 優惠券生效時間 (可為 null = 立即生效) 
    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    // 優惠券失效時間 (可為 null = 永不過期) 
    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}