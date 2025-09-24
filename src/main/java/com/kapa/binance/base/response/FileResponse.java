package com.kapa.binance.base.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileResponse {
    private String fileName;
    private byte[] fileData;
}
