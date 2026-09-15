package com.zzowo.shop_sys.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * 綠界 AIO 金流設定
 * Source: https://developers.ecpay.com.tw/2862.md (2026-09-15)
 *
 * compose 對未設定的變數會傳入空字串 (而非不傳),yaml 的預設值不會生效,
 * 因此全部欄位都以 @NotBlank 在啟動時檢查,缺值直接啟動失敗.
 */
@Configuration
@ConfigurationProperties(prefix = "ecpay")
@Validated
@Data
public class EcpayConfig {

    // 特店編號,必須與 HashKey/HashIV 成對,不可混用其他服務 (物流/發票) 的帳號
    @NotBlank
    private String merchantId;
    @NotBlank
    private String hashKey;
    @NotBlank
    private String hashIv;

    // 綠界付款頁 (stage: payment-stage.ecpay.com.tw,正式: payment.ecpay.com.tw)
    @NotBlank
    private String checkoutUrl;

    // 綠界 server 打得到的公開 API base,含 context-path,例如 https://example.com/api
    // 限制:只能是 80/443 埠,不可放在 CDN 後方,不可用中文網域
    @NotBlank
    private String callbackBaseUrl;

    // 付款完成後把瀏覽器導回的前端 origin
    @NotBlank
    private String frontendBaseUrl;

    // MerchantTradeNo 前綴:測試帳號是所有開發者共用,前綴可降低與他人撞號的機會
    @NotBlank
    private String tradeNoPrefix = "SS";
}
