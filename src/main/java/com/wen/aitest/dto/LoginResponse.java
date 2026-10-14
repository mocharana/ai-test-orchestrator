package com.wen.aitest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功的返回信封：门票 + 用户展示信息。
 *
 * @AllArgsConstructor = 生成全参构造器（service 里 new 它时用）。
 * 请求/响应各一个 dto 成对出现，是接口层的标准姿势。
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    /** JWT 门票：之后每次请求放进请求头出示 */
    private String token;

    private String username;

    private String nickname;
}
