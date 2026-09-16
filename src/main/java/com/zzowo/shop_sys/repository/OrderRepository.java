package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.Order;
import com.zzowo.shop_sys.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    // 撈出逾時未付款的訂單 id (分批,避免一次載入過多)
    @Query("SELECT o.id FROM Order o WHERE o.status = :status AND o.createdAt < :cutoff ORDER BY o.id")
    List<Long> findIdsByStatusAndCreatedAtBefore(@Param("status") OrderStatus status,
                                                 @Param("cutoff") LocalDateTime cutoff,
                                                 Pageable pageable);

    // 條件式取消: 只有狀態仍為 PENDING 時才會成功 (回傳 1)
    // 多實例同時跑排程,或與付款回呼競爭時,保證只有一方會回補庫存
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Order o SET o.status = :cancelled WHERE o.id = :id AND o.status = :pending")
    int cancelIfPending(@Param("id") Long id,
                        @Param("pending") OrderStatus pending,
                        @Param("cancelled") OrderStatus cancelled);
}