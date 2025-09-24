package com.kapa.binance.base.exception;

import com.kapa.binance.base.response.BaseErrorResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CustomException extends RuntimeException {
    private @NotBlank String code;
    private @NotBlank String message;
    private Object error;

    public CustomException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.error = null;
    }

    public CustomException(@NotNull BaseErrorResponse error) {
        super(error.getMessage());
        this.code = error.getCode();
        this.message = error.getMessage();
        this.error = error;
    }

    public CustomException(String code, String message, @NotEmpty List<BaseErrorResponse> error) {
        super(message);
        this.code = code;
        this.message = message;
        this.error = error;
    }

    public CustomException(String code, String message, Object error) {
        super(message);
        this.code = code;
        this.message = message;
        this.error = error;
    }
}
