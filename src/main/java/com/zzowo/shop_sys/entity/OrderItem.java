package com.zzowo.shop_sys.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // @NotFound(IGNORE): 商品軟刪除後 @SQLRestriction 會讓 JPA 找不到該列,
    // 加上此注解使 Hibernate 回傳 null 而非拋出 ObjectNotFoundException
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private Product product;

    // 下單當時的商品名稱快照 (商品日後改名或刪除仍可正確顯示)
    @Column(name = "product_name", nullable = false)
    private String productName;

    // 下單當時的封面圖片 URL 快照
    @Column(name = "cover_image_url")
    private String coverImageUrl;

    // 注意:若商品後續調價,訂單仍保留當時價格,而不是讀取 Product.price
    @Column(name = "price_at_purchase", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    @Column(nullable = false)
    private Integer quantity;
}
