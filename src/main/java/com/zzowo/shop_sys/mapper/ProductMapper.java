package com.zzowo.shop_sys.mapper;

import com.zzowo.shop_sys.dto.request.product.ProductRequest;
import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.ProductImage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component // 加上這個註解,讓 Spring 管理它,可以在 Service 裡 @Autowired
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

        return response;
    }

    // 給詳情頁用 (完整轉換)
    public ProductResponse toDetailResponse(Product product) {
        if (product == null)
            return null;

        // 先呼叫上面轉好基本資料
        ProductResponse response = toSummaryResponse(product);
        response.setDescription(product.getDescription()); // 列表頁可能也不需要描述,詳情頁才加

        // 詳情頁才處理圖片集
        if (product.getImages() != null) { // 若是用 JOIN FETCH,這裡已經被初始化了,不會報錯
            List<String> urls = product.getImages().stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList());
            response.setImageUrls(urls);
        }

        return response;
    }

    // 將 Request 的資料更新到 Product Entity
    public void updateEntityFromRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setStatus(request.getStatus());
        product.setCoverImageUrl(request.getCoverImageUrl());

        // 處理圖片關聯
        if (request.getImageUrls() != null) {
            if (product.getImages() != null) {
                product.getImages().clear();
            }
            List<ProductImage> newImages = request.getImageUrls().stream()
                    .map(url -> {
                        ProductImage img = new ProductImage();
                        img.setImageUrl(url);
                        img.setProduct(product); // 設定雙向關聯
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