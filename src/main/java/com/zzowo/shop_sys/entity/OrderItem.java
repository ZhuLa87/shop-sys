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
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    // 所屬訂單（多對一）
    // order_id：外鍵指向訂單主鍵
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Order order;

    // 對應的商品（多對一）
    // 保留當時下單時的商品資訊關聯
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Product product;

    // 商品下單時的單價（UNSIGNED）
    // 注意：若商品後續調價，訂單仍保留當時價格，而不是讀取 Product.price
    @Column(name = "price_at_purchase", nullable = false, precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) UNSIGNED")
    private BigDecimal priceAtPurchase;

    // 訂購數量（UNSIGNED）
    // 與 priceAtPurchase 一起用於計算該項目的小計金額
    @Column(nullable = false, columnDefinition = "INT UNSIGNED")
    private Integer quantity;
}
