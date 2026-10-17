package com.wen.aitest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 注册请求。校验注解在入口拦截非法数据，service 不重复检查 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3~20")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度 6~64")
    private String password;

    /** 可选：不填默认用用户名 */
    @Size(max = 20, message = "昵称最长 20")
    private String nickname;
}
