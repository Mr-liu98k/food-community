package com.foodcommunity.common;

import lombok.Getter;

/**
 * 业务异常：GlobalExceptionHandler 会统一捕获并转成 Result。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = Result.CODE_ERROR;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
