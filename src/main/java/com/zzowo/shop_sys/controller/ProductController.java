package com.zzowo.shop_sys.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.service.ProductService;

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
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("取得商品詳情成功", product));
    }
}