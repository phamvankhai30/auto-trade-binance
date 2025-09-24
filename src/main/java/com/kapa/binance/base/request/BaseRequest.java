package com.kapa.binance.base.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseRequest<TData> extends BasePageRequest {

    @NotNull
    @Valid
    private TData data;

    public TData initDataIfNull(Class<TData> clazz) {
        if (data == null) {
            try {
                data = clazz.getDeclaredConstructor().newInstance(); // Gán lại vào data luôn
            } catch (Exception e) {
                throw new RuntimeException("Cannot create instance of " + clazz.getName(), e);
            }
        }
        return data;
    }
}

