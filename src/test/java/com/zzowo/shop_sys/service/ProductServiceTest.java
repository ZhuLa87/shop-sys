package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.dto.request.product.ProductRequest;
import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.entity.InventoryLog;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.InventoryChangeReason;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.mapper.InventoryLogMapper;
import com.zzowo.shop_sys.mapper.ProductMapper;
import com.zzowo.shop_sys.repository.InventoryLogRepository;
import com.zzowo.shop_sys.repository.ProductRepository;
import com.zzowo.shop_sys.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String EMAIL = "admin@test.com";
    private static final String OPERATOR_NAME = "王小明";

    @Mock ProductRepository productRepository;
    @Mock ProductMapper productMapper;
    @Mock InventoryLogRepository inventoryLogRepository;
    @Mock InventoryLogMapper inventoryLogMapper;
    @Mock UserRepository userRepository;
    @InjectMocks ProductService productService;

    // ── createProduct (庫存紀錄) ──────────────────────────────────────────────

    @Test
    void createProduct_withInitialStock_writesRestockLog() {
        stubMapperSetsStock(50);
        stubSaveReturnsArgument();
        stubOperator(3L);

        productService.createProduct(EMAIL, buildRequest(50));

        InventoryLog log = captureSavedLog();
        assertThat(log.getChangeAmount()).isEqualTo(50);
        assertThat(log.getReason()).isEqualTo(InventoryChangeReason.RESTOCK);
        assertThat(log.getOperatorId()).isEqualTo(3L);
        assertThat(log.getOperatorName()).isEqualTo(OPERATOR_NAME);
        assertThat(log.getOperatorRole()).isEqualTo(Role.PRODUCT_MANAGER);
    }

    @Test
    void createProduct_zeroStock_writesNoLog() {
        stubMapperSetsStock(0);
        stubSaveReturnsArgument();

        productService.createProduct(EMAIL, buildRequest(0));

        verify(inventoryLogRepository, never()).save(any());
    }

    // ── updateProduct (庫存紀錄) ──────────────────────────────────────────────

    @Test
    void updateProduct_stockIncreased_writesPositiveAdjustmentLog() {
        stubExistingProduct(10);
        stubMapperSetsStock(25);
        stubSaveReturnsArgument();
        stubOperator(3L);

        productService.updateProduct(EMAIL, 1L, buildRequest(25));

        InventoryLog log = captureSavedLog();
        assertThat(log.getChangeAmount()).isEqualTo(15);
        assertThat(log.getReason()).isEqualTo(InventoryChangeReason.ADJUSTMENT);
        assertThat(log.getOperatorId()).isEqualTo(3L);
        assertThat(log.getOperatorName()).isEqualTo(OPERATOR_NAME);
        assertThat(log.getOperatorRole()).isEqualTo(Role.PRODUCT_MANAGER);
    }

    @Test
    void updateProduct_stockDecreased_writesNegativeAdjustmentLog() {
        stubExistingProduct(10);
        stubMapperSetsStock(4);
        stubSaveReturnsArgument();
        stubOperator(3L);

        productService.updateProduct(EMAIL, 1L, buildRequest(4));

        assertThat(captureSavedLog().getChangeAmount()).isEqualTo(-6);
    }

    @Test
    void updateProduct_stockUnchanged_writesNoLog() {
        // 只改名稱/描述時不該產生雜訊紀錄
        stubExistingProduct(10);
        stubMapperSetsStock(10);
        stubSaveReturnsArgument();

        productService.updateProduct(EMAIL, 1L, buildRequest(10));

        verify(inventoryLogRepository, never()).save(any());
    }

    @Test
    void updateProduct_unknownOperator_stillWritesLogWithNullOperator() {
        stubExistingProduct(10);
        stubMapperSetsStock(12);
        stubSaveReturnsArgument();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        productService.updateProduct(EMAIL, 1L, buildRequest(12));

        InventoryLog log = captureSavedLog();
        assertThat(log.getChangeAmount()).isEqualTo(2);
        assertThat(log.getOperatorId()).isNull();
        assertThat(log.getOperatorName()).isNull();
        assertThat(log.getOperatorRole()).isNull();
    }

    @Test
    void updateProduct_operatorWithoutName_snapshotsEmailInstead() {
        // 使用者沒填姓名時退而記 email,稽核日誌至少認得出是誰
        stubExistingProduct(10);
        stubMapperSetsStock(12);
        stubSaveReturnsArgument();
        User user = buildOperator(3L);
        user.setName("  ");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        productService.updateProduct(EMAIL, 1L, buildRequest(12));

        assertThat(captureSavedLog().getOperatorName()).isEqualTo(EMAIL);
    }

    // ── deleteProduct ────────────────────────────────────────────────────────

    @Test
    void deleteProduct_setsDeletedAt_andNeverCallsDeleteById() {
        Product product = buildProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        productService.deleteProduct(1L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void deleteProduct_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(productRepository, never()).save(any());
    }

    // ── getDeletedProducts ───────────────────────────────────────────────────

    @Test
    void getDeletedProducts_returnsAllDeletedMapped() {
        Product p1 = buildProduct();
        Product p2 = buildProduct();
        when(productRepository.findAllDeleted()).thenReturn(List.of(p1, p2));
        when(productMapper.toSummaryResponse(any())).thenReturn(new ProductResponse());

        List<ProductResponse> result = productService.getDeletedProducts();

        assertThat(result).hasSize(2);
        verify(productMapper, times(2)).toSummaryResponse(any());
    }

    @Test
    void getDeletedProducts_noneDeleted_returnsEmptyList() {
        when(productRepository.findAllDeleted()).thenReturn(List.of());

        List<ProductResponse> result = productService.getDeletedProducts();

        assertThat(result).isEmpty();
        verify(productMapper, never()).toSummaryResponse(any());
    }

    // ── restoreProduct ───────────────────────────────────────────────────────

    @Test
    void restoreProduct_clearsDeletedAt_andReturnsResponse() {
        Product product = buildProduct();
        product.setDeletedAt(LocalDateTime.now());
        when(productRepository.findDeletedById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        ProductResponse response = new ProductResponse();
        response.setId(1L);
        when(productMapper.toSummaryResponse(any())).thenReturn(response);

        ProductResponse result = productService.restoreProduct(1L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void restoreProduct_notFound_throwsResourceNotFoundException() {
        when(productRepository.findDeletedById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.restoreProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(productRepository, never()).save(any());
    }

    // ── getProductById ───────────────────────────────────────────────────────

    @Test
    void getProductById_offShelf_storefront_throwsNotFound() {
        Product product = buildProduct();
        product.setStatus(ProductStatus.OFF_SHELF);
        when(productRepository.findByIdWithImages(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.getProductById(1L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProductById_offShelf_includeHidden_returnsProduct() {
        Product product = buildProduct();
        product.setStatus(ProductStatus.OFF_SHELF);
        when(productRepository.findByIdWithImages(1L)).thenReturn(Optional.of(product));
        when(productMapper.toDetailResponse(product)).thenReturn(new ProductResponse());

        assertThat(productService.getProductById(1L, true)).isNotNull();
    }

    @Test
    void getProductById_outOfStock_storefront_returnsProduct() {
        Product product = buildProduct();
        product.setStatus(ProductStatus.OUT_OF_STOCK);
        when(productRepository.findByIdWithImages(1L)).thenReturn(Optional.of(product));
        when(productMapper.toDetailResponse(product)).thenReturn(new ProductResponse());

        assertThat(productService.getProductById(1L, false)).isNotNull();
    }

    // ── getStorefrontProducts ────────────────────────────────────────────────

    @Test
    void getStorefrontProducts_noKeyword_queriesOnShelfAndOutOfStock() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(productRepository.findByStatusInSoldOutLast(any(), any()))
                .thenReturn(new PageImpl<>(List.of(buildProduct()), pageable, 1));
        when(productMapper.toSummaryResponse(any())).thenReturn(new ProductResponse());

        productService.getStorefrontProducts(pageable, null);

        verify(productRepository).findByStatusInSoldOutLast(
                List.of(ProductStatus.ON_SHELF, ProductStatus.OUT_OF_STOCK), pageable);
        verify(productRepository, never()).findByStatusInAndNameContainingSoldOutLast(any(), any(), any());
    }

    @Test
    void getStorefrontProducts_withKeyword_trimsKeyword_andQueriesOnShelfAndOutOfStock() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findByStatusInAndNameContainingSoldOutLast(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        productService.getStorefrontProducts(pageable, "  滑鼠  ");

        verify(productRepository).findByStatusInAndNameContainingSoldOutLast(
                List.of(ProductStatus.ON_SHELF, ProductStatus.OUT_OF_STOCK), "滑鼠", pageable);
        verify(productRepository, never()).findByStatusInSoldOutLast(any(), any());
    }

    @Test
    void getStorefrontProducts_unsupportedSortField_throwsBusinessException() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("stockQuantity"));

        assertThatThrownBy(() -> productService.getStorefrontProducts(pageable, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("stockQuantity");
        verify(productRepository, never()).findByStatusInSoldOutLast(any(), any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    // mapper 是 mock,自行模擬它把請求裡的庫存寫進 entity
    private void stubMapperSetsStock(int newStock) {
        doAnswer(inv -> {
            Product target = inv.getArgument(0);
            target.setStockQuantity(newStock);
            return null;
        }).when(productMapper).updateEntityFromRequest(any(), any());
    }

    private void stubSaveReturnsArgument() {
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubExistingProduct(int currentStock) {
        Product existing = buildProduct();
        existing.setStockQuantity(currentStock);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
    }

    private void stubOperator(Long userId) {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buildOperator(userId)));
    }

    private User buildOperator(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail(EMAIL);
        user.setName(OPERATOR_NAME);
        user.setRole(Role.PRODUCT_MANAGER);
        return user;
    }

    private InventoryLog captureSavedLog() {
        ArgumentCaptor<InventoryLog> captor = ArgumentCaptor.forClass(InventoryLog.class);
        verify(inventoryLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private ProductRequest buildRequest(int stockQuantity) {
        ProductRequest request = new ProductRequest();
        request.setName("測試商品");
        request.setPrice(BigDecimal.valueOf(100));
        request.setStockQuantity(stockQuantity);
        request.setStatus(ProductStatus.ON_SHELF);
        return request;
    }

    private Product buildProduct() {
        Product p = new Product();
        p.setId(1L);
        p.setName("測試商品");
        p.setPrice(BigDecimal.valueOf(100));
        p.setStockQuantity(10);
        p.setStatus(ProductStatus.ON_SHELF);
        return p;
    }
}
