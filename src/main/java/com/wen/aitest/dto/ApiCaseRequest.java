package com.wen.aitest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增/修改用例请求（两接口共用）。
 * headers/params/body/asserts 为 JSON 字符串，结构合法性由执行引擎与 AI 校验器把关。
 */
@Data
public class ApiCaseRequest {

    @NotBlank(message = "用例名称不能为空")
    @Size(max = 100, message = "用例名称最长 100")
    private String caseName;

    /** 可选：不填默认"默认" */
    private String module;

    /** 可选：0=P0 1=P1 2=P2，不填默认 2 */
    private Integer priority;

    @NotBlank(message = "请求方法不能为空")
    private String method;

    @NotBlank(message = "请求地址不能为空")
    @Size(max = 500, message = "URL 最长 500")
    private String url;

    private String headers;

    private String params;

    private String body;

    @NotBlank(message = "断言规则不能为空")
    private String asserts;

    private String remark;
}
