package com.kapa.binance.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FuturesBalance {
    private String accountAlias;
    private String asset;
    private Double balance;
    private String crossWalletBalance;
    private String crossUnPnl;
    private String availableBalance;
    private String maxWithdrawAmount;
    private boolean marginAvailable;
    private long updateTime;
}
