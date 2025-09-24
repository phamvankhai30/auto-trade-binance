package com.kapa.binance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConnectedEnum {
    DISCONNECTED(0),
    CONNECTED(1),
    SERVICE_UNAVAILABLE(2);

    private final int code;

}
