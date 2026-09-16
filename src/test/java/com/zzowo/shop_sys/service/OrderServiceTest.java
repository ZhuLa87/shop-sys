package com.zzowo.shop_sys.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.zzowo.shop_sys.dto.request.order.OrderCreateRequest;
import com.zzowo.shop_sys.dto.response.order.OrderResponse;
import com.zzowo.shop_sys.entity.Cart;
import com.zzowo.shop_sys.entity.InventoryLog;
import com.zzowo.shop_sys.entity.Order;
import com.zzowo.shop_sys.entity.OrderItem;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.OrderStatus;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.mapper.OrderMapper;
import com.zzowo.shop_sys.repository.CartRepository;
import com.zzowo.shop_sys.repository.InventoryLogRepository;
import com.zzowo.shop_sys.repository.OrderRepository;
import com.zzowo.shop_sys.repository.ProductRepository;
import com.zzowo.shop_sys.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String EMAIL = "buyer@test.com";

    @Mock OrderRepository orderRepository;
    @Mock CartRepository cartRepository;
    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;
    @Mock OrderMapper orderMapper;
    @Mock InventoryLogRepository inventoryLogRepository;
    @InjectMocks OrderService orderService;

    // ── createOrder ──────────────────────────────────────────────────────────

    @Test
    void createOrder_success_deductsStock_writesInventoryLog_andClearsCart() {
        User user = buildUser(1L, Role.CUSTOMER);
        Product product = buildProduct(10L, "手機", BigDecimal.valueOf(100), 10);
        stubUser(user);
        when(cartRepository.findByUserId(1L)).thenReturn(List.of(buildCart(1L, user, product, 3)));
        stubDeductStock(10L, 3);
        stubOrderSave();

        orderService.createOrder(EMAIL, createRequest());

        // 庫存以單句條件更新扣減 (mock 下記憶體中的 stockQuantity 不會變動)
        verify(productRepository).deductStock(10L, 3);

        // 庫存異動紀錄
        ArgumentCaptor<InventoryLog> logCaptor = ArgumentCaptor.forClass(InventoryLog.class);
        verify(inventoryLogRepository).save(logCaptor.capture());
        InventoryLog log = logCaptor.getValue();
        assertThat(log.getChangeAmount()).isEqualTo(-3);
        assertThat(log.getReason()).isEqualTo("ORDER");
        assertThat(log.getOperatorId()).isEqualTo(1L);
        assertThat(log.getProduct()).isSameAs(product);

        // 結帳後清空購物車
        verify(cartRepository).deleteByUserId(1L);
    }

    @Test
    void createOrder_calculatesTotalAmountAcrossAllItems() {
        User user = buildUser(1L, Role.CUSTOMER);
        Product cheap = buildProduct(10L, "滑鼠", BigDecimal.valueOf(250), 10);
        Product pricey = buildProduct(11L, "螢幕", BigDecimal.valueOf(4500), 5);
        stubUser(user);
        when(cartRepository.findByUserId(1L)).thenReturn(List.of(
                buildCart(1L, user, cheap, 2),
                buildCart(2L, user, pricey, 3)));
        stubDeductStock(10L, 2);
        stubDeductStock(11L, 3);
        stubOrderSave();

        orderService.createOrder(EMAIL, createRequest());

        // 250 * 2 + 4500 * 3 = 14000
        Order saved = captureSavedOrder();
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(14000));
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getItems()).hasSize(2);
    }

    @Test
    void createOrder_snapshotsProductNameCoverImageAndPrice() {
        User user = buildUser(1L, Role.CUSTOMER);
        Product product = buildProduct(10L, "限量商品", BigDecimal.valueOf(999), 5);
        product.setCoverImageUrl("https://minio/cover.jpg");
        stubUser(user);
        when(cartRepository.findByUserId(1L)).thenReturn(List.of(buildCart(1L, user, product, 2)));
        stubDeductStock(10L, 2);
        stubOrderSave();

        orderService.createOrder(EMAIL, createRequest());

        OrderItem item = captureSavedOrder().getItems().get(0);
        assertThat(item.getProductName()).isEqualTo("限量商品");
        assertThat(item.getCoverImageUrl()).isEqualTo("https://minio/cover.jpg");
        assertThat(item.getPriceAtPurchase()).isEqualByComparingTo(BigDecimal.valueOf(999));
        assertThat(item.getQuantity()).isEqualTo(2);
    }

    @Test
    void createOrder_stockExactlyEqualsQuantity_succeedsAndLeavesZeroStock() {
        User user = buildUser(1L, Role.CUSTOMER);
        Product product = buildProduct(10L, "最後一批", BigDecimal.valueOf(100), 4);
        stubUser(user);
        when(cartRepository.findByUserId(1L)).thenReturn(List.of(buildCart(1L, user, product, 4)));
        stubDeductStock(10L, 4);
        stubOrderSave();

        assertThatNoException().isThrownBy(() -> orderService.createOrder(EMAIL, createRequest()));

        // 庫存剛好等於訂購量時邊界條件成立,仍會扣減
        verify(productRepository).deductStock(10L, 4);
    }

    @Test
    void createOrder_emptyCart_throwsBusinessException_andSavesNothing() {
        stubUser(buildUser(1L, Role.CUSTOMER));
        when(cartRepository.findByUserId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.createOrder(EMAIL, createRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("購物車為空");

        verify(orderRepository, never()).save(any());
        verify(cartRepository, never()).deleteByUserId(any());
    }

    @Test
    void createOrder_softDeletedProduct_throwsBusinessException_andDoesNotTouchStock() {
        User user = buildUser(1L, Role.CUSTOMER);
        Cart cartWithDeletedProduct = buildCart(1L, user, null, 1); // @NotFound(IGNORE) 使 product 為 null
        stubUser(user);
        when(cartRepository.findByUserId(1L)).thenReturn(List.of(cartWithDeletedProduct));

        assertThatThrownBy(() -> orderService.createOrder(EMAIL, createRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已下架或刪除");

        verify(productRepository, never()).deductStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
        verify(cartRepository, never()).deleteByUserId(any());
    }

    @Test
    void createOrder_insufficientStock_throwsWithProductName_andSavesNothing() {
        User user = buildUser(1L, Role.CUSTOMER);
        Product product = buildProduct(10L, "熱門商品", BigDecimal.valueOf(100), 2);
        stubUser(user);
        when(cartRepository.findByUserId(1L)).thenReturn(List.of(buildCart(1L, user, product, 3)));

        assertThatThrownBy(() -> orderService.createOrder(EMAIL, createRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("熱門商品")
                .hasMessageContaining("庫存不足");

        verify(productRepository, never()).deductStock(anyLong(), anyInt()); // 預檢查就擋下,不必打 DB
        verify(orderRepository, never()).save(any());
        verify(inventoryLogRepository, never()).save(any());
    }

    @Test
    void createOrder_lostRaceAfterPrecheck_throwsInsufficientStock() {
        // 預檢查時庫存還夠,但扣庫存的瞬間被其他交易買走 (deductStock 回 0 筆)
        User user = buildUser(1L, Role.CUSTOMER);
        Product product = buildProduct(10L, "秒殺商品", BigDecimal.valueOf(100), 1);
        stubUser(user);
        when(cartRepository.findByUserId(1L)).thenReturn(List.of(buildCart(1L, user, product, 1)));
        when(productRepository.deductStock(10L, 1)).thenReturn(0);

        // 應得到明確的「庫存不足」,而非樂觀鎖衝突的 409
        assertThatThrownBy(() -> orderService.createOrder(EMAIL, createRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("秒殺商品")
                .hasMessageContaining("庫存不足");

        verify(orderRepository, never()).save(any());
        verify(cartRepository, never()).deleteByUserId(any());
    }

    @ParameterizedTest
    @EnumSource(value = ProductStatus.class, names = {"OUT_OF_STOCK", "OFF_SHELF"})
    void createOrder_productNotOnShelf_throwsWithProductName_andSavesNothing(ProductStatus status) {
        User user = buildUser(1L, Role.CUSTOMER);
        Product product = buildProduct(10L, "限量滑鼠", BigDecimal.valueOf(100), 10); // 仍有庫存,但狀態不可購買
        product.setStatus(status);
        stubUser(user);
        when(cartRepository.findByUserId(1L)).thenReturn(List.of(buildCart(1L, user, product, 1)));

        assertThatThrownBy(() -> orderService.createOrder(EMAIL, createRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("限量滑鼠")
                .hasMessageContaining(status.getDescription());

        assertThat(product.getStockQuantity()).isEqualTo(10); // 庫存未被扣減
        verify(productRepository, never()).deductStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
        verify(cartRepository, never()).deleteByUserId(any());
    }

    @Test
    void createOrder_secondItemOutOfStock_doesNotPersistOrder() {
        User user = buildUser(1L, Role.CUSTOMER);
        Product ok = buildProduct(10L, "有庫存", BigDecimal.valueOf(100), 10);
        Product outOfStock = buildProduct(11L, "缺貨中", BigDecimal.valueOf(100), 1);
        stubUser(user);
        when(cartRepository.findByUserId(1L)).thenReturn(List.of(
                buildCart(1L, user, ok, 1),
                buildCart(2L, user, outOfStock, 5)));
        stubDeductStock(10L, 1); // 第一項扣減成功, 第二項才失敗

        assertThatThrownBy(() -> orderService.createOrder(EMAIL, createRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("缺貨中");

        // 訂單未成立;第一項商品的扣減依賴 @Transactional 回滾
        verify(orderRepository, never()).save(any());
        verify(cartRepository, never()).deleteByUserId(any());
        verify(inventoryLogRepository, times(1)).save(any()); // 只有第一項寫入,隨交易一起回滾
    }

    @Test
    void createOrder_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(EMAIL, createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("使用者不存在");

        verify(cartRepository, never()).findByUserId(any());
    }

    // ── getMyOrders ──────────────────────────────────────────────────────────

    @Test
    void getMyOrders_returnsMappedOrdersForCurrentUser() {
        User user = buildUser(1L, Role.CUSTOMER);
        stubUser(user);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(new Order(), new Order()));
        when(orderMapper.toOrderResponse(any())).thenReturn(new OrderResponse());

        List<OrderResponse> result = orderService.getMyOrders(EMAIL);

        assertThat(result).hasSize(2);
        verify(orderMapper, times(2)).toOrderResponse(any());
    }

    @Test
    void getMyOrders_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getMyOrders(EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getOrderById ─────────────────────────────────────────────────────────

    @Test
    void getOrderById_ownOrder_returnsOrder() {
        User user = buildUser(1L, Role.CUSTOMER);
        stubUser(user);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(buildOrder(50L, user)));
        OrderResponse expected = new OrderResponse();
        expected.setId(50L);
        when(orderMapper.toOrderResponse(any())).thenReturn(expected);

        OrderResponse result = orderService.getOrderById(EMAIL, 50L);

        assertThat(result.getId()).isEqualTo(50L);
    }

    @Test
    void getOrderById_otherUsersOrder_throwsForbidden() {
        User requester = buildUser(1L, Role.CUSTOMER);
        User owner = buildUser(2L, Role.CUSTOMER);
        stubUser(requester);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(buildOrder(50L, owner)));

        assertThatThrownBy(() -> orderService.getOrderById(EMAIL, 50L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無權限")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getOrderById_superAdmin_canViewOtherUsersOrder() {
        User admin = buildUser(1L, Role.SUPER_ADMIN);
        User owner = buildUser(2L, Role.CUSTOMER);
        stubUser(admin);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(buildOrder(50L, owner)));
        when(orderMapper.toOrderResponse(any())).thenReturn(new OrderResponse());

        assertThatNoException().isThrownBy(() -> orderService.getOrderById(EMAIL, 50L));
    }

    // ── cancelExpiredOrder ───────────────────────────────────────────────────

    @Test
    void cancelExpiredOrder_restoresStockAndWritesCancelLog() {
        Product product = buildProduct(10L, "手機", BigDecimal.valueOf(100), 0);
        Order order = buildOrderWithItems(500L, buildOrderItem(product, 3));
        stubCancelIfPending(500L, 1);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(productRepository.restoreStock(10L, 3)).thenReturn(1);

        boolean cancelled = orderService.cancelExpiredOrder(500L);

        assertThat(cancelled).isTrue();
        verify(productRepository).restoreStock(10L, 3);

        ArgumentCaptor<InventoryLog> logCaptor = ArgumentCaptor.forClass(InventoryLog.class);
        verify(inventoryLogRepository).save(logCaptor.capture());
        InventoryLog log = logCaptor.getValue();
        assertThat(log.getChangeAmount()).isEqualTo(3);
        assertThat(log.getReason()).isEqualTo("CANCEL");
        assertThat(log.getOperatorId()).isNull(); // 系統自動作業
        assertThat(log.getProduct()).isSameAs(product);
    }

    @Test
    void cancelExpiredOrder_notPending_returnsFalse_andRestoresNothing() {
        // 訂單已付款或已被其他實例取消,絕不可重複回補庫存
        stubCancelIfPending(500L, 0);

        boolean cancelled = orderService.cancelExpiredOrder(500L);

        assertThat(cancelled).isFalse();
        verify(orderRepository, never()).findById(any());
        verify(productRepository, never()).restoreStock(anyLong(), anyInt());
        verify(inventoryLogRepository, never()).save(any());
    }

    @Test
    void cancelExpiredOrder_softDeletedProduct_skipsThatItem_butStillCancels() {
        Product alive = buildProduct(10L, "還在的商品", BigDecimal.valueOf(100), 0);
        Order order = buildOrderWithItems(500L,
                buildOrderItem(alive, 2),
                buildOrderItem(null, 5)); // @NotFound(IGNORE) 使已刪除商品為 null
        stubCancelIfPending(500L, 1);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(productRepository.restoreStock(10L, 2)).thenReturn(1);

        assertThat(orderService.cancelExpiredOrder(500L)).isTrue();

        verify(productRepository).restoreStock(10L, 2);
        verify(productRepository, never()).restoreStock(anyLong(), eq(5));
        verify(inventoryLogRepository, times(1)).save(any());
    }

    @Test
    void cancelExpiredOrder_restoreStockReturnsZero_writesNoLogForThatItem() {
        Product product = buildProduct(10L, "已刪除商品", BigDecimal.valueOf(100), 0);
        Order order = buildOrderWithItems(500L, buildOrderItem(product, 4));
        stubCancelIfPending(500L, 1);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(productRepository.restoreStock(10L, 4)).thenReturn(0);

        assertThat(orderService.cancelExpiredOrder(500L)).isTrue();

        verify(inventoryLogRepository, never()).save(any());
    }

    @Test
    void cancelExpiredOrder_orderDisappeared_throwsResourceNotFoundException() {
        stubCancelIfPending(500L, 1);
        when(orderRepository.findById(500L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelExpiredOrder(500L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單不存在");
    }

    @Test
    void getOrderById_orderNotFound_throwsResourceNotFoundException() {
        stubUser(buildUser(1L, Role.CUSTOMER));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(EMAIL, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單不存在");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void stubUser(User user) {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private void stubDeductStock(Long productId, int quantity) {
        when(productRepository.deductStock(productId, quantity)).thenReturn(1);
    }

    private void stubOrderSave() {
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Order captureSavedOrder() {
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        return captor.getValue();
    }

    private User buildUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(EMAIL);
        user.setRole(role);
        return user;
    }

    private Product buildProduct(Long id, String name, BigDecimal price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setStatus(ProductStatus.ON_SHELF);
        return product;
    }

    private Cart buildCart(Long id, User user, Product product, int quantity) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setUser(user);
        cart.setProduct(product);
        cart.setQuantity(quantity);
        return cart;
    }

    private void stubCancelIfPending(Long orderId, int affectedRows) {
        when(orderRepository.cancelIfPending(orderId, OrderStatus.PENDING, OrderStatus.CANCELLED))
                .thenReturn(affectedRows);
    }

    private Order buildOrderWithItems(Long id, OrderItem... items) {
        Order order = buildOrder(id, buildUser(1L, Role.CUSTOMER));
        for (OrderItem item : items) {
            item.setOrder(order);
            order.getItems().add(item);
        }
        return order;
    }

    private OrderItem buildOrderItem(Product product, int quantity) {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setProductName(product == null ? "已刪除商品" : product.getName());
        item.setPriceAtPurchase(BigDecimal.valueOf(100));
        item.setQuantity(quantity);
        return item;
    }

    private Order buildOrder(Long id, User owner) {
        Order order = new Order();
        order.setId(id);
        order.setUser(owner);
        order.setStatus(OrderStatus.PENDING);
        order.setItems(new ArrayList<>());
        return order;
    }

    private OrderCreateRequest createRequest() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setRecipientName("王小明");
        request.setRecipientPhone("0912345678");
        request.setRecipientAddress("台北市中正區忠孝東路一段 1 號");
        return request;
    }
}
