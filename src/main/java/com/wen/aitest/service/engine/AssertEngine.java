package com.wen.aitest.service.engine;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 断言引擎：http_status 状态码比对 / json_path 路径取值比对。
 * 取值路径格式 a.b.0.c（点号分段，数字段为数组下标）——自研实现，避免引入 jayway 依赖。
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

    /** 执行全部断言。响应体非 JSON 时 json_path 断言判失败并注明原因 */
    public static List<AssertResult> check(int actualStatus, String responseBody, List<AssertRule> rules) {
        JsonNode root = null;
        boolean bodyIsJson = false;
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                root = MAPPER.readTree(responseBody);
                bodyIsJson = root.isObject() || root.isArray();
            } catch (Exception ignored) {
                // 非 JSON 响应体（HTML/纯文本）不是错误，是测试结果的一部分
            }
        }

        List<AssertResult> results = new ArrayList<>();
        for (AssertRule r : rules) {
            results.add(checkOne(r, actualStatus, root, bodyIsJson));
        }
        return results;
    }

    private static AssertResult checkOne(AssertRule r, int actualStatus, JsonNode root, boolean bodyIsJson) {
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

    /** 按点号路径逐段取值，任一段缺失返回 null */
    private static String pick(JsonNode root, String path) {
        JsonNode cur = root;
        for (String seg : path.split("\\.")) {
            if (seg.matches("\\d+")) {
                cur = cur.get(Integer.parseInt(seg));
            } else {
                cur = cur.path(seg);
            }
            if (cur == null || cur.isMissingNode()) {
                return null;
            }
        }
        return cur.asString();
    }
}
