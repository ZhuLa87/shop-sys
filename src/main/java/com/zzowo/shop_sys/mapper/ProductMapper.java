package com.zzowo.shop_sys.mapper;

import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.ProductImage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component // 加上這個註解，讓 Spring 管理它，可以在 Service 裡 @Autowired
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());
        // 轉換枚舉為字串
        if (product.getStatus() != null) {
            response.setStatus(product.getStatus().name());
        }
        response.setCoverImageUrl(product.getCoverImageUrl());

        // 手動處理比較複雜的集合轉換邏輯
        if (product.getImages() != null) {
            List<String> urls = product.getImages().stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList());
            response.setImageUrls(urls);
        }

        return response;
    }
}