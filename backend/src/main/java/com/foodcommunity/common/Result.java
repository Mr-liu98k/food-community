package com.foodcommunity.common;

import lombok.Data;

/**
 * 统一响应体：{ code, msg, data }
 * code=200 成功；400 参数错误；401 未登录/凭证失效；403 无权限；500 业务或系统错误。
 */
@Data
public class Result<T> {

    public static final int CODE_OK = 200;
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_ERROR = 500;

    private Integer code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = CODE_OK;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }

    public static Result<Void> ok() {
        return ok(null);
    }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> Result<T> error(String msg) {
        return error(CODE_ERROR, msg);
    }
}
