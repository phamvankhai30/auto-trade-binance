package com.kapa.binance.enums;

public enum PositionSideEnum {
    LONG, SHORT, BOTH;

    public static PositionSideEnum fromString(String name) {
        // Kiểm tra nếu chuỗi truyền vào là null hoặc rỗng
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        // Lấy giá trị tương ứng của enum, không phân biệt chữ hoa chữ thường
        for (PositionSideEnum position : PositionSideEnum.values()) {
            if (position.name().equalsIgnoreCase(name)) {
                return position;
            }
        }

        // Nếu không có giá trị nào khớp, ném ngoại lệ
        throw new IllegalArgumentException("No enum constant for name: " + name);
    }
}
