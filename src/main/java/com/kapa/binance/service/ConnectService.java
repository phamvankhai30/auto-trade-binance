package com.kapa.binance.service;

import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.model.request.ConnectRequest;
import com.kapa.binance.model.response.ConnectionResponse;

import java.util.concurrent.CompletableFuture;

public interface ConnectService {

    void apiOpenConnect(ConnectRequest request);

    void apiCloseConnect(String uuid);

    CompletableFuture<ConnectionResponse> openConnect(AuthRequest request, boolean isReconnect);

}
