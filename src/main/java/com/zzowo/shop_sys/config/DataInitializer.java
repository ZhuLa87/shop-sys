package com.zzowo.shop_sys.config;

import com.zzowo.shop_sys.entity.Cart;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.repository.CartRepository;
import com.zzowo.shop_sys.repository.ProductRepository;
import com.zzowo.shop_sys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("[DataInitializer] 資料已存在,跳過初始化");
            return;
        }

        log.info("[DataInitializer] 開始建立測試資料...");

        createUser("admin@test.com",    "admin123",      "測試管理員", "0900000001", Role.SUPER_ADMIN);
        createUser("manager@test.com",  "manager123",    "商品管理員", "0900000002", Role.PRODUCT_MANAGER);
        User customer = createUser("test01@example.com", "mypassword123", "測試員小明", "0912345678", Role.CUSTOMER);
        createUser("test02@example.com", "mypassword123", "測試員小王", "0922345678", Role.CUSTOMER);

        Product earphone = createProduct(
                "藍芽耳機 Pro",
                "高音質主動降噪無線耳機,支援 ANC 技術,連續播放 30 小時",
                new BigDecimal("1990.00"), 50, ProductStatus.ON_SHELF,
                "https://picsum.photos/seed/earphone/400/400");

        createProduct(
                "無線靜音滑鼠",
                "三段 DPI 切換,側鍵設計,辦公靜音首選",
                new BigDecimal("890.00"), 120, ProductStatus.ON_SHELF,
                "https://picsum.photos/seed/mouse/400/400");

        createProduct(
                "機械鍵盤 RGB",
                "青軸手感,RGB 背光,鋁合金外殼,87 鍵緊湊佈局",
                new BigDecimal("2490.00"), 30, ProductStatus.ON_SHELF,
                "https://picsum.photos/seed/keyboard/400/400");

        createProduct(
                "智慧手錶 S3",
                "血氧偵測,心率監測,GPS 定位,防水 50 米",
                new BigDecimal("5990.00"), 15, ProductStatus.ON_SHELF,
                "https://picsum.photos/seed/watch/400/400");

        Product hub = createProduct(
                "USB-C 七合一 Hub",
                "支援 4K HDMI,100W PD 充電,USB 3.0 × 3,SD/TF 讀卡",
                new BigDecimal("1290.00"), 200, ProductStatus.ON_SHELF,
                "https://picsum.photos/seed/hub/400/400");

        createProduct(
                "可攜式藍芽音響",
                "IPX7 防水,360° 環繞音效,內建 5000mAh 電池",
                new BigDecimal("1590.00"), 45, ProductStatus.ON_SHELF,
                "https://picsum.photos/seed/speaker/400/400");

        createProduct(
                "舊款有線耳機",
                "已停產,庫存清倉",
                new BigDecimal("490.00"), 0, ProductStatus.OFF_SHELF, null);

        createProduct(
                "限量版電競滑鼠",
                "限量聯名款,預計下季補貨",
                new BigDecimal("3290.00"), 0, ProductStatus.OUT_OF_STOCK,
                "https://picsum.photos/seed/gaming-mouse/400/400");

        // 測試顧客的購物車
        createCartItem(customer, earphone, 2);
        createCartItem(customer, hub, 1);

        log.info("[DataInitializer] 測試資料建立完成");
        log.info("[DataInitializer] ┌──────────────────────────┬──────────────────┬─────────────┐");
        log.info("[DataInitializer] │ Email                    │ 角色             │ 密碼        │");
        log.info("[DataInitializer] ├──────────────────────────┼──────────────────┼─────────────┤");
        log.info("[DataInitializer] │ admin@test.com           │ SUPER_ADMIN      │ admin123    │");
        log.info("[DataInitializer] │ manager@test.com         │ PRODUCT_MANAGER  │ manager123  │");
        log.info("[DataInitializer] │ test01@example.com       │ CUSTOMER         │ mypassword123│");
        log.info("[DataInitializer] │ test02@example.com       │ CUSTOMER         │ mypassword123│");
        log.info("[DataInitializer] └──────────────────────────┴──────────────────┴─────────────┘");
    }

    private User createUser(String email, String password, String name, String phone, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setName(name);
        user.setPhone(phone);
        user.setRole(role);
        return userRepository.save(user);
    }

    private Product createProduct(String name, String description, BigDecimal price,
                                   int stockQuantity, ProductStatus status, String coverImageUrl) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setStatus(status);
        product.setCoverImageUrl(coverImageUrl);
        return productRepository.save(product);
    }

    private void createCartItem(User user, Product product, int quantity) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setProduct(product);
        cart.setQuantity(quantity);
        cartRepository.save(cart);
    }
}
