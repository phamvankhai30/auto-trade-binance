package com.kapa.binance.base.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class AesEncrypt {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding"; // AES with CBC mode and PKCS5 padding
    private static final int IV_SIZE = 16; // AES block size is 16 bytes

    // Tạo IV ngẫu nhiên
    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    // Chuyển khóa từ chuỗi sang SecretKey
    public static SecretKey getSecretKeyFromString(String keyString) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(keyString.getBytes(StandardCharsets.UTF_8));
        return new javax.crypto.spec.SecretKeySpec(key, "AES");
    }

    // Mã hóa văn bản bằng AES
    public static String encrypt(String text, String keyString) {
        try {
            SecretKey secretKey = getSecretKeyFromString(keyString);
            IvParameterSpec iv = generateIv();

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            byte[] encryptedBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            byte[] encryptedIvAndText = new byte[IV_SIZE + encryptedBytes.length];

            System.arraycopy(iv.getIV(), 0, encryptedIvAndText, 0, IV_SIZE);
            System.arraycopy(encryptedBytes, 0, encryptedIvAndText, IV_SIZE, encryptedBytes.length);
            return Base64.getEncoder().encodeToString(encryptedIvAndText);
        } catch (Exception e) {
            return null;
        }
    }

    // Giải mã văn bản bằng AES
    public static String decrypt(String encryptedText, String keyString) {
        try {
            byte[] encryptedIvAndText = Base64.getDecoder().decode(encryptedText);
            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(encryptedIvAndText, 0, iv, 0, IV_SIZE);

            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            byte[] encryptedBytes = new byte[encryptedIvAndText.length - IV_SIZE];
            System.arraycopy(encryptedIvAndText, IV_SIZE, encryptedBytes, 0, encryptedBytes.length);

            SecretKey secretKey = getSecretKeyFromString(keyString);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
    }
}
