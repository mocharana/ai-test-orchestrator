package com.wen.aitest.service.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 断言引擎单测：通过/失败/非 JSON 体/路径缺失等场景（嵌套用例复刻 RuoYi 真实结构） */
@DisplayName("断言引擎")
class AssertEngineTest {

    /** 组合工具：解析规则 → 执行断言（模拟执行服务的调用方式） */
    private List<AssertResult> run(String assertsJson, int status, String body) {
        return AssertEngine.check(status, body, AssertEngine.parseRules(assertsJson));
    }

    @Test
    @DisplayName("http_status：通过")
    void statusPass() {
        List<AssertResult> rs = run("[{\"type\":\"http_status\",\"expect\":200}]", 200, "{}");
        assertTrue(rs.get(0).passed());
    }

    @Test
    @DisplayName("http_status：失败（500 ≠ 200），actual 记录真实值")
    void statusFail() {
        List<AssertResult> rs = run("[{\"type\":\"http_status\",\"expect\":200}]", 500, "");
        assertFalse(rs.get(0).passed());
        assertEquals("500", rs.get(0).actual());
    }

    @Test
    @DisplayName("json_path：顶层字段通过")
    void jsonTopPass() {
        List<AssertResult> rs = run(
                "[{\"type\":\"json_path\",\"path\":\"code\",\"expect\":200}]",
                200, "{\"code\":200,\"msg\":\"操作成功\"}");
        assertTrue(rs.get(0).passed());
        assertEquals("200", rs.get(0).actual());
    }

    @Test
    @DisplayName("json_path：嵌套 + 数组下标（复刻 RuoYi 列表结构 rows.0.noticeTitle）")
    void jsonNestedPass() {
        String body = "{\"total\":1,\"rows\":[{\"noticeTitle\":\"公告1\"}]}";
        List<AssertResult> rs = run(
                "[{\"type\":\"json_path\",\"path\":\"rows.0.noticeTitle\",\"expect\":\"公告1\"}]",
                200, body);
        assertTrue(rs.get(0).passed());
        assertEquals("公告1", rs.get(0).actual());
    }

    @Test
    @DisplayName("json_path：值不等 → 失败")
    void jsonValueFail() {
        List<AssertResult> rs = run(
                "[{\"type\":\"json_path\",\"path\":\"code\",\"expect\":500}]",
                200, "{\"code\":200}");
        assertFalse(rs.get(0).passed());
    }

    @Test
    @DisplayName("响应体不是 JSON（如网络失败文本）→ json_path 判失败并注明")
    void bodyNotJson() {
        List<AssertResult> rs = run(
                "[{\"type\":\"json_path\",\"path\":\"code\",\"expect\":200}]",
                0, "网络异常：connect timeout");
        assertFalse(rs.get(0).passed());
        assertEquals("(响应体不是合法 JSON)", rs.get(0).actual());
    }

    @Test
    @DisplayName("路径不存在 → 失败并标注")
    void pathMissing() {
        List<AssertResult> rs = run(
                "[{\"type\":\"json_path\",\"path\":\"xxx.yyy\",\"expect\":1}]",
                200, "{\"code\":200}");
        assertFalse(rs.get(0).passed());
        assertEquals("(路径不存在)", rs.get(0).actual());
    }
}
