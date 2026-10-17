package com.wen.aitest.service.engine;

/**
 * 单条断言的执行结果——报告页展示的最小单位。
 * passed / expect / actual 三件套 = 报告里"期望 X，实际 Y"的数据来源。
 */
public record AssertResult(boolean passed, String type, String path,
                           String expect, String actual, String message) {
}
