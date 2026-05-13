package com.zzowo.shop_sys.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "統一 API 回應格式")
@Data
public class ApiResponse<T> {

    @Schema(description = "是否成功", example = "true")
    private boolean success;

    @Schema(description = "回應訊息", example = "操作成功")
    private String message;

    @Schema(description = "回應資料（失敗時為 null）")
    private T data;

    public ApiResponse() {}

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
