package com.zzowo.shop_sys.service;

import lombok.RequiredArgsConstructor;
import com.zzowo.shop_sys.dto.request.product.ProductRequest;
import com.zzowo.shop_sys.dto.request.product.ProductUpdateRequest;
import com.zzowo.shop_sys.dto.response.PageResponse;
import com.zzowo.shop_sys.dto.response.product.InventoryLogResponse;
import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.entity.InventoryLog;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.InventoryChangeReason;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.mapper.InventoryLogMapper;
import com.zzowo.shop_sys.mapper.ProductMapper;
import com.zzowo.shop_sys.repository.InventoryLogRepository;
import com.zzowo.shop_sys.repository.ProductRepository;
import com.zzowo.shop_sys.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "price", "createdAt");
    private static final int MAX_PAGE_SIZE = 100;
    // 前台列表可見的商品狀態 (已下架不顯示,缺貨中仍顯示)
    static final List<ProductStatus> STOREFRONT_STATUSES = List.of(ProductStatus.ON_SHELF, ProductStatus.OUT_OF_STOCK);

    private final ProductRepository productRepository;

    private final ProductMapper productMapper;

    private final InventoryLogRepository inventoryLogRepository;

    private final InventoryLogMapper inventoryLogMapper;

    private final UserRepository userRepository;

    @Transactional
    public ProductResponse createProduct(String email, ProductRequest request) {
        Product product = new Product();
        productMapper.updateEntityFromRequest(product, request);

        Product savedProduct = productRepository.save(product);

        // 初始庫存視為一次進貨,讓 inventory_logs 的變動加總等於目前庫存
        Integer initialStock = savedProduct.getStockQuantity();
        if (initialStock != null && initialStock > 0) {
            writeInventoryLog(savedProduct, initialStock, InventoryChangeReason.RESTOCK, operator(email));
        }

        return productMapper.toDetailResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(String email, Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到商品 ID: " + id));

        // 表單會整包覆寫庫存, 所以要確認表單是基於最新資料: 打開表單後若有結帳扣庫存 (deductStock 會 version + 1),
        // 舊表單的庫存值會把扣掉的量加回去. 必須手動比對: 對 managed entity 呼叫 setVersion 不會生效,
        // Hibernate 的 UPDATE ... WHERE version = ? 用的是載入時的值 (那段只防 findById 到 save 之間的競爭)
        if (!request.getVersion().equals(product.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Product.class, id);
        }

        int oldStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();

        productMapper.updateEntityFromRequest(product, request);

        // 立即 flush 讓 version + 1 反映在回應上; 只用 save 的話要等 commit 才遞增, 回應會帶舊 version
        Product savedProduct = productRepository.saveAndFlush(product);

        // 只改名稱/描述時不產生雜訊紀錄
        int delta = savedProduct.getStockQuantity() - oldStock;
        if (delta != 0) {
            writeInventoryLog(savedProduct, delta, InventoryChangeReason.ADJUSTMENT, operator(email));
        }

        return productMapper.toDetailResponse(savedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到商品 ID: " + id));
        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    public PageResponse<ProductResponse> getStorefrontProducts(Pageable pageable, String keyword) {
        pageable = sanitizePageable(pageable);

        Page<Product> productPage;
        if (keyword != null && !keyword.isBlank()) {
            productPage = productRepository.findByStatusInAndNameContainingSoldOutLast(
                    STOREFRONT_STATUSES, keyword.trim(), pageable);
        } else {
            productPage = productRepository.findByStatusInSoldOutLast(STOREFRONT_STATUSES, pageable);
        }

        return new PageResponse<>(productPage.map(productMapper::toSummaryResponse));
    }

    // 前台只能看到上架中/缺貨中的商品, 已下架的回 404 (與不存在無法區分, 避免列舉未發布商品);
    // 後台編輯商品時共用此端點, 由 includeHidden 放行
    public ProductResponse getProductById(Long id, boolean includeHidden) {
        Product product = productRepository.findByIdWithImages(id)
                .filter(p -> includeHidden || STOREFRONT_STATUSES.contains(p.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));

        return productMapper.toDetailResponse(product);
    }

    public List<InventoryLogResponse> getProductInventoryLogs(Long productId) {
        // 商品不存在時回 404, 而不是回傳空的紀錄清單
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("找不到商品 ID: " + productId);
        }

        List<InventoryLog> logs = inventoryLogRepository.findByProductIdOrderByCreatedAtDesc(productId);

        return logs.stream()
                .map(inventoryLogMapper::toResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<ProductResponse> getAdminProducts(Pageable pageable, String keyword, ProductStatus status) {
        pageable = sanitizePageable(pageable);

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

    public List<ProductResponse> getDeletedProducts() {
        return productRepository.findAllDeleted().stream()
                .map(productMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse restoreProduct(Long id) {
        Product product = productRepository.findDeletedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到已刪除的商品 ID: " + id));
        product.setDeletedAt(null);
        return productMapper.toSummaryResponse(productRepository.save(product));
    }

    public List<InventoryLogResponse> getAllInventoryLogs() {
        return inventoryLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(inventoryLogMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 排序欄位只允許白名單, 每頁筆數超過上限時改為上限
    private Pageable sanitizePageable(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BusinessException("不支援的排序欄位: " + order.getProperty() + ",允許欄位: name, price, createdAt");
            }
        });

        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        return pageable;
    }

    // 取得操作者供庫存紀錄快照使用;找不到使用者時回 null (紀錄仍要留,只是沒有操作者)
    private User operator(String email) {
        if (email == null) {
            return null;
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    private void writeInventoryLog(Product product, int changeAmount, InventoryChangeReason reason, User operator) {
        InventoryLog log = new InventoryLog();
        log.setProduct(product);
        log.setChangeAmount(changeAmount);
        log.setReason(reason);
        log.applyOperator(operator);
        inventoryLogRepository.save(log);
    }
}
