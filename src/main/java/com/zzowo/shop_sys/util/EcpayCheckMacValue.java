package com.zzowo.shop_sys.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * 綠界 AIO 金流 CheckMacValue (SHA256)
 * Source: https://developers.ecpay.com.tw/2902.md (2026-09-15),
 * 演算法對應官方 PHP SDK CheckMacValueService::generate() 與 UrlService::ecpayUrlEncode()
 *
 * 注意:這裡的 URL encode 僅適用於 CheckMacValue,與 AES-JSON 服務 (站內付/發票) 的編碼規則不同,不可混用.
 */
public final class EcpayCheckMacValue {

    public static final String FIELD = "CheckMacValue";

    private EcpayCheckMacValue() {
    }

    public static String generate(Map<String, String> params, String hashKey, String hashIv) {
        // 1. 排除 CheckMacValue,key 不分大小寫依字典序排序
        TreeMap<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        params.forEach((k, v) -> {
            if (!FIELD.equals(k)) sorted.put(k, v);
        });

        // 2. HashKey=...&k1=v1&...&HashIV=...
        StringJoiner joiner = new StringJoiner("&");
        sorted.forEach((k, v) -> joiner.add(k + "=" + v));
        String raw = "HashKey=" + hashKey + "&" + joiner + "&HashIV=" + hashIv;

        // 3. ECPay 專用 URL encode → 4. SHA256 → 5. 轉大寫
        byte[] digest = sha256().digest(ecpayUrlEncode(raw).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().withUpperCase().formatHex(digest);
    }

    /**
     * 驗證綠界回傳的 CheckMacValue,使用 timing-safe 比較
     */
    public static boolean verify(Map<String, String> params, String hashKey, String hashIv) {
        String received = params.get(FIELD);
        if (received == null || received.isEmpty()) return false;

        String calculated = generate(params, hashKey, hashIv);
        return MessageDigest.isEqual(
                received.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                calculated.getBytes(StandardCharsets.UTF_8));
    }

    // urlencode (空格為 +) → 全部轉小寫 → 還原 .NET 不編碼的字元
    static String ecpayUrlEncode(String source) {
        return URLEncoder.encode(source, StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT)
                .replace("%2d", "-")
                .replace("%5f", "_")
                .replace("%2e", ".")
                .replace("%21", "!")
                .replace("%2a", "*")
                .replace("%28", "(")
                .replace("%29", ")")
                // PHP urlencode('~') 會輸出 %7E,URLEncoder 不編碼 ~,需補上
                .replace("~", "%7e");
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // 每個 JVM 都必須支援 SHA-256,走到這裡代表執行環境有問題
            throw new IllegalStateException(e);
        }
    }
}
