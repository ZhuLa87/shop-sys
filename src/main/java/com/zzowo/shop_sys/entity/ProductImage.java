package com.zzowo.shop_sys.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "product_images")
public class ProductImage {

    // 商品圖片主鍵 ID（自動遞增、UNSIGNED）
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所屬商品（多對一關係）
    // LAZY：需要使用時才載入商品資料
    // product_id：外鍵，連到產品的主鍵
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @lombok.ToString.Exclude            // 防止 toString 遞迴
    @lombok.EqualsAndHashCode.Exclude  // 防止 equals/hashCode 遞迴
    private Product product;

    // 圖片的 URL 位置
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    // 圖片排序（UNSIGNED，預設值 0，用於決定顯示順序）
    @Column(name = "sort_order")
    private Integer sortOrder;

}
