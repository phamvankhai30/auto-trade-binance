package com.kapa.binance.enums;

public enum SideEnum {
    BUY, SELL;

    public static SideEnum fromString(String name) {
        // Kiểm tra chuỗi đầu vào có hợp lệ hay không
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        // So sánh tên không phân biệt hoa thường
        for (SideEnum side : SideEnum.values()) {
            if (side.name().equalsIgnoreCase(name)) {
                return side;
            }
        }

        // Nếu không khớp, ném ra ngoại lệ
        throw new IllegalArgumentException("No enum constant for name: " + name);
    }
}


