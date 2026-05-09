package com.zzowo.shop_sys.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_logs")
public class InventoryLog {

    // 庫存異動紀錄主鍵 ID（UNSIGNED，自動遞增）
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所屬商品（多對一關聯）
    // product_id 為外鍵，用來指向被異動的商品
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 庫存變動數量：正數＝補貨，負數＝出貨或退貨扣除
    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount;


    /**
     * 異動原因，例如：
     *
     * "RESTOCK"（進貨）
     * "ORDER"（出貨）
     * "ADJUSTMENT"（人工調整）
     * "CANCEL"（訂單取消回庫）
     * "RETURN"（退貨增加庫存）
     */
    @Column(nullable = false)
    private String reason;

    // 操作人員 ID（執行此異動的人，可選，可能是後台管理員或系統）
    @Column(name = "operator_id")
    private Long operatorId;

    // 異動建立時間（不可更改）
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        // 紀錄建立時自動填入時間
        createdAt = LocalDateTime.now();
    }
}