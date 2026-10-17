package com.wen.aitest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增/修改用例的信封（两接口共用）。
 * headers/params/body/asserts 由前端传 JSON 字符串；
 * JSON 内部结构的合法性由 M5 校验器与 M3 执行器把关（此处只查核心字段非空）。
 */
@Data
public class ApiCaseRequest {

    @NotBlank(message = "用例名称不能为空")
    @Size(max = 100, message = "用例名称最长 100")
    private String caseName;

    /** 可选：不填 service 里兜底为"默认" */
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
