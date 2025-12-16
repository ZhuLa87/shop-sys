package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    // 查看某商品的庫存紀錄
    @Query("SELECT l FROM InventoryLog l JOIN FETCH l.product WHERE l.product.id = :productId ORDER BY l.createdAt DESC")
    List<InventoryLog> findByProductIdOrderByCreatedAtDesc(Long productId);

    // 查看所有庫存紀錄 (依建立時間倒序)
    @Query("SELECT l FROM InventoryLog l JOIN FETCH l.product ORDER BY l.createdAt DESC")
    List<InventoryLog> findAllByOrderByCreatedAtDesc();
}