package com.zzowo.shop_sys.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.zzowo.shop_sys.dto.request.product.ProductRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.PageResponse;
import com.zzowo.shop_sys.dto.response.product.InventoryLogResponse;
import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 取得上架商品列表（分頁 + 搜尋 + 排序）
     * URL: GET /v1/products
     * Query params:
     *   page     分頁頁碼，從 0 開始（預設 0）
     *   size     每頁筆數（預設 20，上限 100）
     *   sort     排序欄位與方向，格式 field,asc|desc（預設 createdAt,desc）
     *            可用欄位：name, price, createdAt
     *   keyword  商品名稱關鍵字（選填）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword) {
        PageResponse<ProductResponse> products = productService.getOnShelfProducts(pageable, keyword);
        return ResponseEntity.ok(ApiResponse.success("取得商品列表成功", products));
    }

    /**
     * 取得單一商品詳細資訊
     * URL: GET /v1/products/{id}
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("取得商品詳情成功", product));
    }

    /**
     * 新增商品
     * URL: POST /v1/products
     * @param request
     * @return
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success("商品新增成功", response));
    }

    /**
     * 修改商品
     * URL: PUT /v1/products/{id}
     * @param id
     * @param request
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("商品更新成功", response));
    }

    /**
     * 刪除商品
     * URL: DELETE /v1/products/{id}
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("商品刪除成功"));
    }

    /**
     * 取得所有商品庫存變動紀錄 (管理員總覽用)
     * URL: GET /v1/products/inventory-logs
     * Permission: PRODUCT_MANAGER, SUPER_ADMIN
     * @return
     */
    @GetMapping("/inventory-logs")
    public ResponseEntity<ApiResponse<List<InventoryLogResponse>>> getAllInventoryLogs() {
        List<InventoryLogResponse> logs = productService.getAllInventoryLogs();
        return ResponseEntity.ok(ApiResponse.success("取得所有庫存紀錄成功", logs));
    }

    /**
     * 取得商品庫存變動紀錄
     * URL: GET /v1/products/{id}/inventory-logs
     * @param id
     * @return
     */
    @GetMapping("/{id}/inventory-logs")
    public ResponseEntity<ApiResponse<List<InventoryLogResponse>>> getInventoryLogs(@PathVariable Long id) {
        List<InventoryLogResponse> logs = productService.getProductInventoryLogs(id);
        return ResponseEntity.ok(ApiResponse.success("取得庫存紀錄成功", logs));
    }
}