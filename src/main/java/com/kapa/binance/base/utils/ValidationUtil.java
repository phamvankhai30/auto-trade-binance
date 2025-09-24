package com.kapa.binance.base.utils;

import java.util.List;
import java.util.Objects;

public class ValidationUtil {

    /**
     * Kiểm tra các số truyền vào có khác null hay không.
     * Trả về false nếu có số null.
     * Cho phép số âm hoặc 0.
     */
    public static boolean allNotNulNotZero(Number... numbers) {
        if (numbers == null || numbers.length == 0) return false;
        for (Number num : numbers) {
            if (num == null) return false;
        }
        return true;
    }

    public static boolean isNulOrZero(Number... numbers) {
        if (numbers == null || numbers.length == 0) return true;
        for (Number num : numbers) {
            if (num == null) return true;
        }
        return false;
    }

    public static <T> boolean hasValue(List<T> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        return list.stream().anyMatch(Objects::nonNull);
    }
}
