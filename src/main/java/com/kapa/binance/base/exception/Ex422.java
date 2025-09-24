package com.kapa.binance.base.exception;

import com.kapa.binance.base.constants.CodeConstant;
import com.kapa.binance.base.response.BaseErrorResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class Ex422 extends CustomException {

    public Ex422(String code, String message) {
        super(code, message);
    }

    public Ex422(String message) {
        super(CodeConstant.CODE_422, message);
    }

    public Ex422(@NotNull BaseErrorResponse error) {
        super(error);
    }

    public Ex422(String message, @NotEmpty List<BaseErrorResponse> error) {
        super(CodeConstant.CODE_422, message, error);
    }

    public Ex422(String message, Object error) {
        super(CodeConstant.CODE_422, message, error);
    }
}
