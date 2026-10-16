package com.campus.trading.common;

import lombok.Data;

/**
 * 统一响应结果封装
 */
@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;

    public static final int OK = 200;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(OK, "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(OK, "success", data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
