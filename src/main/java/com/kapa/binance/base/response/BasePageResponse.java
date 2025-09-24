package com.kapa.binance.base.response;

import com.kapa.binance.base.request.BasePageRequest;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasePageResponse {
    private int page;
    private int size;
    private long totalRecords;
    private int totalPages;


    public static BasePageResponse of(int page, int size, int total) {
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return BasePageResponse.builder().page(page).size(size).totalRecords(total).totalPages(totalPages).build();
    }

    public static BasePageResponse of(int page, int size, long total) {
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return BasePageResponse.builder().page(page).size(size).totalRecords(total).totalPages(totalPages).build();
    }



    public static BasePageResponse of(BasePageRequest pageRequest, long total) {
        return pageRequest == null ? null : of(pageRequest.getPage(), pageRequest.getSize(), total);
    }
}