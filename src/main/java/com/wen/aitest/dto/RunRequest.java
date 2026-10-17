package com.wen.aitest.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 执行请求的信封：要跑哪些用例 */
@Data
public class RunRequest {

    @NotEmpty(message = "用例 ID 列表不能为空")
    private List<Long> caseIds;
}
