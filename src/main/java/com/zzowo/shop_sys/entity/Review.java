package com.zzowo.shop_sys.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

// 尚未實作: 目前只對應既有 schema, 沒有任何 service 或 API 使用 (商品評論功能尚在規劃中)
@Getter
@Setter
@Entity
@Table(name = "reviews")
public class Review {

    // 主鍵 ID,自動遞增,使用 UNSIGNED BIGINT 儲存
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 評論所屬的使用者 (多對一關係) ,延遲載入
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 評論所屬的商品 (多對一關係) ,延遲載入
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 評分 (整數) ,不可為 null
    @Column(nullable = false)
    private Integer rating;

    // 評論內容,可為 null
    @Column(length = 65535)
    private String comment;

    // 建立時間,建立後不可更新
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 在資料儲存前自動設定建立時間
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
