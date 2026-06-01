package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.mapper.InventoryLogMapper;
import com.zzowo.shop_sys.mapper.ProductMapper;
import com.zzowo.shop_sys.repository.InventoryLogRepository;
import com.zzowo.shop_sys.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock ProductRepository productRepository;
    @Mock ProductMapper productMapper;
    @Mock InventoryLogRepository inventoryLogRepository;
    @Mock InventoryLogMapper inventoryLogMapper;
    @InjectMocks ProductService productService;

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

    // ── helpers ──────────────────────────────────────────────────────────────

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
