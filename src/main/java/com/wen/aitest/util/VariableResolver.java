package com.wen.aitest.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量替换器：把 {{key}} 占位符按变量表替换。
 *
 * 为什么是 static 工具类而不是 Bean？
 *   它无状态、不读配置、不依赖其他 Bean——纯函数；
 *   对比 JwtUtil（要读 yml 密钥，有状态）才需要做 Bean。
 *   "有状态才注入，纯函数直接 static"是工具类的设计判断。
 *
 * 为什么未定义的占位符保留原样而不是抛异常？
 *   M3 执行时，日志里看到 {{xxx}} 残留 = 一眼定位"这个变量没进变量表"；
 *   若抛异常/替换成空，问题就被藏起来了——保留现场是排障友好设计。
 */
public class VariableResolver {

    /** 匹配 {{key}}：\{\{ 是字面大括号（{ 在正则里要转义），(\w+) 捕获组抓 key */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private VariableResolver() {
        // 纯工具类：构造器私有，禁止 new
    }

    /**
     * @param template  含 {{key}} 占位的字符串（用例的 headers/params/body/asserts 原文）
     * @param variables 变量表（如 {"token": "eyJ..."}）
     * @return 替换后的字符串；null 输入返回 null；未定义的占位保留原样
     */
    public static String resolve(String template, Map<String, String> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            // group(0) = 整个匹配 {{token}}；group(1) = 捕获组 token
            String replacement = variables.getOrDefault(matcher.group(1), matcher.group(0));
            // quoteReplacement：变量值里可能含 $ 或 \，不转义会被当成正则替换语法误读
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
