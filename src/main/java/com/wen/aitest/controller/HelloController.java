package com.wen.aitest.controller;

import com.wen.aitest.common.BizException;
import com.wen.aitest.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第一个接口：验证"浏览器请求 → Spring → 统一格式响应"全链路通。
 * M0-4 验收就是从 Swagger 里点它。
 */
@RestController  // = @Controller + @ResponseBody：本类所有方法的返回值直接转 JSON 给前端
public class HelloController {

    /**
     * 调用侧真实顺序：
     * 浏览器 GET localhost:8081/hello
     *   → 内嵌 Tomcat 收到请求
     *   → Spring 的"总调度台" DispatcherServlet 查路由表
     *   → 找到本方法（@GetMapping("/hello")）
     *   → 执行方法体，拿到 Result 对象
     *   → Jackson 把对象转成 JSON 返回
     */
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.ok("AI-Test-Orchestrator 已启动");
    }

    /**
     * 故意制造业务异常——M0-4 在 Swagger 里点它，验证全局异常处理器生效。
     */
    @GetMapping("/boom")
    public Result<Void> boom() {
        throw new BizException(1001, "这是故意制造的业务异常");
    }

    /**
     * 受保护接口示例（M1-4）：不在白名单 → 必须带票才能进。
     * currentUser 是 JwtInterceptor 验票通过后挂到 request 上的。
     */
    @GetMapping("/me")
    public Result<String> me(jakarta.servlet.http.HttpServletRequest request) {
        String currentUser = (String) request.getAttribute("currentUser");
        return Result.ok("当前登录用户：" + currentUser);
    }
}
