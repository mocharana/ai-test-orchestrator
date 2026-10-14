package com.wen.aitest.common;

/**
 * 业务异常：业务规则不满足时主动抛出（如"用户名不存在"、"用例不存在"）。
 *
 * 与系统异常（空指针、连不上数据库）区分开——它只携带业务码 + 给人看的提示。
 *
 * 为什么继承 RuntimeException 而不是 Exception？
 * RuntimeException 是"非受检异常"：方法签名上不用写 throws，
 * 调用侧也不用层层 try-catch，代码干净——配合全局异常处理器统一兜底。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String msg) {
        super(msg);   // 把提示交给父类的 message 字段
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
