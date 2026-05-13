package com.zzowo.shop_sys.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "分頁回應格式")
@Getter
public class PageResponse<T> {

    @Schema(description = "當頁資料列表")
    private final List<T> content;

    @Schema(description = "當前頁碼（從 0 開始）", example = "0")
    private final int page;

    @Schema(description = "每頁筆數", example = "20")
    private final int size;

    @Schema(description = "總筆數", example = "100")
    private final long totalElements;

    @Schema(description = "總頁數", example = "5")
    private final int totalPages;

    @Schema(description = "是否為最後一頁", example = "false")
    private final boolean last;

    public PageResponse(Page<T> pageData) {
        this.content = pageData.getContent();
        this.page = pageData.getNumber();
        this.size = pageData.getSize();
        this.totalElements = pageData.getTotalElements();
        this.totalPages = pageData.getTotalPages();
        this.last = pageData.isLast();
    }
}
