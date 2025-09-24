package com.kapa.binance.base.utils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateTimeUtil {

    private DateTimeUtil() {
        // Private constructor để tránh khởi tạo đối tượng
    }

    /**
     * Get the current timestamp in ISO 8601 format (UTC) with millisecond precision.
     * Example: 2020-12-08T09:08:57.715Z
     */
    public static String getCurrentTimestamp() {
        return DateTimeFormatter.ISO_INSTANT
                .format(Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    public static long getUnixEpochTime() {
        return Instant.now().getEpochSecond();
    }
}
