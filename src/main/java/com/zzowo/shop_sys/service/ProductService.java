package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.dto.request.product.ProductRequest;
import com.zzowo.shop_sys.dto.response.product.InventoryLogResponse;
import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.entity.InventoryLog;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.ProductImage;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.mapper.InventoryLogMapper;
import com.zzowo.shop_sys.mapper.ProductMapper;
import com.zzowo.shop_sys.repository.InventoryLogRepository;
import com.zzowo.shop_sys.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private InventoryLogRepository inventoryLogRepository;

    @Autowired
    private InventoryLogMapper inventoryLogMapper;

    // 新增商品
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        productMapper.updateEntityFromRequest(product, request);

        // 儲存
        Product savedProduct = productRepository.save(product);
        return productMapper.toDetailResponse(savedProduct);
    }

    // 修改商品
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到商品 ID: " + id));

        productMapper.updateEntityFromRequest(product, request);

        Product savedProduct = productRepository.save(product);
        return productMapper.toDetailResponse(savedProduct);
    }

    // 刪除商品 (這裡直接刪除，實務上通常是改狀態為 OFF_SHELF)
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("找不到商品 ID: " + id);
        }
        productRepository.deleteById(id);
    }

    // 取得所有上架商品
    public List<ProductResponse> getOnShelfProducts() {
        return productRepository.findByStatus(ProductStatus.ON_SHELF).stream()
                .map(productMapper::toSummaryResponse) // 交給 Mapper 處理
                .collect(Collectors.toList());
    }

    // 取得單一商品詳情
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdWithImages(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));

        return productMapper.toDetailResponse(product);
    }

    // 查詢特定商品的庫存紀錄
    public List<InventoryLogResponse> getProductInventoryLogs(Long productId) {
        // 確認商品存在 (這行是為了防呆，若商品不存在 repository 通常會回傳空 list 或報錯，視需求而定)
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("找不到商品 ID: " + productId);
        }

        List<InventoryLog> logs = inventoryLogRepository.findByProductIdOrderByCreatedAtDesc(productId);

        // 轉換成 DTO
        return logs.stream()
                .map(inventoryLogMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 取得所有商品的庫存紀錄 (管理員總覽用)
    public List<InventoryLogResponse> getAllInventoryLogs() {
        return inventoryLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(inventoryLogMapper::toResponse)
                .collect(Collectors.toList());
    }
}