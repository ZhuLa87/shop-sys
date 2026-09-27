package com.zzowo.shop_sys.controller;

import com.zzowo.shop_sys.dto.request.upload.PresignRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.upload.PresignResponse;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.service.MinioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final MinioService minioService;

    private static final List<String> PRODUCT_TYPES = List.of("product-cover", "product-image");
    private static final Set<String> PRODUCT_UPLOAD_AUTHORITIES = Set.of(
            Role.PRODUCT_MANAGER.getAuthority(), Role.SUPER_ADMIN.getAuthority());

    @PostMapping("/presign")
    public ResponseEntity<ApiResponse<PresignResponse>> presign(
            @Valid @RequestBody PresignRequest request,
            Authentication authentication) {

        // 商品圖片類型需要 PRODUCT_MANAGER 或 SUPER_ADMIN 角色
        if (PRODUCT_TYPES.contains(request.getType())) {
            boolean hasRole = authentication.getAuthorities().stream()
                    .anyMatch(a -> PRODUCT_UPLOAD_AUTHORITIES.contains(a.getAuthority()));
            if (!hasRole) {
                throw new BusinessException("權限不足,無法上傳商品圖片", HttpStatus.FORBIDDEN);
            }
        }

        PresignResponse response = minioService.generatePresignedUrl(request);
        return ResponseEntity.ok(ApiResponse.success("已產生上傳授權", response));
    }
}
