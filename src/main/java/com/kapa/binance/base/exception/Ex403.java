package com.kapa.binance.base.exception;

import com.kapa.binance.base.constants.CodeConstant;
import com.kapa.binance.base.constants.MessageConstant;

import java.util.List;

public class Ex403 extends CustomException {

    public Ex403() {
        super(CodeConstant.CODE_403, MessageConstant.FORBIDDEN);
    }

    public Ex403(List<String> detail) {
        super(CodeConstant.CODE_403, MessageConstant.FORBIDDEN, detail);
    }

    public Ex403(String message) {
        super(CodeConstant.CODE_403, message);
    }
}
