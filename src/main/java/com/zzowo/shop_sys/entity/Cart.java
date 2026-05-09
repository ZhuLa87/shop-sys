package com.zzowo.shop_sys.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "carts")
public class Cart {

    // 購物車項目的主鍵 ID（UNSIGNED，自動遞增）
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所屬會員（多對一）
    // user_id：外鍵，連結到 User 的 id
    // LAZY：需要時才載入使用者資訊
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 所屬商品（多對一）
    // product_id：外鍵，連結到 Product 的 id
    // LAZY：避免一次載入所有商品資料提升效能
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 購買數量（預設至少為 1）
    @Column(nullable = false)
    private Integer quantity;

    // 新增購物車項目的時間（建立後不可修改）
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 最後更新時間
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        // 建立時自動設定時間戳
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        // 更新時自動刷新 updatedAt
        updatedAt = LocalDateTime.now();
    }
}