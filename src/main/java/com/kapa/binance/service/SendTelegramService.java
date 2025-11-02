package com.kapa.binance.service;

import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.model.response.DataOrder;

public interface SendTelegramService {
    void sendMessage(String message);
    void sendDisconnectMessage(AuthRequest authRequest);
    void sendTakeProfitMessage(AuthRequest authRequest, DataOrder order);
}
