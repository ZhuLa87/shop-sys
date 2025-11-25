package com.zzowo.shop_sys.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "coupons")
public class Coupon {

    // 優惠券主鍵 ID（UNSIGNED，自動遞增）
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    // 優惠券代碼（不可重複，用於兌換識別）
    @Column(nullable = false, unique = true)
    private String code;

    // 優惠類型："PERCENT"（百分比折扣）或 "FIXED"（固定金額折扣）
    @Column(name = "discount_type", nullable = false)
    private String discountType; // "PERCENT", "FIXED"

    // 折扣數值：若為百分比則為 0-100；若為固定折扣則為金額
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) UNSIGNED")
    private BigDecimal discountValue;

    // 最低消費金額（0 表示無門檻）
    @Column(name = "min_spend", precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) UNSIGNED DEFAULT 0")
    private BigDecimal minSpend;

    // 優惠券生效時間（可為 null = 立即生效）
    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    // 優惠券失效時間（可為 null = 永不過期）
    @Column(name = "valid_to")
    private LocalDateTime validTo;

    // 使用總次數限制（UNSIGNED，預設 1 次）
    @Column(name = "usage_limit", columnDefinition = "INT UNSIGNED DEFAULT 1")
    private Integer usageLimit;

    // 建立時間（建立後不可修改）
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        // 建立資料時自動設定建立時間
        createdAt = LocalDateTime.now();
    }
}