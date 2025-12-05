package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.enums.ProductStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 搜尋名稱含有 keyword 的商品 (類似 SQL 的 LIKE %keyword%)
    List<Product> findByNameContaining(String keyword);

    // 找出所有狀態為 status 的商品 (例如找所有 "ON_SHELF" 的商品)
    List<Product> findByStatus(ProductStatus status);

    // JOIN FETCH: 告訴 JPA 查詢 Product 時，順便把 images 關聯表抓出來填好
    // LEFT JOIN: 就算商品沒有圖片，商品本身也要查出來 (避免因沒圖片導致商品消失)
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);
}