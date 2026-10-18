package com.wen.aitest.dto;

import java.util.List;

/**
 * AI 生成结果报告：总数 / 入库数 / 入库用例名 / 被拦截清单及原因。
 * "AI 说的话不直接信"的落点——每一次拦截都向调用方交底。
 */
public record AiGenerateResult(
        int total,
        int accepted,
        List<String> savedNames,
        List<String> rejected
) {
}
