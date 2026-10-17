package com.wen.aitest.common;

import lombok.Data;

/**
 * 统一返回结构：所有接口返回 {code, msg, data}。
 * HTTP 状态码恒为 200，业务成败看 code —— 两层分离便于前端统一处理。
 */
@Data
public class Result<T> {

    /** 业务状态码：200=成功，其他=各类失败 */
    private int code;

    /** 提示信息，直接给前端展示 */
    private String msg;

    /** 数据载体 */
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }

    public static Result<Void> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
