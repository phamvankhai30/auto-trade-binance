package com.kapa.binance.base.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculatorUtil {

    /**
     * Tính giá khi đạt mức lỗ mong muốn cho một lệnh long.
     *
     * @param entryPrice     Giá vào lệnh ban đầu (giá trung bình).
     * @param lossPercentage Phần trăm lỗ mong muốn (ví dụ: 10%).
     * @param leverage       Đòn bẩy đang sử dụng cho lệnh.
     * @return Giá sẽ đạt khi lỗ theo tỷ lệ phần trăm mong muốn.
     */
    public static BigDecimal dcaLongPrice(double entryPrice, double lossPercentage,
                                      int leverage, double tickSize, int pricePrecision) {
        // Chuyển phần trăm lỗ thành hệ số lỗ (lấy phần trăm chia cho 100).
        double lossFactor = lossPercentage / 100;

        // Công thức: entryPrice - (entryPrice * (lossFactor / leverage))
        double price = entryPrice - entryPrice * (lossFactor / leverage);
        return roundValue(price, tickSize, pricePrecision);
    }

    /**
     * Tính giá khi đạt mức lỗ mong muốn cho một lệnh short.
     *
     * @param entryPrice     Giá vào lệnh ban đầu (giá trung bình).
     * @param lossPercentage Phần trăm lỗ mong muốn (ví dụ: 10%).
     * @param leverage       Đòn bẩy đang sử dụng cho lệnh.
     * @return Giá sẽ đạt khi lỗ theo tỷ lệ phần trăm mong muốn.
     */
    public static BigDecimal dcaShortPrice(double entryPrice, double lossPercentage,
                                       int leverage, double tickSize, int pricePrecision) {
        // Chuyển phần trăm lỗ thành hệ số lỗ (lấy phần trăm chia cho 100).
        double lossFactor = lossPercentage / 100;

        // Công thức: entryPrice + entryPrice * (lossFactor / leverage)
        double price = entryPrice + entryPrice * (lossFactor / leverage);
        return roundValue(price, tickSize, pricePrecision);
    }


    /**
     * Tính giá khi đạt mức lợi nhuận mong muốn cho một lệnh long.
     *
     * @param entryPrice       Giá vào lệnh ban đầu (giá trung bình).
     * @param profitPercentage Phần trăm lợi nhuận mong muốn (ví dụ: 10%).
     * @param leverage         Đòn bẩy đang sử dụng cho lệnh.
     * @return Giá sẽ đạt khi có lợi nhuận theo tỷ lệ phần trăm mong muốn.
     */
    public static BigDecimal takeProfitLongPrice(double entryPrice, double profitPercentage,
                                             int leverage, double tickSize, int pricePrecision) {
        // Chuyển phần trăm lợi nhuận thành hệ số lợi nhuận (lấy phần trăm chia cho 100).
        double profitFactor = profitPercentage / 100;

        // Công thức: entryPrice + entryPrice * (profitFactor / leverage)
        double price = entryPrice + entryPrice * (profitFactor / leverage);

        // Làm tròn giá
        return roundValue(price, tickSize, pricePrecision);
    }

    /**
     * Tính giá khi đạt mức lợi nhuận mong muốn cho một lệnh short.
     *
     * @param entryPrice       Giá vào lệnh ban đầu (giá trung bình).
     * @param profitPercentage Phần trăm lợi nhuận mong muốn (ví dụ: 10%).
     * @param leverage         Đòn bẩy đang sử dụng cho lệnh.
     * @return Giá sẽ đạt khi có lợi nhuận theo tỷ lệ phần trăm mong muốn.
     */
    public static BigDecimal takeProfitShortPrice(double entryPrice, double profitPercentage,
                                              int leverage, double tickSize, int pricePrecision) {
        // Chuyển phần trăm lợi nhuận thành hệ số lợi nhuận (lấy phần trăm chia cho 100).
        double profitFactor = profitPercentage / 100;

        // Công thức: entryPrice - entryPrice * (profitFactor / leverage)
        double price = entryPrice - entryPrice * (profitFactor / leverage);

        // Làm tròn giá
        return roundValue(price, tickSize, pricePrecision);
    }

    public static BigDecimal priceDrop(double avgPrice, double percentage, int leverage, double tickSize,
                                       int pricePrecision, boolean isLong) {
        double adjustmentFactor = percentage / (leverage * 100.0);
        double price = isLong
                ? avgPrice * (1 + adjustmentFactor)  // Long -> giá tăng để có lãi
                : avgPrice * (1 - adjustmentFactor); // Short -> giá giảm để có lãi
        return roundValue(price, tickSize, pricePrecision);
    }

    public static double reduceQuantity(double currentQuantity, double reducePercentage, double stepSize, int pricePrecision) {
        if (reducePercentage < 0 || reducePercentage > 100) return 0.0;
        double quantity =  Math.abs(currentQuantity) * (1 - reducePercentage / 100);
        return roundValue(quantity, stepSize, pricePrecision).doubleValue();
    }

    public static BigDecimal quantity(double volume, double price, double stepSize, int quantityPrecision) {
        double token = volume / price;
        return roundValue(token, stepSize, quantityPrecision);
    }

    public static BigDecimal roundValue(double value, double size, int precision) {
        BigDecimal bigValue = BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP);
        BigDecimal step = BigDecimal.valueOf(size);

        // Tính phần dư
        BigDecimal remainder = bigValue.remainder(step);

        // Nếu giá trị đã chia hết cho stepSize, trả về giá trị đã làm tròn
        if (remainder.compareTo(BigDecimal.ZERO) == 0) {
            return bigValue;
        }

        // Điều chỉnh giá trị để chia hết cho stepSize
        return bigValue.subtract(remainder).setScale(precision, RoundingMode.HALF_UP);
    }
}

