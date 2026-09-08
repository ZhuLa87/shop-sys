package com.zzowo.shop_sys.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.zzowo.shop_sys.dto.request.cart.AddToCartRequest;
import com.zzowo.shop_sys.dto.response.cart.CartItemResponse;
import com.zzowo.shop_sys.entity.Cart;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.mapper.CartMapper;
import com.zzowo.shop_sys.repository.CartRepository;
import com.zzowo.shop_sys.repository.ProductRepository;
import com.zzowo.shop_sys.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final String EMAIL = "buyer@test.com";

    @Mock CartRepository cartRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @Mock CartMapper cartMapper;
    @InjectMocks CartService cartService;

    // ── getUserCart ──────────────────────────────────────────────────────────

    @Test
    void getUserCart_removesStaleItems_andReturnsOnlyValidOnes() {
        User user = buildUser(1L);
        Product product = buildProduct(10L, 10);
        Cart valid = buildCart(1L, user, product, 2);
        Cart stale = buildCart(2L, user, null, 1); // 商品已軟刪除,@NotFound(IGNORE) 回傳 null
        stubUser(user);
        when(cartRepository.findByUserId(1L)).thenReturn(List.of(valid, stale));
        when(cartMapper.toCartItemResponse(valid)).thenReturn(new CartItemResponse());

        List<CartItemResponse> result = cartService.getUserCart(EMAIL);

        assertThat(result).hasSize(1);
        verify(cartRepository).deleteAllById(List.of(2L));
        verify(cartMapper, never()).toCartItemResponse(stale);
    }

    @Test
    void getUserCart_allProductsValid_doesNotDeleteAnything() {
        User user = buildUser(1L);
        Cart cart = buildCart(1L, user, buildProduct(10L, 10), 1);
        stubUser(user);
        when(cartRepository.findByUserId(1L)).thenReturn(List.of(cart));
        when(cartMapper.toCartItemResponse(cart)).thenReturn(new CartItemResponse());

        List<CartItemResponse> result = cartService.getUserCart(EMAIL);

        assertThat(result).hasSize(1);
        verify(cartRepository, never()).deleteAllById(any());
    }

    @Test
    void getUserCart_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getUserCart(EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("使用者不存在");
    }

    // ── addToCart ────────────────────────────────────────────────────────────

    @Test
    void addToCart_newItem_savesWithRequestedQuantity() {
        User user = buildUser(1L);
        Product product = buildProduct(10L, 10);
        stubUser(user);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserIdAndProductId(1L, 10L)).thenReturn(Optional.empty());

        cartService.addToCart(EMAIL, addRequest(10L, 3));

        Cart saved = captureSavedCart();
        assertThat(saved.getQuantity()).isEqualTo(3);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getProduct()).isSameAs(product);
    }

    @Test
    void addToCart_existingItem_accumulatesQuantity() {
        User user = buildUser(1L);
        Product product = buildProduct(10L, 10);
        Cart existing = buildCart(5L, user, product, 2);
        stubUser(user);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserIdAndProductId(1L, 10L)).thenReturn(Optional.of(existing));

        cartService.addToCart(EMAIL, addRequest(10L, 3));

        assertThat(captureSavedCart().getQuantity()).isEqualTo(5); // 2 + 3
    }

    @Test
    void addToCart_existingPlusNewExceedsStock_throwsWithCurrentQuantity() {
        User user = buildUser(1L);
        Product product = buildProduct(10L, 10);
        stubUser(user);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserIdAndProductId(1L, 10L))
                .thenReturn(Optional.of(buildCart(5L, user, product, 8)));

        // 已有 8 件 + 再加 3 件 = 11 > 庫存 10
        assertThatThrownBy(() -> cartService.addToCart(EMAIL, addRequest(10L, 3)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("庫存不足")
                .hasMessageContaining("已有 8 件");

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_totalExactlyEqualsStock_succeeds() {
        User user = buildUser(1L);
        Product product = buildProduct(10L, 10);
        stubUser(user);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserIdAndProductId(1L, 10L))
                .thenReturn(Optional.of(buildCart(5L, user, product, 7)));

        // 7 + 3 = 10,剛好等於庫存,應允許
        assertThatNoException().isThrownBy(() -> cartService.addToCart(EMAIL, addRequest(10L, 3)));

        assertThat(captureSavedCart().getQuantity()).isEqualTo(10);
    }

    @Test
    void addToCart_newItemExceedsStock_throws() {
        User user = buildUser(1L);
        Product product = buildProduct(10L, 2);
        stubUser(user);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserIdAndProductId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(EMAIL, addRequest(10L, 5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("庫存不足");

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_productNotFound_throwsResourceNotFoundException() {
        stubUser(buildUser(1L));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(EMAIL, addRequest(99L, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品不存在");

        verify(cartRepository, never()).save(any());
    }

    // ── removeFromCart ───────────────────────────────────────────────────────

    @Test
    void removeFromCart_ownItem_deletesIt() {
        User user = buildUser(1L);
        Cart cart = buildCart(5L, user, buildProduct(10L, 10), 1);
        stubUser(user);
        when(cartRepository.findById(5L)).thenReturn(Optional.of(cart));

        cartService.removeFromCart(EMAIL, 5L);

        verify(cartRepository).delete(cart);
    }

    @Test
    void removeFromCart_otherUsersItem_throwsForbidden_andDeletesNothing() {
        User requester = buildUser(1L);
        Cart othersCart = buildCart(5L, buildUser(2L), buildProduct(10L, 10), 1);
        stubUser(requester);
        when(cartRepository.findById(5L)).thenReturn(Optional.of(othersCart));

        assertThatThrownBy(() -> cartService.removeFromCart(EMAIL, 5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無權限")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(cartRepository, never()).delete(any());
    }

    @Test
    void removeFromCart_itemNotFound_throwsResourceNotFoundException() {
        stubUser(buildUser(1L));
        when(cartRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeFromCart(EMAIL, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("購物車資料不存在");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void stubUser(User user) {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private Cart captureSavedCart() {
        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(captor.capture());
        return captor.getValue();
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail(EMAIL);
        user.setRole(Role.CUSTOMER);
        return user;
    }

    private Product buildProduct(Long id, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName("測試商品");
        product.setPrice(BigDecimal.valueOf(100));
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

    private AddToCartRequest addRequest(Long productId, int quantity) {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        return request;
    }
}
