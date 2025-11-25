package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    // 查看某商品的庫存紀錄
    List<InventoryLog> findByProductIdOrderByCreatedAtDesc(Long productId);
}