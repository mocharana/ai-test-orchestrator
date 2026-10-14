package com.wen.aitest.interceptor;

import com.wen.aitest.common.Result;
import com.wen.aitest.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * JWT 拦截器：所有受保护接口的"门卫"。
 *
 * 三个请求到达前 / 后的钩子，我们只重写 preHandle（到达 Controller 之前）：
 *   return true  → 放行，请求继续去往 Controller
 *   return false → 拦下，Controller 根本不会执行
 *
 * ⚠️ 关键认知（经典坑）：全局异常处理器 @RestControllerAdvice
 *    管不到拦截器！它只拦 Controller 内部抛的异常。
 *    所以 preHandle 里要拒绝请求时，必须自己往 response 手写 JSON。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;   // Jackson 的 JSON 转换器，Spring Boot 自动配好了 Bean

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 取票：从请求头拿 "Bearer eyJ..."（检验题：这就是"出示门票"的具体动作）
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeJson(response, Result.fail(401, "未登录，请先登录"));
            return false;
        }
        String token = auth.substring(7);   // 去掉 "Bearer " 前缀（7 个字符）

        // 2. 验票：签名+过期一体（parseUsername 内部不合法就抛异常）
        try {
            String username = jwtUtil.parseUsername(token);
            // 3. 放行：把"你是谁"挂到 request 上，Controller 里随时可取（/me 接口演示）
            request.setAttribute("currentUser", username);
            return true;
        } catch (Exception e) {
            // 过期 / 签名不符 / 格式错，统一提示（不区分细节，不给黑客试探空间）
            writeJson(response, Result.fail(401, "登录已过期，请重新登录"));
            return false;
        }
    }

    /** 拦截器层手写 JSON：HTTP 状态码保持 200，业务成败看 code（统一信封原则）。
     *  注意 ObjectMapper 是 Jackson 3 的（tools.jackson 包）——SB4 的自动配置 Bean，
     *  不是老教程里 com.fasterxml 的 Jackson 2 版本（本项目第一个真实的 SB4 版本坑） */
    private void writeJson(HttpServletResponse response, Result<?> result) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
