package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.mapper.ProductMapper;
import com.zzowo.shop_sys.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;

    // 取得所有上架商品
    public List<ProductResponse> getOnShelfProducts() {
        return productRepository.findByStatus("ON_SHELF").stream()
                .map(productMapper::toResponse) // 交給 Mapper 處理
                .collect(Collectors.toList());
    }

    // 取得單一商品詳情
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        return productMapper.toResponse(product);
    }
}