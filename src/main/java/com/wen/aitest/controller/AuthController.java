package com.wen.aitest.controller;

import com.wen.aitest.common.Result;
import com.wen.aitest.dto.LoginRequest;
import com.wen.aitest.dto.LoginResponse;
import com.wen.aitest.dto.RegisterRequest;
import com.wen.aitest.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证入口：注册 / 登录。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 注册（M1-2 已验收）。
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
        userService.register(req);
        return Result.ok();
    }

    /**
     * 登录（调用侧真实顺序）：
     * POST /auth/login {"username":"wen","password":"abc123"}
     *   → @Valid 安检 → login 四步（查人 → 比密文 → 查状态 → 发票）
     *   → 返回 {"code":200,"data":{"token":"eyJ...","username":"wen","nickname":"wen"}}
     * 之后访问受保护接口：请求头带 Authorization: Bearer eyJ...（M1-4 拦截器认它）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(userService.login(req));
    }
}
