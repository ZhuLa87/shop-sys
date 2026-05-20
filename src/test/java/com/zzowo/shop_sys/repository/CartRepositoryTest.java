package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.Cart;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CartRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired CartRepository cartRepository;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = em.persist(buildUser("buyer@test.com"));
        product = em.persist(buildProduct("商品A"));
        em.flush();
    }

    @Test
    void findByUserId_returnsCartsWithProductLoaded() {
        em.persist(buildCart(user, product, 2));
        em.persist(buildCart(user, product, 1));
        em.flush();
        em.clear();

        List<Cart> result = cartRepository.findByUserId(user.getId());

        assertThat(result).hasSize(2);
        // JOIN FETCH 確認 product 已載入,不是 proxy
        assertThat(result.get(0).getProduct().getName()).isEqualTo("商品A");
    }

    @Test
    void findByUserId_doesNotReturnOtherUsersCart() {
        User other = em.persistAndFlush(buildUser("other@test.com"));
        em.persist(buildCart(user, product, 1));
        em.persist(buildCart(other, product, 1));
        em.flush();

        List<Cart> result = cartRepository.findByUserId(user.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void findByUserIdAndProductId_returnsExistingItem() {
        em.persistAndFlush(buildCart(user, product, 3));

        Optional<Cart> result = cartRepository.findByUserIdAndProductId(user.getId(), product.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getQuantity()).isEqualTo(3);
    }

    @Test
    void deleteByUserId_removesOnlyTargetUserCarts() {
        User other = em.persistAndFlush(buildUser("other@test.com"));
        em.persist(buildCart(user, product, 1));
        em.persist(buildCart(user, product, 2));
        em.persist(buildCart(other, product, 1));
        em.flush();

        cartRepository.deleteByUserId(user.getId());
        em.flush();
        em.clear();

        assertThat(cartRepository.findByUserId(user.getId())).isEmpty();
        assertThat(cartRepository.findByUserId(other.getId())).hasSize(1);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User buildUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setRole(Role.CUSTOMER);
        return u;
    }

    private Product buildProduct(String name) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(BigDecimal.valueOf(500));
        p.setStockQuantity(20);
        p.setStatus(ProductStatus.ON_SHELF);
        return p;
    }

    private Cart buildCart(User u, Product p, int quantity) {
        Cart c = new Cart();
        c.setUser(u);
        c.setProduct(p);
        c.setQuantity(quantity);
        return c;
    }
}
