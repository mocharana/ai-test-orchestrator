package com.wen.aitest.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {{key}} 占位符替换器（static 纯函数工具类，无状态）。
 * 未定义的占位符保留原样——执行日志中看到 {{xxx}} 残留即知该变量未配置。
 */
public class VariableResolver {

    /** 匹配 {{key}}：\{\{ 转义字面大括号，(\w+) 捕获组抓 key */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private VariableResolver() {
    }

    /**
     * @param template  含 {{key}} 占位的字符串
     * @param variables 变量表
     * @return 替换后的字符串；null 入参返回 null；未定义占位保留原样
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
            String replacement = variables.getOrDefault(matcher.group(1), matcher.group(0));
            // quoteReplacement：值中含 $ 或 \ 时避免被当作替换语法
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
