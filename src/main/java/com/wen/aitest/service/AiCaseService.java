package com.wen.aitest.service;

import tools.jackson.databind.json.JsonMapper;
import com.wen.aitest.common.BizException;
import com.wen.aitest.dto.AiGenerateResult;
import com.wen.aitest.dto.ApiCaseRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 生成用例：Prompt 模板 + 输出清洗 + 逐条机器校验 + 入库。
 * 与 M2 的 ApiCaseService 分工：这里只管"AI 产出→过滤"，落库复用 create()，createBy 记 "AI"。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCaseService {

    /**
     * 系统提示词：角色 + 字段契约 + 铁律。文本块 """ 是 Java 15+ 语法。
     * 为什么 system 和 user 分开？system 定"你是谁+游戏规则"（可复用），
     * user 装"这次的具体输入"（每次变）——职责分离，换需求只换 user 段。
     */
    private static final String SYSTEM_PROMPT = """
            你是资深接口测试设计专家。根据用户给出的功能需求描述，设计接口测试用例。

            铁律：
            1. 只输出一个 JSON 数组，禁止任何解释文字或 markdown 代码块标记。
            2. 数组每个元素字段固定：
               caseName(用例名称), module(模块名), priority(0/1/2 整数, 0 最高),
               method(只能 GET/POST/PUT/DELETE 大写), url(完整地址, http 开头),
               headers(JSON数组字符串, 每项形如 {"key":"Content-Type","value":"application/json"}，可为""), params(JSON对象字符串, 如 {"pageNum":1}，可为""),
               body(JSON字符串, 可为""), asserts(JSON数组字符串, 每项形如 {"type":"http_status","expect":200} 或 {"type":"json_path","path":"$.code","expect":"400"}，json_path 的路径必须放 path 字段、期望值放 expect),
               remark(一句话说明这条用例测什么)
            3. 正常场景 + 异常场景都要覆盖，共 2~5 条。
            4. url 用需求描述里出现的地址；没给就填 http://待补充。
            """;

    private final AiChatService aiChatService;
    private final AiCaseValidator validator;
    private final ApiCaseService apiCaseService;
    private final JsonMapper jsonMapper;

    public AiGenerateResult generate(String requirement, String currentUser) {
        // 1. 调 AI（统一通道 AiChatService：自动留痕 t_ai_log；温度 0.3 求稳；关深度思考）
        String reply = aiChatService
                .call("GENERATE", SYSTEM_PROMPT, requirement, currentUser, 0.3)
                .reply();

        // 2. 清洗输出：截取首个 [ 到最后一个 ] 之间的内容（防 AI 嘴碎带围栏/解释）
        String json = extractJsonArray(reply);

        // 3. 整体解析：数组本身不合法 = 全军覆没，直接报业务异常
        ApiCaseRequest[] cases;
        try {
            cases = jsonMapper.readValue(json, ApiCaseRequest[].class);
        } catch (Exception e) {
            log.warn("AI 返回 JSON 解析失败: {}", reply);
            throw new BizException(5001, "AI 返回内容无法解析为用例数组，已丢弃（原文见日志）");
        }

        // 4. 逐条机器校验：通过的入库(createBy=AI)，不过的拦下并记原因
        List<String> savedNames = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        for (ApiCaseRequest c : cases) {
            List<String> problems = validator.validate(c);
            if (problems.isEmpty()) {
                apiCaseService.create(c, "AI");
                savedNames.add(c.getCaseName());
            } else {
                rejected.add("[" + c.getCaseName() + "] " + String.join("；", problems));
            }
        }
        return new AiGenerateResult(cases.length, savedNames.size(), savedNames, rejected);
    }

    /** 剥 markdown 围栏 + 截取 JSON 数组主体 */
    private String extractJsonArray(String reply) {
        int start = reply.indexOf('[');
        int end = reply.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw new BizException(5001, "AI 返回中未找到 JSON 数组，已丢弃");
        }
        return reply.substring(start, end + 1);
    }
}
