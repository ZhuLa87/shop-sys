package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.ProductImage;
import com.zzowo.shop_sys.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

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
        Product p = new Product();
        p.setName(name);
        p.setPrice(BigDecimal.valueOf(100));
        p.setStockQuantity(10);
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
