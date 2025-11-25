package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 找出某個用戶的所有訂單
    List<Order> findByUserId(Long userId);

    // 找出某個用戶的所有訂單，並依照建立時間新到舊排序
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}