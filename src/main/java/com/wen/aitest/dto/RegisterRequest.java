package com.wen.aitest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求的"信封"：前端 JSON → 这个对象。
 *
 * 校验注解 = 入口安检：不满足条件的请求直接被拦下返回 400，
 * 业务代码（service）一行都不会执行——脏数据进不了核心逻辑。
 *
 * 包名是 jakarta.*：Spring Boot 3 起从 javax.* 改名（老教程常踩的坑）。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3~20")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度 6~64")
    private String password;

    /** 可选：不填就用用户名当昵称（service 里处理） */
    @Size(max = 20, message = "昵称最长 20")
    private String nickname;
}
