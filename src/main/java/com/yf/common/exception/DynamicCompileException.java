package com.yf.common.exception;

/**
     * 编译异常：封装编译过程中的错误信息
     */
public class DynamicCompileException extends Exception {
    public DynamicCompileException(String message) {
            super(message);
        }
}
