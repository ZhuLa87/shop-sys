package com.zzowo.shop_sys.config;

import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 全域例外處理
public class GlobalExceptionHandler {

    // 捕捉自己丟出的 RuntimeException
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        // 回傳 400 Bad Request，並附上錯誤訊息
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    // 捕捉所有意料之外的 Exception (例如 NullPointerException、資料庫連線失敗)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        e.printStackTrace(); // 在後台印出錯誤堆疊，方便除錯
        // 回傳 500 Internal Server Error
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("伺服器發生未預期的錯誤，請聯繫管理員"));
    }

    // 捕捉"找不到資源"的例外
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
    }
}