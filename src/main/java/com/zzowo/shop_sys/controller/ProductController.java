package com.zzowo.shop_sys.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.zzowo.shop_sys.dto.request.product.ProductRequest;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.PageResponse;
import com.zzowo.shop_sys.dto.response.product.InventoryLogResponse;
import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Product", description = "商品管理 API")
@RestController
@RequestMapping("/v1/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Operation(
        summary = "取得前台商品列表",
        description = "取得所有狀態為 ON_SHELF 或 OUT_OF_STOCK 的商品 (不含 OFF_SHELF),缺貨商品固定排在最後," +
                      "支援分頁,關鍵字搜尋 (商品名稱) 與排序.排序欄位:name,price,createdAt (預設 createdAt,desc) "
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取得成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "商品名稱關鍵字 (選填) ") @RequestParam(required = false) String keyword) {
        PageResponse<ProductResponse> products = productService.getStorefrontProducts(pageable, keyword);
        return ResponseEntity.ok(ApiResponse.success("取得商品列表成功", products));
    }

    @Operation(summary = "取得單一商品詳情", description = "依商品 ID 取得詳細資訊,僅限上架商品")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取得成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "商品不存在")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @Parameter(description = "商品 ID") @PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("取得商品詳情成功", product));
    }

    @Operation(summary = "新增商品", description = "新增商品至系統 (需要 PRODUCT_MANAGER 或 SUPER_ADMIN 角色) ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "商品新增成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數錯誤")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success("商品新增成功", response));
    }

    @Operation(summary = "修改商品", description = "更新指定商品資訊 (需要 PRODUCT_MANAGER 或 SUPER_ADMIN 角色) ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "商品更新成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數錯誤")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "商品不存在")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "商品 ID") @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("商品更新成功", response));
    }

    @Operation(summary = "刪除商品", description = "刪除指定商品 (需要 PRODUCT_MANAGER 或 SUPER_ADMIN 角色) ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "商品刪除成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "商品不存在")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "商品 ID") @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("商品刪除成功"));
    }

    @Operation(
        summary = "管理員取得所有商品列表",
        description = "管理員查詢全部商品 (含下架/缺貨),支援關鍵字搜尋與狀態篩選 (需要 PRODUCT_MANAGER 或 SUPER_ADMIN 角色)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取得成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAdminProducts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "商品名稱關鍵字 (選填)") @RequestParam(required = false) String keyword,
            @Parameter(description = "狀態篩選 (選填): ON_SHELF, OFF_SHELF, OUT_OF_STOCK") @RequestParam(required = false) ProductStatus status) {
        PageResponse<ProductResponse> products = productService.getAdminProducts(pageable, keyword, status);
        return ResponseEntity.ok(ApiResponse.success("取得商品列表成功", products));
    }

    @Operation(summary = "取得已刪除商品清單", description = "回收桶:列出所有軟刪除商品 (需要 PRODUCT_MANAGER 或 SUPER_ADMIN 角色) ")
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getDeletedProducts() {
        List<ProductResponse> deleted = productService.getDeletedProducts();
        return ResponseEntity.ok(ApiResponse.success("取得已刪除商品成功", deleted));
    }

    @Operation(summary = "還原已刪除商品", description = "將軟刪除商品恢復為原狀態 (需要 PRODUCT_MANAGER 或 SUPER_ADMIN 角色) ")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<ProductResponse>> restoreProduct(
            @Parameter(description = "商品 ID") @PathVariable Long id) {
        ProductResponse restored = productService.restoreProduct(id);
        return ResponseEntity.ok(ApiResponse.success("商品已還原", restored));
    }

    @Operation(summary = "取得所有商品庫存變動紀錄", description = "管理員總覽所有商品的庫存異動紀錄 (需要 PRODUCT_MANAGER 或 SUPER_ADMIN 角色) ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取得成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    @GetMapping("/inventory-logs")
    public ResponseEntity<ApiResponse<List<InventoryLogResponse>>> getAllInventoryLogs() {
        List<InventoryLogResponse> logs = productService.getAllInventoryLogs();
        return ResponseEntity.ok(ApiResponse.success("取得所有庫存紀錄成功", logs));
    }

    @Operation(summary = "取得指定商品庫存變動紀錄", description = "取得特定商品的所有庫存異動紀錄 (需要 PRODUCT_MANAGER 或 SUPER_ADMIN 角色) ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取得成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "商品不存在")
    @GetMapping("/{id}/inventory-logs")
    public ResponseEntity<ApiResponse<List<InventoryLogResponse>>> getInventoryLogs(
            @Parameter(description = "商品 ID") @PathVariable Long id) {
        List<InventoryLogResponse> logs = productService.getProductInventoryLogs(id);
        return ResponseEntity.ok(ApiResponse.success("取得庫存紀錄成功", logs));
    }
}
