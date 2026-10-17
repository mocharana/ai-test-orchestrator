package com.wen.aitest.service.engine;

/** 单条断言规则（来自用例 asserts 字段的 JSON） */
public record AssertRule(String type, String path, String expect) {
}
