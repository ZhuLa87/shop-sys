package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.ProductImage;
import com.zzowo.shop_sys.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ProductRepository productRepository;

    @Test
    void save_andFind_success() {
        em.persistAndFlush(buildProduct("測試商品", ProductStatus.ON_SHELF));

        List<Product> all = productRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getName()).isEqualTo("測試商品");
        assertThat(all.get(0).getPrice()).isEqualByComparingTo("100");
    }

    @Test
    void findByStatus_returnsOnlyMatchingProducts() {
        em.persistAndFlush(buildProduct("上架A", ProductStatus.ON_SHELF));
        em.persistAndFlush(buildProduct("上架B", ProductStatus.ON_SHELF));
        em.persistAndFlush(buildProduct("下架C", ProductStatus.OFF_SHELF));

        List<Product> result = productRepository.findByStatus(ProductStatus.ON_SHELF);

        assertThat(result).hasSize(2)
                .extracting("name")
                .containsExactlyInAnyOrder("上架A", "上架B");
    }

    // ── storefront listing ───────────────────────────────────────────────────

    private static final List<ProductStatus> STOREFRONT = List.of(ProductStatus.ON_SHELF, ProductStatus.OUT_OF_STOCK);

    @Test
    void findByStatusInSoldOutLast_includesOutOfStock_excludesOffShelf_andPutsSoldOutLast() {
        em.persist(buildProduct("上架售完", ProductStatus.ON_SHELF, 0));
        em.persist(buildProduct("缺貨中", ProductStatus.OUT_OF_STOCK, 0));
        em.persist(buildProduct("缺貨但有庫存", ProductStatus.OUT_OF_STOCK, 5));
        em.persist(buildProduct("可購買", ProductStatus.ON_SHELF, 10));
        em.persist(buildProduct("已下架", ProductStatus.OFF_SHELF, 10));
        em.flush();

        Page<Product> result = productRepository.findByStatusInSoldOutLast(STOREFRONT, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent()).extracting("name").doesNotContain("已下架");
        // 只有「上架且有庫存」排前面,OUT_OF_STOCK 即使庫存 > 0 也排在後面
        assertThat(result.getContent().get(0).getName()).isEqualTo("可購買");
        assertThat(result.getContent().subList(1, 4)).extracting("name")
                .containsExactlyInAnyOrder("上架售完", "缺貨中", "缺貨但有庫存");
    }

    @Test
    void findByStatusInAndNameContainingSoldOutLast_filtersByKeywordAndStatus() {
        em.persist(buildProduct("限量版電競滑鼠", ProductStatus.OUT_OF_STOCK, 0));
        em.persist(buildProduct("無線電競滑鼠", ProductStatus.ON_SHELF, 10));
        em.persist(buildProduct("舊款電競滑鼠", ProductStatus.OFF_SHELF, 10));
        em.persist(buildProduct("機械鍵盤", ProductStatus.ON_SHELF, 10));
        em.flush();

        Page<Product> result = productRepository.findByStatusInAndNameContainingSoldOutLast(
                STOREFRONT, "電競滑鼠", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting("name")
                .containsExactly("無線電競滑鼠", "限量版電競滑鼠");
    }

    @Test
    void findByIdWithImages_returnsProductWithImagesLoaded() {
        Product p = buildProduct("帶圖商品", ProductStatus.ON_SHELF);
        em.persist(p);
        em.persist(buildImage(p, "http://example.com/1.jpg", 0));
        em.persist(buildImage(p, "http://example.com/2.jpg", 1));
        em.flush();
        em.clear();

        Optional<Product> result = productRepository.findByIdWithImages(p.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getImages()).hasSize(2)
                .extracting("imageUrl")
                .containsExactlyInAnyOrder(
                        "http://example.com/1.jpg",
                        "http://example.com/2.jpg"
                );
    }

    @Test
    void findByIdWithImages_productWithNoImages_returnsEmptyList() {
        Product p = buildProduct("無圖商品", ProductStatus.ON_SHELF);
        em.persistAndFlush(p);
        em.clear();

        Optional<Product> result = productRepository.findByIdWithImages(p.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getImages()).isEmpty();
    }

    // ── soft delete ──────────────────────────────────────────────────────────

    @Test
    void sqlRestriction_excludesSoftDeletedFromNormalQueries() {
        Product active = buildProduct("正常商品", ProductStatus.ON_SHELF);
        Product deleted = buildProduct("已刪除商品", ProductStatus.ON_SHELF);
        deleted.setDeletedAt(LocalDateTime.now());
        em.persistAndFlush(active);
        em.persistAndFlush(deleted);
        em.clear();

        List<Product> all = productRepository.findAll();
        Optional<Product> byId = productRepository.findById(deleted.getId());

        assertThat(all).hasSize(1).extracting("name").containsExactly("正常商品");
        assertThat(byId).isEmpty();
    }

    @Test
    void findAllDeleted_returnsOnlySoftDeletedProducts() {
        Product active = buildProduct("正常商品", ProductStatus.ON_SHELF);
        Product d1 = buildProduct("刪除商品A", ProductStatus.ON_SHELF);
        Product d2 = buildProduct("刪除商品B", ProductStatus.OFF_SHELF);
        d1.setDeletedAt(LocalDateTime.now());
        d2.setDeletedAt(LocalDateTime.now());
        em.persistAndFlush(active);
        em.persistAndFlush(d1);
        em.persistAndFlush(d2);
        em.clear();

        List<Product> result = productRepository.findAllDeleted();

        assertThat(result).hasSize(2)
                .extracting("name")
                .containsExactlyInAnyOrder("刪除商品A", "刪除商品B");
    }

    @Test
    void findDeletedById_returnsDeletedProductWithNonNullDeletedAt() {
        Product p = buildProduct("已刪除商品", ProductStatus.ON_SHELF);
        p.setDeletedAt(LocalDateTime.now());
        em.persistAndFlush(p);
        em.clear();

        Optional<Product> result = productRepository.findDeletedById(p.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getDeletedAt()).isNotNull();
        assertThat(result.get().getName()).isEqualTo("已刪除商品");
    }

    @Test
    void findDeletedById_activeProduct_returnsEmpty() {
        Product p = buildProduct("正常商品", ProductStatus.ON_SHELF);
        em.persistAndFlush(p);

        Optional<Product> result = productRepository.findDeletedById(p.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void restore_clearedDeletedAt_reappearsInNormalQueries() {
        Product p = buildProduct("恢復商品", ProductStatus.ON_SHELF);
        p.setDeletedAt(LocalDateTime.now());
        em.persistAndFlush(p);
        em.clear();

        // 用 findDeletedById 取出,清除 deletedAt,儲存
        Product deleted = productRepository.findDeletedById(p.getId()).orElseThrow();
        deleted.setDeletedAt(null);
        productRepository.saveAndFlush(deleted);
        em.clear();

        assertThat(productRepository.findById(p.getId())).isPresent();
        assertThat(productRepository.findAllDeleted()).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Product buildProduct(String name, ProductStatus status) {
        return buildProduct(name, status, 10);
    }

    private Product buildProduct(String name, ProductStatus status, int stock) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(BigDecimal.valueOf(100));
        p.setStockQuantity(stock);
        p.setStatus(status);
        return p;
    }

    private ProductImage buildImage(Product product, String url, int sortOrder) {
        ProductImage img = new ProductImage();
        img.setProduct(product);
        img.setImageUrl(url);
        img.setSortOrder(sortOrder);
        return img;
    }
}
