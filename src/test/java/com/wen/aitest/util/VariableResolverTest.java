package com.wen.aitest.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 变量替换器单测：覆盖正常流/边界/特殊场景（等价类+边界值） */
@DisplayName("变量替换器")
class VariableResolverTest {

    @Test
    @DisplayName("替换单个变量")
    void replaceSingle() {
        String r = VariableResolver.resolve("Bearer {{token}}",
                Map.of("token", "eyJ.abc"));
        assertEquals("Bearer eyJ.abc", r);
    }

    @Test
    @DisplayName("多个变量 + 同一占位出现两次也替换")
    void replaceMultipleAndRepeated() {
        String r = VariableResolver.resolve("{{a}}-{{b}}-{{a}}",
                Map.of("a", "1", "b", "2"));
        assertEquals("1-2-1", r);
    }

    @Test
    @DisplayName("无占位符：原样返回")
    void noPlaceholder() {
        String r = VariableResolver.resolve("http://localhost:8080/login",
                Map.of("token", "x"));
        assertEquals("http://localhost:8080/login", r);
    }

    @Test
    @DisplayName("未定义变量：保留 {{key}} 原样")
    void undefinedKeptAsIs() {
        String r = VariableResolver.resolve("Bearer {{token}}-{{unknown}}",
                Map.of("token", "t1"));
        assertEquals("Bearer t1-{{unknown}}", r);
    }

    @Test
    @DisplayName("空变量表：原样返回")
    void emptyVariables() {
        String r = VariableResolver.resolve("Bearer {{token}}", Map.of());
        assertEquals("Bearer {{token}}", r);
    }

    @Test
    @DisplayName("null 模板：返回 null")
    void nullTemplate() {
        assertNull(VariableResolver.resolve(null, Map.of("token", "x")));
    }
}
