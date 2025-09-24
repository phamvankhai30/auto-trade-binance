package com.kapa.binance.base.exception;

import com.kapa.binance.base.constants.CodeConstant;
import com.kapa.binance.base.constants.MessageConstant;
import com.kapa.binance.base.response.BaseErrorResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class Ex400 extends CustomException {

    public Ex400() {
        super(CodeConstant.CODE_400, MessageConstant.BAD_REQUEST);
    }

    public Ex400(String message) {
        super(CodeConstant.CODE_400, message);
    }

    public Ex400(@NotNull BaseErrorResponse error) {
        super(error.getCode(), error.getMessage());
    }

    public Ex400(String message, @NotEmpty List<BaseErrorResponse> error) {
        super(CodeConstant.CODE_400, message, error);
    }

    public Ex400(String message, Object error) {
        super(CodeConstant.CODE_400, message, error);
    }
}
