package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.config.MinioConfig;
import com.zzowo.shop_sys.dto.request.upload.PresignRequest;
import com.zzowo.shop_sys.dto.response.upload.PresignResponse;
import com.zzowo.shop_sys.exception.BusinessException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private static final int PRESIGN_EXPIRY_MINUTES = 15;

    public PresignResponse generatePresignedUrl(PresignRequest request) {
        String ext = extractExtension(request.getFilename());
        String objectKey = buildObjectKey(request.getType(), request.getResourceId(), ext);
        String uploadUrl = buildPresignedPutUrl(objectKey);
        String publicUrl = buildPublicUrl(objectKey);
        return new PresignResponse(uploadUrl, publicUrl, objectKey);
    }

    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.warn("MinIO 物件刪除失敗: {}", objectKey, e);
        }
    }

    public String buildPublicUrl(String objectKey) {
        return minioConfig.getPublicUrl() + "/" + minioConfig.getBucketName() + "/" + objectKey;
    }

    private String buildObjectKey(String type, Long resourceId, String ext) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return switch (type) {
            case "product-cover" -> String.format("products/%d/cover/%s.%s", resourceId, uuid, ext);
            case "product-image" -> String.format("products/%d/images/%s.%s", resourceId, uuid, ext);
            case "avatar" -> String.format("avatars/%d/%s.%s", resourceId, uuid, ext);
            default -> throw new BusinessException("不支援的上傳類型: " + type);
        };
    }

    private String buildPresignedPutUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .expiry(PRESIGN_EXPIRY_MINUTES, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("無法產生 MinIO presigned URL, objectKey={}", objectKey, e);
            throw new BusinessException("無法產生上傳授權,請稍後再試");
        }
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot >= filename.length() - 1) return "jpg";
        return filename.substring(dot + 1).toLowerCase();
    }
}
