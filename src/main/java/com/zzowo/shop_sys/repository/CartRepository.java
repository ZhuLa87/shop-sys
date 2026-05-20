package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    // 找出某用戶的購物車清單
    @Query("SELECT c FROM Cart c JOIN FETCH c.product WHERE c.user.id = :userId")
    List<Cart> findByUserId(Long userId);

    // 找出某用戶購物車內是否有特定商品 (用來檢查是否要新增還是更新數量)
    Optional<Cart> findByUserIdAndProductId(Long userId, Long productId);

    // 清空某用戶的購物車 (結帳後使用,直接 DELETE 避免逐筆 SELECT+DELETE)
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}