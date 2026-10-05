package com.zzowo.shop_sys.mapper;

import com.zzowo.shop_sys.dto.request.product.ProductRequest;
import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.ProductImage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductMapper {

    // 給列表頁用 (只轉基本資料 + 封面圖,不觸發 getImages)
    public ProductResponse toSummaryResponse(Product product) {
        if (product == null)
            return null;

        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());
        response.setCoverImageUrl(product.getCoverImageUrl());

        if (product.getStatus() != null) {
            response.setStatus(product.getStatus().name());
        }

        response.setDeletedAt(product.getDeletedAt());
        response.setVersion(product.getVersion());

        return response;
    }

    public ProductResponse toDetailResponse(Product product) {
        if (product == null)
            return null;

        ProductResponse response = toSummaryResponse(product);
        response.setDescription(product.getDescription());

        if (product.getImages() != null) {
            List<String> urls = product.getImages().stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList());
            response.setImageUrls(urls);
        }

        return response;
    }

    // version 由 Hibernate 管理, 不從 request 複製 (修改時的比對在 ProductService.updateProduct)
    public void updateEntityFromRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setStatus(request.getStatus());
        product.setCoverImageUrl(request.getCoverImageUrl());

        if (request.getImageUrls() != null) {
            if (product.getImages() != null) {
                product.getImages().clear();
            }
            List<ProductImage> newImages = request.getImageUrls().stream()
                    .map(url -> {
                        ProductImage img = new ProductImage();
                        img.setImageUrl(url);
                        img.setProduct(product);
                        return img;
                    })
                    .collect(Collectors.toList());

            if (product.getImages() == null) {
                product.setImages(newImages);
            } else {
                product.getImages().addAll(newImages);
            }
        }
    }
}