package com.wen.aitest.service;

import com.wen.aitest.entity.ApiCase;
import com.wen.aitest.entity.ApiResult;
import com.wen.aitest.entity.Execution;
import com.wen.aitest.mapper.ApiCaseMapper;
import com.wen.aitest.mapper.ApiResultMapper;
import com.wen.aitest.mapper.ExecutionMapper;
import com.wen.aitest.service.engine.AssertEngine;
import com.wen.aitest.service.engine.AssertResult;
import com.wen.aitest.service.engine.HttpExecutor;
import com.wen.aitest.service.engine.HttpResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 异步执行体：登录被测系统取 token → 逐条执行（替换变量/发请求/断言）→ 结果落库 → 更新批次。
 * 执行明细有意不加事务：渐进可见（跑到哪存到哪），与批量导入的原子性要求相反。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionRunner {

    private final ApiCaseMapper apiCaseMapper;
    private final ApiResultMapper apiResultMapper;
    private final ExecutionMapper executionMapper;
    private final HttpExecutor httpExecutor;

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Value("${ruoyi.base-url}")
    private String ruoyiBaseUrl;
    @Value("${ruoyi.username}")
    private String ruoyiUsername;
    @Value("${ruoyi.password}")
    private String ruoyiPassword;

    @Async("executionExecutor")
    public void runAsync(Long executionId, List<Long> caseIds, String currentUser) {
        // 兜底：任何意外都不能让批次卡在 RUNNING
        try {
            doRun(executionId, caseIds, currentUser);
        } catch (Exception e) {
            log.error("批次 {} 执行异常", executionId, e);
            finishBatch(executionId, 0, caseIds.size());
        }
    }

    private void doRun(Long executionId, List<Long> caseIds, String currentUser) {
        // ① 准备变量表：登录被测系统拿 token（{{token}} 的取值来源）
        Map<String, String> variables = new HashMap<>();
        String token = loginRuoYi();
        if (token == null) {
            // 登录失败也继续跑：用例收到 401 自然失败，如实记录
            log.warn("批次 {}：RuoYi 登录失败，用例将以未认证状态执行", executionId);
        } else {
            variables.put("token", token);
        }

        // ② 逐条执行：发请求 → 断言 → 落库
        int passed = 0, failed = 0;
        for (Long caseId : caseIds) {
            ApiCase c = apiCaseMapper.selectById(caseId);
            long t0 = System.currentTimeMillis();
            ApiResult row = new ApiResult();
            row.setExecutionId(executionId);
            row.setCaseId(caseId);

            if (c == null) {
                row.setCaseName("用例#" + caseId);
                row.setPassed(0);
                row.setHttpStatus(0);
                row.setAssertSummary("用例不存在（可能已删除）");
                row.setCostMs(0L);
                failed++;
            } else {
                row.setCaseName(c.getCaseName());
                HttpResult hr = httpExecutor.execute(c, variables);
                List<AssertResult> ars = AssertEngine.check(
                        hr.status(), hr.body(), AssertEngine.parseRules(c.getAsserts()));
                boolean allPass = ars.stream().allMatch(AssertResult::passed);

                row.setPassed(allPass ? 1 : 0);
                row.setHttpStatus(hr.status());
                row.setAssertSummary(buildSummary(ars));
                row.setResponseBody(truncate(hr.body(), 5000));
                row.setCostMs(System.currentTimeMillis() - t0);
                if (allPass) passed++; else failed++;
            }
            apiResultMapper.insert(row);
        }

        // ③ 收尾批次
        finishBatch(executionId, passed, failed);
        log.info("批次 {} 执行完成：{} 通过 / {} 失败", executionId, passed, failed);
    }

    /** 登录被测系统拿 token：构造临时用例对象复用 HttpExecutor */
    private String loginRuoYi() {
        ApiCase loginCase = new ApiCase();
        loginCase.setMethod("POST");
        loginCase.setUrl(ruoyiBaseUrl + "/login");
        loginCase.setBody("{\"username\":\"" + ruoyiUsername + "\",\"password\":\"" + ruoyiPassword + "\"}");
        loginCase.setAsserts("[{\"type\":\"http_status\",\"expect\":200}]");

        HttpResult r = httpExecutor.execute(loginCase, Map.of());
        if (r.status() != 200) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(r.body());
            return root.get("token") == null ? null : root.get("token").asString();
        } catch (Exception e) {
            return null;
        }
    }

    /** 断言结果 → 摘要（报告明细展示用） */
    private String buildSummary(List<AssertResult> ars) {
        long passCount = ars.stream().filter(AssertResult::passed).count();
        String base = ars.size() + " 条断言 " + passCount + " 通过";
        String fails = ars.stream()
                .filter(a -> !a.passed())
                .map(AssertResult::message)
                .collect(Collectors.joining("；"));
        return fails.isEmpty() ? base : base + "；失败：" + fails;
    }

    /** 响应体快照截断，避免超过 TEXT 列上限 */
    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "...(已截断)" : s;
    }

    private void finishBatch(Long executionId, int passed, int failed) {
        Execution upd = new Execution();
        upd.setId(executionId);
        upd.setStatus("FINISHED");
        upd.setPassedCount(passed);
        upd.setFailedCount(failed);
        upd.setFinishTime(LocalDateTime.now());
        executionMapper.updateById(upd);
    }
}
