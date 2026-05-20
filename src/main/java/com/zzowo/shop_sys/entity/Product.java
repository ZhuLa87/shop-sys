package com.zzowo.shop_sys.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.zzowo.shop_sys.enums.ProductStatus;

@Data
@Entity
@Table(name = "products")
public class Product {

    // 主鍵
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 名稱
    @Column(nullable = false)
    private String name;

    // 描述
    @Column(length = 65535)
    private String description;

    // 價格 (最多 10 位數,2 位小數,不可為負) 
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // 庫存數量 (預設值為 0) 
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    // 商品狀態,例如 "ON_SHELF" (上架) ,"OFF_SHELF" (下架) 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    // 封面圖片 URL
    @Column(name = "cover_image_url")
    private String coverImageUrl;

    // 圖片清單 (與 ProductImage 建立一對多關聯) 
    // cascade = ALL:操作商品時同步操作圖片
    // orphanRemoval = true:移除圖片關聯時會自動刪除資料庫中的圖片記錄
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;

    // 建立時間 (建立後不可修改) 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 最後更新時間
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 樂觀鎖版本號,防止併發修改衝突
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        // 新增資料時自動填入建立與更新時間
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        // 更新資料時自動更新 updatedAt
        updatedAt = LocalDateTime.now();
    }
}
