package com.wen.aitest.common;

import lombok.Data;

/**
 * 统一返回结构：平台所有接口都返回这个"信封"。
 *
 * 三层断言的呼应（阶段 2）：HTTP 状态码是外层（快递单），业务码 code 是里层
 * （包裹里的验收单）——前端永远先看 code 判断业务成败。
 *
 * @Data 是 Lombok 注解：编译期自动生成 getter/setter/toString 等模板方法。
 * 关键：JSON 序列化靠 getter——没有 getter，Spring 就取不到字段值，
 * 返回给前端的 JSON 会变成空的 {}。
 *
 * @param <T> 泛型：data 里装什么类型都行（字符串、对象、列表）
 */
@Data
public class Result<T> {

    /** 业务状态码：200=成功，其他=各类失败（与 HTTP 状态码是两回事） */
    private int code;

    /** 提示信息："操作成功" / "用户名不存在" 等，直接给前端展示用 */
    private String msg;

    /** 真正的数据载体 */
    private T data;

    /** 成功（带数据）——静态工厂方法：比裸 new 更好读 */
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }

    /** 成功（不带数据，如删除操作） */
    public static Result<Void> ok() {
        return ok(null);
    }

    /** 失败（自定义业务码 + 提示） */
    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
