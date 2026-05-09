package com.zzowo.shop_sys.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "order_items")
public class OrderItem {

    // 訂單項目主鍵 ID（UNSIGNED、自動遞增）
    // 每筆紀錄代表該訂單中的一個商品
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所屬訂單（多對一）
    // order_id：外鍵指向訂單主鍵
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @lombok.ToString.Exclude            // 防止 toString 遞迴
    @lombok.EqualsAndHashCode.Exclude  // 防止 equals/hashCode 遞迴
    private Order order;

    // 對應的商品（多對一）
    // 保留當時下單時的商品資訊關聯
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 商品下單時的單價
    // 注意：若商品後續調價，訂單仍保留當時價格，而不是讀取 Product.price
    @Column(name = "price_at_purchase", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    // 訂購數量
    @Column(nullable = false)
    private Integer quantity;
}
