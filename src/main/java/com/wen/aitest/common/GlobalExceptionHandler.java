package com.wen.aitest.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：所有 Controller 里抛出的异常，都统一到这一处拦截，
 * 转成规整的 Result JSON 返回——前端永远看到统一信封，绝不看到报错页面。
 *
 * 调用侧真实顺序（以 /boom 为例）：
 * Controller 抛出 new BizException(...)
 *   → DispatcherServlet 捕获异常
 *   → 找到 @RestControllerAdvice 标注的本类
 *   → 按"谁的 @ExceptionHandler 匹配异常类型"路由到对应方法
 *   → 方法返回 Result.fail(...) → JSON 给前端
 *
 * @Slf4j 是 Lombok 注解：自动生成日志对象 log（下一行就在用）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 拦截业务异常：按异常自带的业务码返回，提示原样给前端看 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常：code={}，msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 兜底：拦截其他一切异常（空指针、数据库挂了……） */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        // 原始错误详情进日志（给开发者排查用），不返回给前端
        log.error("系统异常", e);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }
}
