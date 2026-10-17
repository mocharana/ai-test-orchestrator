package com.wen.aitest.controller;

import com.wen.aitest.common.BizException;
import com.wen.aitest.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试用接口：/hello 验活、/boom 验证全局异常、/me 验证拦截器（需登录）。
 */
@RestController
public class HelloController {

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.ok("AI-Test-Orchestrator 已启动");
    }

    /** 故意抛业务异常，验证全局异常处理器 */
    @GetMapping("/boom")
    public Result<Void> boom() {
        throw new BizException(1001, "这是故意制造的业务异常");
    }

    /** 受保护接口示例：currentUser 由拦截器验票后写入 */
    @GetMapping("/me")
    public Result<String> me(jakarta.servlet.http.HttpServletRequest request) {
        String currentUser = (String) request.getAttribute("currentUser");
        return Result.ok("当前登录用户：" + currentUser);
    }
}
