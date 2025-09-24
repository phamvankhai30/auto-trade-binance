package com.kapa.binance.base.request;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class BasePageRequest {

    private static final int DEFAULT_CURRENT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 1000;

    private int page = DEFAULT_CURRENT_PAGE;
    private int size = DEFAULT_PAGE_SIZE;

    public void setPage(int page) {
        this.page = Math.max(page, DEFAULT_CURRENT_PAGE);
    }

    public void setSize(int size) {
        if (size > MAX_PAGE_SIZE) this.size = MAX_PAGE_SIZE;
        else if (size > 0 && size <= DEFAULT_PAGE_SIZE) this.size = size;
        else this.size = Math.max(size, DEFAULT_PAGE_SIZE);
    }

    @Hidden
    public PageRequest getPageable() {
        return PageRequest.of(this.getPage(), this.getSize());
    }

    @Hidden
    public PageRequest getPageable(Sort sort) {
        return PageRequest.of(this.getPage(), this.getSize(), sort);
    }

    @Hidden
    public PaginationRequest getPagination() {
        return PaginationRequest.of(this.page * this.size, this.size);
    }
}