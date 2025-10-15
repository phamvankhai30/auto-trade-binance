package com.kapa.binance.service;

import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.model.response.DataOrder;

public interface CopierService {

    void sendCopierOrder(DataOrder dataOrder, AuthRequest leaderAuthRequest);
}
