package com.kapa.binance.service;

import com.kapa.binance.model.dtos.AuthRequest;

import java.util.List;

public interface UserService {
    void updateStatusConnectAsync(List<String> uuid, String connectStatus);
    void updateStatusConnectSync(List<String> uuid, String connectStatus);
    List<AuthRequest> getAllUserAuth();
    AuthRequest getUserAuthByUuid(String uuid);
    AuthRequest getUserAuthByUuidWithError(String uuid);
}
