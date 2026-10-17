package com.wen.aitest.service.engine;

/**
 * 单条断言规则——从用例 asserts 字段的 JSON 解析而来：
 * [{"type":"http_status","expect":200},
 *  {"type":"json_path","path":"rows.0.noticeTitle","expect":"公告1"}]
 *
 * type 支持：http_status（状态码比对）/ json_path（点号+数字下标取值比对）。
 */
public record AssertRule(String type, String path, String expect) {
}
