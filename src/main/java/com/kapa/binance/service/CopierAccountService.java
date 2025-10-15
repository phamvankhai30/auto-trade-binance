package com.kapa.binance.service;

import com.kapa.binance.model.dtos.AuthRequest;

import java.util.List;

public interface CopierAccountService {

    List<AuthRequest> getCopierAuthByLeader(String leaderUuid);
}
