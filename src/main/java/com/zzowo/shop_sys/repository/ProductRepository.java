package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 搜尋名稱含有 keyword 的商品 (類似 SQL 的 LIKE %keyword%)
    List<Product> findByNameContaining(String keyword);

    // 找出所有狀態為 status 的商品 (例如找所有 "ON_SHELF" 的商品)
    List<Product> findByStatus(String status);
}