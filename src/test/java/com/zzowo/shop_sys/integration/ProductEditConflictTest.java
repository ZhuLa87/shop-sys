package com.zzowo.shop_sys.integration;

import com.zzowo.shop_sys.dto.request.order.OrderCreateRequest;
import com.zzowo.shop_sys.dto.request.product.ProductRequest;
import com.zzowo.shop_sys.dto.request.product.ProductUpdateRequest;
import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.entity.Cart;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.repository.CartRepository;
import com.zzowo.shop_sys.repository.ProductRepository;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.service.OrderService;
import com.zzowo.shop_sys.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 後台編輯表單打開後才發生的結帳, 不能被表單上的舊庫存值蓋回去.
// 結帳的 deductStock 是 native UPDATE (version + 1), 要在真的 MariaDB 上走完整流程才證明得了
class ProductEditConflictTest extends AbstractMariaDbIntegrationTest {

    private static final String ADMIN_EMAIL = "admin-not-seeded@example.com";

    @Autowired ProductService productService;
    @Autowired OrderService orderService;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    @Autowired CartRepository cartRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;

    @Test
    void checkoutAfterEditFormOpened_staleSaveIsRejected_andStockIsKept() {
        // given: 庫存 10, 管理員打開編輯表單
        Product product = createProduct(10);
        ProductResponse form = productService.getProductById(product.getId(), true);

        // when: 表單打開期間有人買了 1 件
        User buyer = createBuyer();
        addToCart(buyer, product, 1);
        orderService.createOrder(buyer.getEmail(), orderRequest());
        assertThat(stockOf(product)).isEqualTo(9);

        // then: 用舊表單 (庫存 10, 舊 version) 儲存會被拒絕, 賣掉的 1 件不會被加回去
        ProductUpdateRequest staleSave = requestFrom(form);
        assertThatThrownBy(() -> productService.updateProduct(ADMIN_EMAIL, product.getId(), staleSave))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(stockOf(product)).isEqualTo(9);
        assertThat(inventoryChangeOf(product)).isEqualTo(-1);

        // 重新載入表單後再存就會成功
        ProductResponse reloaded = productService.getProductById(product.getId(), true);
        assertThat(reloaded.getStockQuantity()).isEqualTo(9);
        assertThat(reloaded.getVersion()).isGreaterThan(form.getVersion());

        ProductUpdateRequest freshSave = requestFrom(reloaded);
        freshSave.setStockQuantity(20);
        productService.updateProduct(ADMIN_EMAIL, product.getId(), freshSave);

        assertThat(stockOf(product)).isEqualTo(20);
        // 商品直接用 repository 建立, 沒有進貨紀錄: 結帳 -1, 人工調整 9 -> 20 記 +11
        assertThat(inventoryChangeOf(product)).isEqualTo(10);
    }

    @Test
    void updateWithoutStockChange_returnsVersionMatchingDb() {
        // 只改名稱時不寫庫存紀錄, 回應的 version 仍要是更新後的值, 否則拿回應接著再存會一直 409
        Product product = createProduct(10);
        ProductUpdateRequest rename = requestFrom(productService.getProductById(product.getId(), true));
        rename.setName("改過的名稱");

        ProductResponse saved = productService.updateProduct(ADMIN_EMAIL, product.getId(), rename);

        assertThat(saved.getVersion()).isEqualTo(versionOf(product)).isEqualTo(rename.getVersion() + 1);

        // 拿回應的 version 接著再存也會成功
        ProductUpdateRequest next = requestFrom(saved);
        next.setDescription("第二次修改");
        assertThat(productService.updateProduct(ADMIN_EMAIL, product.getId(), next).getVersion())
                .isEqualTo(versionOf(product));
    }

    @Test
    void createProduct_ignoresVersionInRequestBody() throws Exception {
        // 新增商品的 request 沒有 version 欄位, client 多送了也不能影響樂觀鎖的起點
        ProductRequest request = objectMapper.readValue("""
                {"name":"帶了 version 的新商品","price":100,"stockQuantity":5,"status":"ON_SHELF","version":99}
                """, ProductRequest.class);

        ProductResponse created = productService.createProduct(ADMIN_EMAIL, request);

        assertThat(created.getVersion()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT version FROM products WHERE id = ?", Long.class, created.getId())).isZero();
    }

    private Product createProduct(int stock) {
        Product product = new Product();
        product.setName("編輯衝突商品");
        product.setPrice(BigDecimal.valueOf(100));
        product.setStockQuantity(stock);
        product.setStatus(ProductStatus.ON_SHELF);
        return productRepository.save(product);
    }

    private User createBuyer() {
        User user = new User();
        user.setEmail("buyer-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        user.setPasswordHash("not-used");
        user.setName("Buyer");
        return userRepository.save(user);
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

    // 模擬前端表單: 把取得商品時的資料 (含 version) 原樣送回
    private ProductUpdateRequest requestFrom(ProductResponse response) {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName(response.getName());
        request.setDescription(response.getDescription());
        request.setPrice(response.getPrice());
        request.setStockQuantity(response.getStockQuantity());
        request.setStatus(ProductStatus.valueOf(response.getStatus()));
        request.setCoverImageUrl(response.getCoverImageUrl());
        request.setImageUrls(response.getImageUrls());
        request.setVersion(response.getVersion());
        return request;
    }

    // 以下直接查 DB, 不經過 JPA 快取

    private int stockOf(Product product) {
        return jdbcTemplate.queryForObject(
                "SELECT stock_quantity FROM products WHERE id = ?", Integer.class, product.getId());
    }

    private long versionOf(Product product) {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM products WHERE id = ?", Long.class, product.getId());
    }

    private int inventoryChangeOf(Product product) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(change_amount), 0) FROM inventory_logs WHERE product_id = ?", Integer.class, product.getId());
    }
}
