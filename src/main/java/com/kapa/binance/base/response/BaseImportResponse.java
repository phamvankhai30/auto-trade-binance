package com.kapa.binance.base.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseImportResponse<S, E> {

    private S success;
    private E error;
    private byte[] file;
}
