package com.zzowo.shop_sys.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "shipments")
public class Shipment {

    // 主鍵 ID，採用自動遞增，使用 UNSIGNED BIGINT
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 此出貨資訊所對應的訂單（一對一關係），延遲載入
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 物流追蹤編號
    @Column(name = "tracking_number")
    private String trackingNumber;

    // 物流運送公司名稱
    @Column(name = "logistics_provider")
    private String logisticsProvider;

    // 出貨狀態，如 "PREPARING", "SHIPPED", "DELIVERED"
    private String status;

    // 出貨時間
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    // 配送完成時間
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}
