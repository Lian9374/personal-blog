package com.personalblog.common.exception;

import lombok.Getter;

/**
 * 业务异常, 携带 HTTP 状态码供全局异常处理器渲染错误页
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
