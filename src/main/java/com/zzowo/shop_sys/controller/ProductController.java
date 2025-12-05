package com.zzowo.shop_sys.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.zzowo.shop_sys.dto.request.product.ProductRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 取得所有上架商品列表
     * URL: GET /v1/products
     */
    @GetMapping // 繼承上方的路徑
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts() {
        List<ProductResponse> products = productService.getOnShelfProducts();
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
}