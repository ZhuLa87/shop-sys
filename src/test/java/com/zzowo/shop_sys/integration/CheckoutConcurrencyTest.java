package com.zzowo.shop_sys.integration;

import com.zzowo.shop_sys.dto.request.order.OrderCreateRequest;
import com.zzowo.shop_sys.entity.Cart;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.repository.CartRepository;
import com.zzowo.shop_sys.repository.ProductRepository;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 用真正的 MariaDB 驗證結帳不會超賣 (container 設定見 AbstractMariaDbIntegrationTest)
class CheckoutConcurrencyTest extends AbstractMariaDbIntegrationTest {

    // 不能超過 AbstractMariaDbIntegrationTest 設定的連線池大小
    private static final int THREADS = 40;

    @Autowired OrderService orderService;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    @Autowired CartRepository cartRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void concurrentCheckout_singleProduct_neverOversells() throws Exception {
        // given: 庫存 10, 40 位使用者的購物車各放 1 件
        Product product = createProduct("限量商品", 10);
        List<User> buyers = createBuyers(THREADS);
        for (User buyer : buyers) {
            addToCart(buyer, product, 1);
        }

        // when: 同時結帳
        CheckoutResults results = checkoutConcurrently(buyers);

        // then: 剛好賣出 10 件, 其餘都是庫存不足
        assertThat(results.unexpected()).isEmpty();
        assertThat(results.succeeded()).isEqualTo(10);
        assertThat(results.outOfStock()).isEqualTo(THREADS - 10);

        assertThat(stockOf(product)).isZero();
        assertThat(soldQuantityOf(product)).isEqualTo(10);
        assertThat(inventoryChangeOf(product)).isEqualTo(-10);
        // 失敗的交易整筆回滾, 購物車沒有被清空
        assertThat(remainingCartItems(buyers)).isEqualTo(THREADS - 10);
    }

    @Test
    void concurrentCheckout_multipleUnits_neverOversells() throws Exception {
        // given: 庫存 10, 每人買 3 件. 最多成交 3 筆 (9 件), 剩下 1 件不夠任何人買
        Product product = createProduct("一次買三件", 10);
        List<User> buyers = createBuyers(THREADS);
        for (User buyer : buyers) {
            addToCart(buyer, product, 3);
        }

        CheckoutResults results = checkoutConcurrently(buyers);

        assertThat(results.unexpected()).isEmpty();
        assertThat(results.succeeded()).isEqualTo(3);
        assertThat(stockOf(product)).isEqualTo(1);
        assertThat(soldQuantityOf(product)).isEqualTo(9);
        assertThat(inventoryChangeOf(product)).isEqualTo(-9);
    }

    @Test
    void concurrentCheckout_multipleProducts_rollsBackAndAvoidsDeadlock() throws Exception {
        // given: A 庫存 10, B 庫存 5, 每人各買 1 件 A 與 B.
        // 一半的人先放 A 再放 B, 另一半相反: 若沒有固定扣庫存順序, 兩筆交易會互等對方的行鎖而死鎖
        Product productA = createProduct("商品 A", 10);
        Product productB = createProduct("商品 B", 5);
        List<User> buyers = createBuyers(THREADS);
        for (int i = 0; i < buyers.size(); i++) {
            User buyer = buyers.get(i);
            if (i % 2 == 0) {
                addToCart(buyer, productA, 1);
                addToCart(buyer, productB, 1);
            } else {
                addToCart(buyer, productB, 1);
                addToCart(buyer, productA, 1);
            }
        }

        CheckoutResults results = checkoutConcurrently(buyers);

        // then: 沒有死鎖或其他非預期例外, 成交數受 B 限制
        assertThat(results.unexpected()).isEmpty();
        assertThat(results.succeeded()).isEqualTo(5);
        assertThat(stockOf(productB)).isZero();
        // B 不足而失敗的交易, 已扣掉的 A 也要一起回滾
        assertThat(stockOf(productA)).isEqualTo(5);
        assertThat(soldQuantityOf(productA)).isEqualTo(5);
        assertThat(soldQuantityOf(productB)).isEqualTo(5);
        assertThat(inventoryChangeOf(productA)).isEqualTo(-5);
        assertThat(inventoryChangeOf(productB)).isEqualTo(-5);
    }

    // 所有執行緒都準備好之後才一起放行, 讓結帳盡可能同時進到 DB
    private CheckoutResults checkoutConcurrently(List<User> buyers) throws Exception {
        CountDownLatch ready = new CountDownLatch(buyers.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(buyers.size())) {
            for (User buyer : buyers) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    orderService.createOrder(buyer.getEmail(), orderRequest());
                    return null;
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int succeeded = 0;
            int outOfStock = 0;
            List<Throwable> unexpected = new ArrayList<>();
            for (Future<?> future : futures) {
                try {
                    future.get(60, TimeUnit.SECONDS);
                    succeeded++;
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof BusinessException be && be.getMessage().contains("庫存不足")) {
                        outOfStock++;
                    } else {
                        unexpected.add(e.getCause());
                    }
                }
            }
            return new CheckoutResults(succeeded, outOfStock, unexpected);
        }
    }

    private record CheckoutResults(int succeeded, int outOfStock, List<Throwable> unexpected) {
    }

    private Product createProduct(String name, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(BigDecimal.valueOf(100));
        product.setStockQuantity(stock);
        product.setStatus(ProductStatus.ON_SHELF);
        return productRepository.save(product);
    }

    private List<User> createBuyers(int count) {
        String batch = UUID.randomUUID().toString().substring(0, 8);
        List<User> buyers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setEmail("buyer-" + batch + "-" + i + "@example.com");
            user.setPasswordHash("not-used");
            user.setName("Buyer " + i);
            buyers.add(user);
        }
        return userRepository.saveAll(buyers);
    }

    private void addToCart(User user, Product product, int quantity) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setProduct(product);
        cart.setQuantity(quantity);
        cartRepository.save(cart);
    }

    private OrderCreateRequest orderRequest() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setRecipientName("王小明");
        request.setRecipientPhone("0912345678");
        request.setRecipientAddress("台北市中正區忠孝東路一段 1 號");
        return request;
    }

    // 以下直接查 DB, 不經過 JPA 快取

    private int stockOf(Product product) {
        return jdbcTemplate.queryForObject(
                "SELECT stock_quantity FROM products WHERE id = ?", Integer.class, product.getId());
    }

    private int soldQuantityOf(Product product) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM order_items WHERE product_id = ?", Integer.class, product.getId());
    }

    private int inventoryChangeOf(Product product) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(change_amount), 0) FROM inventory_logs WHERE product_id = ?", Integer.class, product.getId());
    }

    private int remainingCartItems(List<User> users) {
        List<Long> ids = users.stream().map(User::getId).toList();
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM carts WHERE user_id IN (" + placeholders + ")", Integer.class, ids.toArray());
    }
}
