package com.zzowo.shop_sys.dto.response.product;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class ProductResponse {
    // 商品 ID (前端點擊商品進入詳情頁時需要用到)
    private Long id;

    // 商品名稱
    private String name;

    // 商品描述
    private String description;

    // 價格
    private BigDecimal price;

    // 庫存數量 (前端可以用來判斷顯示 "缺貨" 或 "剩餘 N 件")
    private Integer stockQuantity;

    // 封面圖 (列表頁顯示用)
    private String coverImageUrl;

    // 商品圖片集 (詳情頁的輪播圖，只存 URL 字串即可)
    private List<String> imageUrls;

    // 狀態 (雖然是用戶端，但有時候前端需要知道是 '缺貨' 還是 '上架中' 來決定按鈕狀態)
    private String status;
}
