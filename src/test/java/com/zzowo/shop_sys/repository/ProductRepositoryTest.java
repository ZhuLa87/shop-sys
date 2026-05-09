package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProductRepository productRepository;

    @Test
    void save_andFind_success() {
        Product p = new Product();
        p.setName("測試商品");
        p.setPrice(BigDecimal.valueOf(100));
        p.setStockQuantity(10);
        p.setStatus(ProductStatus.ON_SHELF);

        em.persistAndFlush(p);

        List<Product> all = productRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getName()).isEqualTo("測試商品");
        assertThat(all.get(0).getPrice()).isEqualByComparingTo("100");
    }
}
