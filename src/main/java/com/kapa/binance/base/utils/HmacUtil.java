package com.kapa.binance.base.utils;

import org.apache.tomcat.util.buf.HexUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private HmacUtil() {
    }


    // Trả về HMAC-SHA256 dưới dạng Hex (Binance yêu cầu kiểu này)
    public static String getSignature(String data, String key) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), HMAC_SHA256);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hmacSha256 = mac.doFinal(data.getBytes());
            return HexUtils.toHexString(hmacSha256);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC-SHA256 (Hex)", e);
        }
    }
}
