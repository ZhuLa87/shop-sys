package com.zzowo.shop_sys.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String publicUrl;
    private String region = "us-east-1";

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                // 明確指定 region 讓 SDK 略過 GetBucketLocation 這次網路呼叫,
                // 產生 presigned URL 才會是純本地運算 (否則每次簽章都要先連到 endpoint,
                // 在容器環境下 endpoint 是對外位址,會多繞一圈甚至連不通)
                .region(region)
                .credentials(accessKey, secretKey)
                .build();
    }
}
