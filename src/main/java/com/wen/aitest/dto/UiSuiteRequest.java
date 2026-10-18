package com.wen.aitest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增/修改 UI 套件请求 */
@Data
public class UiSuiteRequest {

    @NotBlank(message = "套件名称不能为空")
    @Size(max = 100, message = "套件名称最长 100")
    private String suiteName;

    /** pytest 目标路径：相对 work-dir，如 testcases/test_smoke_ui.py */
    @NotBlank(message = "pytest 路径不能为空")
    private String pyPath;

    private String description;
}
