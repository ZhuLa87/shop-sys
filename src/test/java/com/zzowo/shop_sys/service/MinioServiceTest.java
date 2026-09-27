package com.zzowo.shop_sys.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.zzowo.shop_sys.config.MinioConfig;
import com.zzowo.shop_sys.dto.request.upload.PresignRequest;
import com.zzowo.shop_sys.dto.response.upload.PresignResponse;
import com.zzowo.shop_sys.exception.BusinessException;

// 使用真的 MinioClient: 已指定 region, 產生 presigned URL 是純本地運算, 不需要連線
class MinioServiceTest {

    MinioService minioService;

    @BeforeEach
    void setUp() {
        MinioConfig config = new MinioConfig();
        config.setEndpoint("http://localhost:9000");
        config.setAccessKey("test-access");
        config.setSecretKey("test-secret");
        config.setBucketName("shop-sys-public");
        config.setPublicUrl("http://cdn.test");
        config.setRegion("us-east-1");
        minioService = new MinioService(config.minioClient(), config);
    }

    @ParameterizedTest
    @CsvSource({"image/jpeg,jpg", "image/png,png", "image/webp,webp", "image/gif,gif"})
    void presign_extensionComesFromContentType_notFilename(String contentType, String ext) {
        PresignResponse res = minioService.generatePresignedUrl(request("avatar", 7L, "evil.html", contentType));

        assertThat(res.getObjectKey()).startsWith("avatars/7/").endsWith("." + ext);
        assertThat(res.getPublicUrl()).isEqualTo("http://cdn.test/shop-sys-public/" + res.getObjectKey());
    }

    @Test
    void presign_signsContentTypeHeader() {
        PresignResponse res = minioService.generatePresignedUrl(request("product-cover", 3L, "a.png", "image/png"));

        // 上傳時若改用其他 Content-Type (例如 text/html), 簽章會不符而被 MinIO 拒絕
        String url = URLDecoder.decode(res.getUploadUrl(), StandardCharsets.UTF_8);
        assertThat(url).contains("X-Amz-SignedHeaders=content-type;host");
    }

    @Test
    void isUploadedObjectUrl_acceptsUrlProducedByPresign() {
        PresignResponse res = minioService.generatePresignedUrl(request("avatar", 7L, "a.png", "image/png"));

        assertThat(minioService.isUploadedObjectUrl(res.getPublicUrl(), "avatars/7/")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://cdn.test/shop-sys-public/avatars/8/0123456789abcdef0123456789abcdef.png",         // 別人的目錄
            "https://evil.test/shop-sys-public/avatars/7/0123456789abcdef0123456789abcdef.png",       // 別的主機
            "http://cdn.test/shop-sys-public/avatars/7/../8/0123456789abcdef0123456789abcdef.png",    // 路徑穿越到別人
            "http://cdn.test/shop-sys-public/avatars/7/../../../other-bucket/0123456789abcdef0123456789abcdef.png", // 跳出 bucket
            "http://cdn.test/shop-sys-public/avatars/7/%2e%2e/8/0123456789abcdef0123456789abcdef.png", // 編碼過的 ..
            "http://cdn.test/shop-sys-public/avatars/7/sub/0123456789abcdef0123456789abcdef.png",     // 子目錄
            "http://cdn.test/shop-sys-public/avatars/7/0123456789abcdef0123456789abcdef.html",        // 非白名單副檔名
            "http://cdn.test/shop-sys-public/avatars/7/0123456789abcdef0123456789abcdef.png?x=1",     // query
            "http://cdn.test/shop-sys-public/avatars/7/0123456789abcdef0123456789abcdef.png#x",       // fragment
            "http://cdn.test/shop-sys-public/avatars/7/a.png"                                         // 非 presign 產生的檔名
    })
    void isUploadedObjectUrl_rejectsAnythingElse(String url) {
        assertThat(minioService.isUploadedObjectUrl(url, "avatars/7/")).isFalse();
    }

    @Test
    void isUploadedObjectUrl_null_returnsFalse() {
        assertThat(minioService.isUploadedObjectUrl(null, "avatars/7/")).isFalse();
    }

    @Test
    void presign_unsupportedContentType_throws() {
        assertThatThrownBy(() -> minioService.generatePresignedUrl(request("avatar", 7L, "a.html", "text/html")))
                .isInstanceOf(BusinessException.class);
    }

    private PresignRequest request(String type, Long resourceId, String filename, String contentType) {
        PresignRequest request = new PresignRequest();
        request.setType(type);
        request.setResourceId(resourceId);
        request.setFilename(filename);
        request.setContentType(contentType);
        return request;
    }
}
