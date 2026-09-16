package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.dto.request.product.ProductRequest;
import com.zzowo.shop_sys.dto.response.PageResponse;
import com.zzowo.shop_sys.dto.response.product.InventoryLogResponse;
import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.entity.InventoryLog;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.mapper.InventoryLogMapper;
import com.zzowo.shop_sys.mapper.ProductMapper;
import com.zzowo.shop_sys.repository.InventoryLogRepository;
import com.zzowo.shop_sys.repository.ProductRepository;
import com.zzowo.shop_sys.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "price", "createdAt");
    private static final int MAX_PAGE_SIZE = 100;
    // 前台列表可見的商品狀態 (已下架不顯示,缺貨中仍顯示)
    static final List<ProductStatus> STOREFRONT_STATUSES = List.of(ProductStatus.ON_SHELF, ProductStatus.OUT_OF_STOCK);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private InventoryLogRepository inventoryLogRepository;

    @Autowired
    private InventoryLogMapper inventoryLogMapper;

    @Autowired
    private UserRepository userRepository;

    // 新增商品
    @Transactional
    public ProductResponse createProduct(String email, ProductRequest request) {
        Product product = new Product();
        productMapper.updateEntityFromRequest(product, request);

        // 儲存
        Product savedProduct = productRepository.save(product);

        // 初始庫存視為一次進貨,讓 inventory_logs 的變動加總等於目前庫存
        Integer initialStock = savedProduct.getStockQuantity();
        if (initialStock != null && initialStock > 0) {
            writeInventoryLog(savedProduct, initialStock, "RESTOCK", operator(email));
        }

        return productMapper.toDetailResponse(savedProduct);
    }

    // 修改商品
    @Transactional
    public ProductResponse updateProduct(String email, Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到商品 ID: " + id));

        // 表單會整包覆寫庫存,先記下舊值才算得出差額
        int oldStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();

        productMapper.updateEntityFromRequest(product, request);

        Product savedProduct = productRepository.save(product);

        // 只改名稱/描述時不產生雜訊紀錄
        int delta = savedProduct.getStockQuantity() - oldStock;
        if (delta != 0) {
            writeInventoryLog(savedProduct, delta, "ADJUSTMENT", operator(email));
        }

        return productMapper.toDetailResponse(savedProduct);
    }

    // 軟刪除商品 (設定 deletedAt,資料保留於 DB)
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到商品 ID: " + id));
        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    // 取得前台商品列表 (上架中 + 缺貨中,分頁 + 關鍵字搜尋)
    public PageResponse<ProductResponse> getStorefrontProducts(Pageable pageable, String keyword) {
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BusinessException("不支援的排序欄位: " + order.getProperty() + ",允許欄位: name, price, createdAt");
            }
        });

        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            pageable = PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }

        Page<Product> productPage;
        if (keyword != null && !keyword.isBlank()) {
            productPage = productRepository.findByStatusInAndNameContainingSoldOutLast(
                    STOREFRONT_STATUSES, keyword.trim(), pageable);
        } else {
            productPage = productRepository.findByStatusInSoldOutLast(STOREFRONT_STATUSES, pageable);
        }

        return new PageResponse<>(productPage.map(productMapper::toSummaryResponse));
    }

    // 取得單一商品詳情
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdWithImages(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));

        return productMapper.toDetailResponse(product);
    }

    // 查詢特定商品的庫存紀錄
    public List<InventoryLogResponse> getProductInventoryLogs(Long productId) {
        // 確認商品存在 (這行是為了防呆,若商品不存在 repository 通常會回傳空 list 或報錯,視需求而定)
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("找不到商品 ID: " + productId);
        }

        List<InventoryLog> logs = inventoryLogRepository.findByProductIdOrderByCreatedAtDesc(productId);

        // 轉換成 DTO
        return logs.stream()
                .map(inventoryLogMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 管理員查詢所有商品 (分頁 + 關鍵字 + 狀態篩選)
    public PageResponse<ProductResponse> getAdminProducts(Pageable pageable, String keyword, ProductStatus status) {
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BusinessException("不支援的排序欄位: " + order.getProperty() + ",允許欄位: name, price, createdAt");
            }
        });

        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            pageable = PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        Page<Product> productPage;

        if (status != null && hasKeyword) {
            productPage = productRepository.findByStatusAndNameContaining(status, keyword.trim(), pageable);
        } else if (status != null) {
            productPage = productRepository.findByStatus(status, pageable);
        } else if (hasKeyword) {
            productPage = productRepository.findByNameContaining(keyword.trim(), pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        return new PageResponse<>(productPage.map(productMapper::toSummaryResponse));
    }

    // 取得已軟刪除的商品清單 (管理員回收桶用)
    public List<ProductResponse> getDeletedProducts() {
        return productRepository.findAllDeleted().stream()
                .map(productMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    // 還原已軟刪除的商品
    @Transactional
    public ProductResponse restoreProduct(Long id) {
        Product product = productRepository.findDeletedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到已刪除的商品 ID: " + id));
        product.setDeletedAt(null);
        return productMapper.toSummaryResponse(productRepository.save(product));
    }

    // 取得所有商品的庫存紀錄 (管理員總覽用)
    public List<InventoryLogResponse> getAllInventoryLogs() {
        return inventoryLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(inventoryLogMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 取得操作者供庫存紀錄快照使用;找不到使用者時回 null (紀錄仍要留,只是沒有操作者)
    private User operator(String email) {
        if (email == null) {
            return null;
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    // 寫入庫存異動紀錄 (欄位與 OrderService 結帳扣庫存時一致)
    private void writeInventoryLog(Product product, int changeAmount, String reason, User operator) {
        InventoryLog log = new InventoryLog();
        log.setProduct(product);
        log.setChangeAmount(changeAmount);
        log.setReason(reason);
        log.applyOperator(operator);
        inventoryLogRepository.save(log);
    }
}
