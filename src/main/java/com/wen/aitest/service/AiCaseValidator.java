package com.wen.aitest.service;

import tools.jackson.databind.json.JsonMapper;
import com.wen.aitest.dto.ApiCaseRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * AI 产出用例校验器：AI 说的话不直接信，逐条机器校验，不过就拦截。
 * 三条规则：①必填字段齐全 ②method 合法 + URL 以 http 开头 ③asserts 是合法 JSON 数组。
 * 独立成类：可单独单测，规则一眼可读。
 */
@Component
public class AiCaseValidator {

    private static final Set<String> LEGAL_METHODS = Set.of("GET", "POST", "PUT", "DELETE");

    /** 断言类型白名单：与 AssertEngine 支持的能力对齐（规则④，AI 曾发明引擎没有的字段契约） */
    private static final Set<String> LEGAL_ASSERT_TYPES = Set.of("http_status", "json_path");

    /** Spring Boot 4 迁移到 Jackson 3：包名 com.fasterxml → tools.jackson，容器提供的是 JsonMapper bean */
    private final JsonMapper jsonMapper;

    public AiCaseValidator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /** 返回问题列表；空列表 = 通过 */
    public List<String> validate(ApiCaseRequest req) {
        List<String> problems = new ArrayList<>();

        if (!StringUtils.hasText(req.getCaseName())) {
            problems.add("caseName 缺失");
        }

        String method = req.getMethod() == null ? "" : req.getMethod().trim().toUpperCase();
        if (!LEGAL_METHODS.contains(method)) {
            problems.add("method 非法: " + req.getMethod());
        }

        if (!StringUtils.hasText(req.getUrl()) || !req.getUrl().trim().toLowerCase().startsWith("http")) {
            problems.add("url 缺失或不是 http 开头");
        }

        if (StringUtils.hasText(req.getAsserts())) {
            try {
                JsonNode arr = jsonMapper.readTree(req.getAsserts());
                if (!arr.isArray()) {
                    problems.add("asserts 不是 JSON 数组");
                } else {
                    // 规则④：断言字段契约校验——type 必须是引擎支持的类型，json_path 必须带 path
                    for (JsonNode n : arr) {
                        String type = n.path("type").asString("");
                        if (type.isEmpty()) {
                            problems.add("asserts 项缺少 type 字段");
                        } else if (!LEGAL_ASSERT_TYPES.contains(type)) {
                            problems.add("不支持断言类型: " + type);
                        } else if ("json_path".equals(type)
                                && !StringUtils.hasText(n.path("path").asString(""))) {
                            problems.add("json_path 断言缺少 path 字段（路径放 path，期望值放 expect）");
                        }
                    }
                }
            } catch (Exception e) {
                problems.add("asserts 不是合法 JSON 数组");
            }
        } else {
            problems.add("asserts 缺失");
        }

        // 规则⑤：headers/params/body 形态契约——headers 数组 [{key,value}]、params 对象、body 合法 JSON
        if (StringUtils.hasText(req.getHeaders())) {
            try {
                JsonNode h = jsonMapper.readTree(req.getHeaders());
                if (!h.isArray()) {
                    problems.add("headers 必须是 [{\"key\":..,\"value\":..}] 数组形态");
                } else {
                    for (JsonNode n : h) {
                        if (n.path("key").asString("").isEmpty()) {
                            problems.add("headers 项缺少 key 字段");
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                problems.add("headers 不是合法 JSON");
            }
        }
        if (StringUtils.hasText(req.getParams())) {
            try {
                if (!jsonMapper.readTree(req.getParams()).isObject()) {
                    problems.add("params 必须是 JSON 对象形态");
                }
            } catch (Exception e) {
                problems.add("params 不是合法 JSON");
            }
        }
        if (StringUtils.hasText(req.getBody())) {
            try {
                jsonMapper.readTree(req.getBody());
            } catch (Exception e) {
                problems.add("body 不是合法 JSON");
            }
        }

        return problems;
    }
}
