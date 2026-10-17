package com.wen.aitest.service.engine;

/** 单条断言执行结果：passed/expect/actual 是报告页"期望 X，实际 Y"的数据来源 */
public record AssertResult(boolean passed, String type, String path,
                           String expect, String actual, String message) {
}
