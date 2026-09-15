package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 找出某個用戶的所有訂單
    List<Order> findByUserId(Long userId);

    // 找出某個用戶的所有訂單,並依照建立時間新到舊排序
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 取得訂單並加上寫入鎖 (SELECT ... FOR UPDATE)
    // 綠界的 ReturnURL 與 OrderResultURL 可能同時到達,需序列化處理避免重複入帳
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}