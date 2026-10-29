package com.wen.aitest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wen.aitest.common.BizException;
import com.wen.aitest.entity.AiLog;
import com.wen.aitest.entity.ApiCase;
import com.wen.aitest.entity.ApiResult;
import com.wen.aitest.mapper.AiLogMapper;
import com.wen.aitest.mapper.ApiCaseMapper;
import com.wen.aitest.mapper.ApiResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 失败归因（M5-3 功能②）：把单条失败明细的证据三件套
 * （用例数据 / 实际响应 / 断言差异）喂给 AI，输出原因分析与修复建议。
 * 只喂事实不喂猜测——语义层判断交给 AI，但必须基于 evidence。
 */
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private static final String SYSTEM_PROMPT = """
            你是资深测试开发专家，负责分析接口测试用例失败的原因。

            你会收到一份 JSON 证据包：用例数据（caseName/method/url/headers/params/body/asserts）、
            实际执行结果（httpStatus/responseBody）、断言差异摘要。

            任务：
            1.【原因分析】判断失败属于哪类并给出依据：
               A. 用例期望值错误（对被测系统行为的假设不对）
               B. 被测系统缺陷（行为明显不合理）
               C. 环境/数据问题（连不上、数据被删等）
            2.【修复建议】给出具体可执行的修改：改用例哪个字段、改成什么；或建议排查什么。

            铁律：只基于证据分析，证据里没有的不要编造；输出固定两段【原因分析】和【修复建议】。
            """;

    private final AiChatService aiChatService;
    private final ApiResultMapper apiResultMapper;
    private final ApiCaseMapper apiCaseMapper;
    private final AiLogMapper aiLogMapper;
    private final JsonMapper jsonMapper;

    /**
     * 对单条失败明细做 AI 归因。
     * 先查 t_ai_log 历史归因——同一明细已分析过就直接返回，不再烧一次模型调用；
     * 未命中才走模型，并把 bizId=resultId 落库，供刷新后回看。
     */
    public String analyze(Long resultId, String currentUser) {
        // ① 取明细行：只对失败明细归因
        ApiResult row = apiResultMapper.selectById(resultId);
        if (row == null) {
            throw new BizException(5004, "执行明细不存在（本接口只支持接口用例明细）");
        }
        if (row.getPassed() != null && row.getPassed() == 1) {
            throw new BizException(5004, "该明细是通过的，无需归因");
        }

        // ② 命中历史归因则直接返回（省一次 15s 调用 + token）
        String cached = findCached(resultId);
        if (cached != null) {
            return cached;
        }

        // ③ 取用例数据（可能已被删除——用明细行冗余的 caseName 兜底）
        ApiCase c = row.getCaseId() == null ? null : apiCaseMapper.selectById(row.getCaseId());

        // ④ 证据三件套打包：只给事实，不做任何推断
        Evidence ev = new Evidence(
                c == null ? row.getCaseName() : c.getCaseName(),
                c == null ? null : c.getMethod(),
                c == null ? null : c.getUrl(),
                c == null ? null : c.getHeaders(),
                c == null ? null : c.getParams(),
                c == null ? null : c.getBody(),
                c == null ? null : c.getAsserts(),
                row.getHttpStatus(),
                row.getResponseBody(),
                row.getAssertSummary());

        String user = "证据包：\n" + jsonMapper.writeValueAsString(ev);

        // ⑤ 走统一通道：bizType=ANALYZE + bizId=resultId，自动留痕且可回查
        return aiChatService.call("ANALYZE", resultId, SYSTEM_PROMPT, user, currentUser, 0.3).reply();
    }

    /** 该明细最近一次成功的归因文本；没有则返回 null */
    private String findCached(Long resultId) {
        AiLog hit = aiLogMapper.selectOne(new LambdaQueryWrapper<AiLog>()
                .eq(AiLog::getBizType, "ANALYZE")
                .eq(AiLog::getBizId, resultId)
                .eq(AiLog::getSuccess, 1)
                .orderByDesc(AiLog::getId)
                .last("LIMIT 1"));
        return hit == null ? null : hit.getResponse();
    }

    /**
     * 某批次下所有已归因明细的历史结果：{ resultId: 归因文本 }。
     * 前端打开明细弹窗时一次性拉取，已分析过的行直接可看，不用再点、不用再调模型。
     */
    public Map<Long, String> listByExecution(Long executionId) {
        List<ApiResult> rows = apiResultMapper.selectList(new LambdaQueryWrapper<ApiResult>()
                .eq(ApiResult::getExecutionId, executionId));
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = rows.stream().map(ApiResult::getId).toList();
        List<AiLog> logs = aiLogMapper.selectList(new LambdaQueryWrapper<AiLog>()
                .eq(AiLog::getBizType, "ANALYZE")
                .in(AiLog::getBizId, ids)
                .eq(AiLog::getSuccess, 1)
                .orderByAsc(AiLog::getId));
        Map<Long, String> result = new LinkedHashMap<>();
        // 按 id 升序遍历，后写入的覆盖先写入的 = 每个明细取最新一次
        for (AiLog l : logs) {
            result.put(l.getBizId(), l.getResponse());
        }
        return result;
    }

    /** 证据三件套（record 直接序列化成 JSON 喂给 AI） */
    public record Evidence(String caseName, String method, String url,
                           String headers, String params, String body, String asserts,
                           Integer httpStatus, String responseBody, String assertSummary) {
    }
}
