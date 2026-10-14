package com.wen.aitest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求的信封：前端 JSON → 对象（@Valid 安检只查"非空"，格式对错交给登录逻辑）。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
