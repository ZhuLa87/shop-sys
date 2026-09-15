package com.zzowo.shop_sys.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 測試向量取自綠界官方 ECPay-API-Skill test-vectors/checkmacvalue.json (SHA256 部分)
 */
class EcpayCheckMacValueTest {

    private static final String HASH_KEY = "pwFHCqoQZGmho4w6";
    private static final String HASH_IV = "EkRm7iFT261dpevs";

    @Test
    void generate_aioBaseline_matchesOfficialVector() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("MerchantID", "3002607");
        params.put("MerchantTradeNo", "Test1234567890");
        params.put("MerchantTradeDate", "2025/01/01 12:00:00");
        params.put("PaymentType", "aio");
        params.put("TotalAmount", "100");
        params.put("TradeDesc", "測試");
        params.put("ItemName", "測試商品");
        params.put("ReturnURL", "https://example.com/notify");
        params.put("ChoosePayment", "ALL");
        params.put("EncryptType", "1");

        assertThat(EcpayCheckMacValue.generate(params, HASH_KEY, HASH_IV))
                .isEqualTo("291CBA324D31FB5A4BBBFDF2CFE5D32598524753AFD4959C3BF590C5B2F57FB2");
    }

    @Test
    void generate_apostrophe_isEncodedAs27() {
        assertThat(EcpayCheckMacValue.generate(itemParams("Tom's Shop", "100"), HASH_KEY, HASH_IV))
                .isEqualTo("CF0A3D4901D99459D8641516EC57210700E8A5C9AB26B1D021301E9CB93EF78D");
    }

    @Test
    void generate_tilde_isEncodedAs7e() {
        assertThat(EcpayCheckMacValue.generate(itemParams("Test~Product", "200"), HASH_KEY, HASH_IV))
                .isEqualTo("CEEAE01D2F9A8E74D4AC0DCE7735B046D73F35A5EC99558A31A2EE03159DA1C9");
    }

    @Test
    void generate_space_isEncodedAsPlus() {
        assertThat(EcpayCheckMacValue.generate(itemParams("My Test Product", "300"), HASH_KEY, HASH_IV))
                .isEqualTo("7712A5E6EDC3B57086063C88568084C66CE882A21D40E74DE5ACA3B478C6F316");
    }

    @Test
    void generate_ignoresKeyOrderAndExistingCheckMacValue() {
        Map<String, String> ordered = itemParams("My Test Product", "300");
        Map<String, String> shuffled = new HashMap<>();
        shuffled.put("TotalAmount", "300");
        shuffled.put("ItemName", "My Test Product");
        shuffled.put("MerchantID", "3002607");
        shuffled.put("CheckMacValue", "SHOULD_BE_IGNORED");

        assertThat(EcpayCheckMacValue.generate(shuffled, HASH_KEY, HASH_IV))
                .isEqualTo(EcpayCheckMacValue.generate(ordered, HASH_KEY, HASH_IV));
    }

    @Test
    void verify_officialCallbackVector_returnsTrue() {
        Map<String, String> callback = callbackParams();
        callback.put("CheckMacValue", "2AB536D86AFF8E1086744D59175040A32538C96B1C28C4135B551BD728E913B8");

        assertThat(EcpayCheckMacValue.verify(callback, HASH_KEY, HASH_IV)).isTrue();
    }

    @Test
    void verify_tamperedAmount_returnsFalse() {
        Map<String, String> callback = callbackParams();
        callback.put("CheckMacValue", "2AB536D86AFF8E1086744D59175040A32538C96B1C28C4135B551BD728E913B8");
        callback.put("TradeAmt", "1");

        assertThat(EcpayCheckMacValue.verify(callback, HASH_KEY, HASH_IV)).isFalse();
    }

    @Test
    void verify_missingCheckMacValue_returnsFalse() {
        assertThat(EcpayCheckMacValue.verify(callbackParams(), HASH_KEY, HASH_IV)).isFalse();
    }

    @Test
    void verify_wrongHashKey_returnsFalse() {
        Map<String, String> callback = callbackParams();
        callback.put("CheckMacValue", "2AB536D86AFF8E1086744D59175040A32538C96B1C28C4135B551BD728E913B8");

        assertThat(EcpayCheckMacValue.verify(callback, "wrongHashKey0000", HASH_IV)).isFalse();
    }

    private Map<String, String> itemParams(String itemName, String totalAmount) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("MerchantID", "3002607");
        params.put("ItemName", itemName);
        params.put("TotalAmount", totalAmount);
        return params;
    }

    private Map<String, String> callbackParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("MerchantID", "3002607");
        params.put("MerchantTradeNo", "Test1234567890");
        params.put("RtnCode", "1");
        params.put("RtnMsg", "Succeeded");
        params.put("TradeNo", "2301011234567890");
        params.put("TradeAmt", "100");
        params.put("PaymentDate", "2025/01/01 12:05:00");
        params.put("PaymentType", "Credit_CreditCard");
        params.put("TradeDate", "2025/01/01 12:00:00");
        params.put("SimulatePaid", "0");
        return params;
    }
}
