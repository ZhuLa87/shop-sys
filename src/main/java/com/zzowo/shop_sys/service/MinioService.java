package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.config.MinioConfig;
import com.zzowo.shop_sys.dto.request.upload.PresignRequest;
import com.zzowo.shop_sys.dto.response.upload.PresignResponse;
import com.zzowo.shop_sys.exception.BusinessException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private static final int PRESIGN_EXPIRY_MINUTES = 15;

    // 副檔名只由白名單內的 Content-Type 決定, 不採用使用者提供的檔名
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    // buildObjectKey 產生的檔名: 32 位小寫 hex (去掉連字號的 UUID) + 白名單副檔名
    private static final Pattern UPLOADED_FILENAME = Pattern.compile(
            "[0-9a-f]{32}\\.(" + String.join("|", EXTENSIONS.values()) + ")");

    public PresignResponse generatePresignedUrl(PresignRequest request) {
        String contentType = request.getContentType();
        String ext = EXTENSIONS.get(contentType);
        if (ext == null) {
            throw new BusinessException("不支援的檔案類型: " + contentType);
        }
        String objectKey = buildObjectKey(request.getType(), request.getResourceId(), ext);
        String uploadUrl = buildPresignedPutUrl(objectKey, contentType);
        String publicUrl = buildPublicUrl(objectKey);
        return new PresignResponse(uploadUrl, publicUrl, objectKey);
    }

    // 判斷 URL 是否為透過 presign 上傳到指定目錄下的物件 (避免使用者把任意 URL 存成頭像).
    // 只比對前綴不夠: "avatars/7/../8/x.png" 會被瀏覽器正規化成別的路徑, 甚至跳出 bucket,
    // 所以前綴之後必須完全符合 buildObjectKey 產生的檔名格式, 不允許子目錄, "..", query 或 fragment
    public boolean isUploadedObjectUrl(String url, String keyPrefix) {
        String prefix = buildPublicUrl(keyPrefix);
        return url != null && url.startsWith(prefix)
                && UPLOADED_FILENAME.matcher(url.substring(prefix.length())).matches();
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

    // Content-Type 納入簽章: 上傳時帶其他 Content-Type (例如 text/html) 會被 MinIO 拒絕,
    // 避免公開 bucket 把上傳的檔案當成網頁提供
    private String buildPresignedPutUrl(String objectKey, String contentType) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .expiry(PRESIGN_EXPIRY_MINUTES, TimeUnit.MINUTES)
                            .extraHeaders(Map.of("Content-Type", contentType))
                            .build()
            );
        } catch (Exception e) {
            log.error("無法產生 MinIO presigned URL, objectKey={}", objectKey, e);
            throw new BusinessException("無法產生上傳授權,請稍後再試");
        }
    }
}
