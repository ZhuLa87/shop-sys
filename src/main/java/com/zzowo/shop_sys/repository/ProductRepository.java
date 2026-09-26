package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.enums.ProductStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 搜尋名稱含有 keyword 的商品 (類似 SQL 的 LIKE %keyword%)
    List<Product> findByNameContaining(String keyword);

    // 找出所有狀態為 status 的商品 (例如找所有 "ON_SHELF" 的商品)
    List<Product> findByStatus(ProductStatus status);

    // 分頁查詢上架商品
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    // 分頁查詢上架商品 + 名稱關鍵字搜尋
    Page<Product> findByStatusAndNameContaining(ProductStatus status, String keyword, Pageable pageable);

    // 管理員: 分頁查詢全部商品 (含關鍵字,不限狀態)
    Page<Product> findByNameContaining(String keyword, Pageable pageable);

    // 前台列表: 分頁查詢多種狀態的商品,可購買 (上架且有庫存) 的排前面,缺貨 (庫存為 0 或狀態為 OUT_OF_STOCK) 的排最後
    @Query("SELECT p FROM Product p WHERE p.status IN :statuses ORDER BY CASE WHEN p.status = com.zzowo.shop_sys.enums.ProductStatus.ON_SHELF AND p.stockQuantity > 0 THEN 0 ELSE 1 END ASC")
    Page<Product> findByStatusInSoldOutLast(@Param("statuses") Collection<ProductStatus> statuses, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status IN :statuses AND p.name LIKE %:keyword% ORDER BY CASE WHEN p.status = com.zzowo.shop_sys.enums.ProductStatus.ON_SHELF AND p.stockQuantity > 0 THEN 0 ELSE 1 END ASC")
    Page<Product> findByStatusInAndNameContainingSoldOutLast(@Param("statuses") Collection<ProductStatus> statuses, @Param("keyword") String keyword, Pageable pageable);

    // JOIN FETCH: 告訴 JPA 查詢 Product 時,順便把 images 關聯表抓出來填好
    // LEFT JOIN: 就算商品沒有圖片,商品本身也要查出來 (避免因沒圖片導致商品消失)
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);

    // 原子扣庫存: 庫存不足時條件不成立,回傳 0 筆,由呼叫端判定為庫存不足
    // version + 1 讓後台整包覆寫商品時仍會觸發樂觀鎖衝突,避免庫存被舊表單值還原
    // :qty > 0 擋下非正數: 負數扣減等於憑空增加庫存
    // deleted_at IS NULL 明確寫出,不倚賴 @SQLRestriction 是否作用於 bulk update
    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE products SET stock_quantity = stock_quantity - :qty, version = version + 1 " +
                   "WHERE id = :id AND :qty > 0 AND stock_quantity >= :qty AND deleted_at IS NULL",
           nativeQuery = true)
    int deductStock(@Param("id") Long id, @Param("qty") int qty);

    // 原子回補庫存 (訂單取消) ; 商品已軟刪除時回傳 0,由呼叫端記錄 log
    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE products SET stock_quantity = stock_quantity + :qty, version = version + 1 " +
                   "WHERE id = :id AND deleted_at IS NULL",
           nativeQuery = true)
    int restoreStock(@Param("id") Long id, @Param("qty") int qty);

    // 以下兩個 native query 刻意繞過 @SQLRestriction,專供軟刪除回收桶使用
    @Query(value = "SELECT * FROM products WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC",
           nativeQuery = true)
    List<Product> findAllDeleted();

    @Query(value = "SELECT * FROM products WHERE id = :id AND deleted_at IS NOT NULL",
           nativeQuery = true)
    Optional<Product> findDeletedById(@Param("id") Long id);
}