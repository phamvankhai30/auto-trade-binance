package com.kapa.binance.base.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginationRequest {
    private int offset;
    private int limit;

    public static PaginationRequest of(int start, int end) {
        return PaginationRequest.builder().offset(start).limit(end).build();
    }
}
