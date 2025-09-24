package com.kapa.binance.base.exception;

import com.kapa.binance.base.constants.CodeConstant;

public class Ex401 extends CustomException {

    public Ex401(String message) {
        super(CodeConstant.CODE_401, message);
    }
}
