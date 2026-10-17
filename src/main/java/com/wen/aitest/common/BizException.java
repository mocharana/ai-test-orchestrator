package com.wen.aitest.common;

/**
 * 业务异常：业务规则不满足时抛出，由 GlobalExceptionHandler 统一转为失败信封。
 * 继承 RuntimeException（非受检）：方法签名免 throws，配合全局处理器统一兜底。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
