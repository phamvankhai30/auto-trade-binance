package com.kapa.binance.base.config;

public class LogContext {
    private static final ThreadLocal<String> logIdHolder = new ThreadLocal<>();

    public static void setLogId(String logId) {
        logIdHolder.set(logId);
    }

    public static String getLogId() {
        return logIdHolder.get();
    }

    public static void clear() {
        logIdHolder.remove();
    }
}
