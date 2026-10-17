package com.wen.aitest.service.engine;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 断言引擎：阶段 2 response_checker.py 三层断言的 Java 复刻。
 *
 * 支持两类断言：
 *   http_status —— HTTP 状态码比对
 *   json_path   —— 按点号路径从响应 JSON 取值比对（数字段 = 数组下标，如 rows.0.noticeTitle）
 *
 * 为什么不引第三方 JSONPath 库？需求只有"点号+下标"两层语义，
 * 自研 20 行足够；等真需要复杂路径表达式时再引 jayway json-path——依赖权衡。
 *
 * static 纯逻辑（无状态、不读配置）——与 VariableResolver 同款判断。
 */
public class AssertEngine {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private AssertEngine() {
    }

    /** 用例的 asserts 字符串 → 规则列表 */
    public static List<AssertRule> parseRules(String assertsJson) {
        if (assertsJson == null || assertsJson.isBlank()) {
            return List.of();
        }
        List<AssertRule> rules = new ArrayList<>();
        JsonNode arr = MAPPER.readTree(assertsJson);
        arr.forEach(n -> rules.add(new AssertRule(
                n.get("type").asString(),
                n.has("path") ? n.get("path").asString() : null,
                n.has("expect") ? n.get("expect").asString() : null)));
        return rules;
    }

    /**
     * 执行全部断言。
     *
     * @param actualStatus 执行器拿到的 HTTP 状态码（0 = 网络级失败）
     * @param responseBody 响应体原文（可能是 JSON / HTML / 纯文本）
     * @param rules        断言规则列表
     */
    public static List<AssertResult> check(int actualStatus, String responseBody, List<AssertRule> rules) {
        // 响应体是否为合法 JSON——提前探测一次，json_path 断言据此分流
        JsonNode root = null;
        boolean bodyIsJson = false;
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                root = MAPPER.readTree(responseBody);
                bodyIsJson = root.isObject() || root.isArray();
            } catch (Exception ignored) {
                // 非 JSON（HTML/纯文本/网络失败文本）——不是错误，是测试结果的一部分
            }
        }

        List<AssertResult> results = new ArrayList<>();
        for (AssertRule r : rules) {
            results.add(checkOne(r, actualStatus, root, bodyIsJson));
        }
        return results;
    }

    private static AssertResult checkOne(AssertRule r, int actualStatus, JsonNode root, boolean bodyIsJson) {
        // 箭头 switch（Java 14+）：不用 break、编译器检查覆盖完整
        return switch (r.type()) {
            case "http_status" -> {
                String actual = String.valueOf(actualStatus);
                boolean pass = actual.equals(r.expect());
                yield new AssertResult(pass, r.type(), null, r.expect(), actual,
                        pass ? "状态码匹配" : "期望状态码 " + r.expect() + "，实际 " + actual);
            }
            case "json_path" -> {
                if (!bodyIsJson) {
                    yield new AssertResult(false, r.type(), r.path(), r.expect(),
                            "(响应体不是合法 JSON)", "响应体不是 JSON，无法按路径取值");
                }
                String actual = pick(root, r.path());
                if (actual == null) {
                    yield new AssertResult(false, r.type(), r.path(), r.expect(),
                            "(路径不存在)", "路径 " + r.path() + " 在响应中不存在");
                }
                boolean pass = actual.equals(r.expect());
                yield new AssertResult(pass, r.type(), r.path(), r.expect(), actual,
                        pass ? "值匹配" : "路径 " + r.path() + " 期望 " + r.expect() + "，实际 " + actual);
            }
            default -> new AssertResult(false, r.type(), r.path(), r.expect(),
                    "(未知断言类型)", "不支持的断言类型：" + r.type());
        };
    }

    /** 按 a.b.0.c 取值：点号分段，纯数字段 = 数组下标；任一段缺失 → null */
    private static String pick(JsonNode root, String path) {
        JsonNode cur = root;
        for (String seg : path.split("\\.")) {
            if (seg.matches("\\d+")) {
                cur = cur.get(Integer.parseInt(seg));   // 数组下标（越界返回 null）
            } else {
                cur = cur.path(seg);                     // 对象字段（缺失返回 MissingNode）
            }
            if (cur == null || cur.isMissingNode()) {
                return null;
            }
        }
        return cur.asString();
    }
}
