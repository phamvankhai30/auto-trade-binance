package com.kapa.binance.service;

import com.kapa.binance.model.dtos.AuthRequest;

public interface OrderService {
    void receiveMessage(AuthRequest authRequest, String message);
}
