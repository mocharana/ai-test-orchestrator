package com.wen.aitest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 登录响应：token + 用户展示信息 */
@Data
@AllArgsConstructor
public class LoginResponse {

    /** JWT，后续请求以 Authorization: Bearer 携带 */
    private String token;

    private String username;

    private String nickname;
}
