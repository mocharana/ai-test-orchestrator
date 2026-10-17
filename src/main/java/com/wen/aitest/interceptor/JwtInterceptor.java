package com.wen.aitest.interceptor;

import com.wen.aitest.common.Result;
import com.wen.aitest.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JWT 拦截器：Controller 前验票，验票通过后把用户名挂到 request 供 Controller 读取。
 * 失败时需自行写 JSON 响应（@RestControllerAdvice 不覆盖拦截器层）。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeJson(response, Result.fail(401, "未登录，请先登录"));
            return false;
        }
        String token = auth.substring(7);

        try {
            String username = jwtUtil.parseUsername(token);
            request.setAttribute("currentUser", username);
            return true;
        } catch (Exception e) {
            writeJson(response, Result.fail(401, "登录已过期，请重新登录"));
            return false;
        }
    }

    private void writeJson(HttpServletResponse response, Result<?> result) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
