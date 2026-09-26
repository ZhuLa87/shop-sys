package com.zzowo.shop_sys.entity;

import com.zzowo.shop_sys.enums.InventoryChangeReason;
import com.zzowo.shop_sys.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "inventory_logs")
public class InventoryLog {

    // 庫存異動紀錄主鍵 ID (UNSIGNED,自動遞增) 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所屬商品 (多對一關聯)
    // @NotFound(IGNORE): 商品軟刪除後 @SQLRestriction 使 JPA 找不到該列,
    // 加上此注解使 Hibernate 回傳 null 而非拋出 ObjectNotFoundException
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Product product;

    // 庫存變動數量:正數＝補貨,負數＝出貨或退貨扣除
    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount;


    // 異動原因
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryChangeReason reason;

    // 操作人員 ID (執行此異動的人,可選,可能是後台管理員或系統) 
    @Column(name = "operator_id")
    private Long operatorId;

    // 操作人員名稱快照 (寫入當下的姓名,使用者改名或軟刪除後仍保留當時身分) 
    @Column(name = "operator_name", length = 100)
    private String operatorName;

    // 操作人員角色快照 (寫入當下的角色,日後調整權限不影響歷史紀錄) 
    @Enumerated(EnumType.STRING)
    @Column(name = "operator_role", length = 32)
    private Role operatorRole;

    // 異動建立時間 (不可更改) 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        // 紀錄建立時自動填入時間
        createdAt = LocalDateTime.now();
    }

    // 寫入當下的操作者身分快照;operator 為 null 代表系統自動作業,三個欄位都留 null
    public void applyOperator(User operator) {
        if (operator == null) {
            return;
        }
        this.operatorId = operator.getId();
        this.operatorName = (operator.getName() == null || operator.getName().isBlank())
                ? operator.getEmail()
                : operator.getName();
        this.operatorRole = operator.getRole();
    }
}