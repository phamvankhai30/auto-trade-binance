package com.kapa.binance.base.exception;

import com.kapa.binance.base.constants.CodeConstant;
import com.kapa.binance.base.constants.MessageConstant;

public class Ex500 extends CustomException {

    public Ex500(String messageDetail) {
        super(CodeConstant.CODE_500, MessageConstant.INTERNAL_SERVER_ERROR, messageDetail);
    }

    public Ex500(String code, String message, String messageDetail) {
        super(code, message, messageDetail);
    }
}
