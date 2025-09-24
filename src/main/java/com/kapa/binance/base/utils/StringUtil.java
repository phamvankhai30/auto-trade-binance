package com.kapa.binance.base.utils;

import java.security.SecureRandom;

public class StringUtil {

    private static final char[] CHARACTERS = "0123456789".toCharArray();
    private static final int DEFAULT_LENGTH = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String random() {
        return randomWithPrefix("", DEFAULT_LENGTH);
    }

    public static String random(String prefix) {
        return randomWithPrefix(prefix, DEFAULT_LENGTH);
    }

    public static String random(int length) {
        return randomWithPrefix("", length);
    }

    public static String random(String prefix, int length) {
        return randomWithPrefix(prefix, length);
    }

    private static String randomWithPrefix(String prefix, int length) {
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            result[i] = CHARACTERS[RANDOM.nextInt(CHARACTERS.length)];
        }
        return prefix + new String(result);
    }

    public static String paddingLeft(String input, int length, char padChar) {
        if (input == null) input = "";
        if (input.length() >= length) return input;

        return String.valueOf(padChar).repeat(length - input.length()) + input;
    }

    public static String paddingLeft(String input, int length) {
        return paddingLeft(input, length, '0');
    }
}
