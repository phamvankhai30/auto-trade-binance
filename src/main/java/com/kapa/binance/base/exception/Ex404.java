package com.kapa.binance.base.exception;

import com.kapa.binance.base.constants.CodeConstant;
import com.kapa.binance.base.response.BaseErrorResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class Ex404 extends CustomException {
    public Ex404(String code, String message) {
        super(code, message);
    }

    public Ex404(String message) {
        super(CodeConstant.CODE_404, message);
    }

    public Ex404(@NotNull BaseErrorResponse error) {
        super(error);
    }

    public Ex404(String message, @NotEmpty List<BaseErrorResponse> error) {
        super(CodeConstant.CODE_404, message, error);
    }

    public Ex404(String message, Object error) {
        super(CodeConstant.CODE_404, message, error);
    }
}
