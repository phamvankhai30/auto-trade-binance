package com.kapa.binance.enums;

public enum CopyOrderTypeEnum {
    UNKNOWN,
    MARKET_ORDER,
    LIMIT_ORDER,
    STOP_LOSS,
    TAKE_PROFIT,
    CLOSE_POSITION;

    public static CopyOrderTypeEnum detectOrderType(String positionSide, String orderType,
                                                    String side, Boolean closePosition) {

        // Nếu đang giao dịch với vị thế LONG
        if ("LONG".equals(positionSide)) {

            // Mở vị thế LONG bằng lệnh thị trường
            if ("MARKET".equals(orderType) && "BUY".equals(side)) {
                return MARKET_ORDER;
            }

            // Mở vị thế LONG bằng lệnh giới hạn
            if ("LIMIT".equals(orderType) && "BUY".equals(side)) {
                return LIMIT_ORDER;
            }

            // Đặt Stop Loss cho vị thế LONG
            if ("STOP_MARKET".equals(orderType) && "SELL".equals(side)) {
                return STOP_LOSS;
            }

            // Đặt Take Profit cho vị thế LONG
            if ("TAKE_PROFIT_MARKET".equals(orderType) && "SELL".equals(side)) {
                return TAKE_PROFIT;
            }

            // Đóng vị thế LONG bằng lệnh thị trường
            if ("MARKET".equals(orderType) && "SELL".equals(side) && Boolean.FALSE.equals(closePosition)) {
                return CLOSE_POSITION;
            }
        }

        // Nếu đang giao dịch với vị thế SHORT
        if ("SHORT".equals(positionSide)) {

            // Mở vị thế SHORT bằng lệnh thị trường
            if ("MARKET".equals(orderType) && "SELL".equals(side)) {
                return MARKET_ORDER;
            }

            // Mở vị thế SHORT bằng lệnh giới hạn
            if ("LIMIT".equals(orderType) && "SELL".equals(side)) {
                return LIMIT_ORDER;
            }

            // Đặt Stop Loss cho vị thế SHORT
            if ("STOP_MARKET".equals(orderType) && "BUY".equals(side)) {
                return STOP_LOSS;
            }

            // Đặt Take Profit cho vị thế SHORT
            if ("TAKE_PROFIT_MARKET".equals(orderType) && "BUY".equals(side)) {
                return TAKE_PROFIT;
            }

            // Đóng vị thế SHORT bằng lệnh thị trường
            if ("MARKET".equals(orderType) && "BUY".equals(side) && Boolean.FALSE.equals(closePosition)) {
                return CLOSE_POSITION;
            }
        }

        // Nếu không khớp bất kỳ điều kiện nào thì UNKNOWN
        return UNKNOWN;
    }
}
