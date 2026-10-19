package com.wen.aitest.controller;

import com.wen.aitest.common.BizException;
import com.wen.aitest.common.Result;
import com.wen.aitest.dto.AiChatRequest;
import com.wen.aitest.dto.AiGenerateRequest;
import com.wen.aitest.dto.AiGenerateResult;
import com.wen.aitest.service.AiAnalysisService;
import com.wen.aitest.service.AiCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * M5-1 连通冒测：ChatClient 最小调用，验证 平台→智谱 glm-4.5-flash 链路通不通。
 * POST /ai/chat {"message":"..."} → AI 回复文本。
 * M5-2 的"生成用例"会在同一调用姿势上叠加 Prompt 模板与结果校验器。
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    /** 统一 AI 对话入口：base-url / api-key / model 都已在配置里定好 */
    private final ChatClient chatClient;

    /** AI 生成用例业务（M5-2）：Prompt + 清洗 + 校验 + 落库 */
    private final AiCaseService aiCaseService;

    /** 失败归因业务（M5-3）：证据三件套 → AI 分析 */
    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/chat")
    public Result<String> chat(@RequestBody AiChatRequest req) {
        String reply = chatClient.prompt()
                .user(req.message())
                .call()      // 同步调用：发出去 → 等模型写完 → 一次性拿结果
                .content();  // 取回复正文（不含元信息）
        return Result.ok(reply);
    }

    /** 功能①：需求描述 → AI 生成接口用例（逐条机器校验，通过才入库，createBy=AI） */
    @PostMapping("/generate-cases")
    public Result<AiGenerateResult> generateCases(@RequestBody AiGenerateRequest req, HttpServletRequest request) {
        if (!StringUtils.hasText(req.requirement())) {
            throw new BizException(5002, "需求描述不能为空");
        }
        String currentUser = (String) request.getAttribute("currentUser");
        return Result.ok(aiCaseService.generate(req.requirement(), currentUser));
    }

    /** 功能②：失败归因——按单条失败明细取证据三件套喂给 AI，输出原因分析与修复建议 */
    @PostMapping("/analyze-failure/{resultId}")
    public Result<String> analyzeFailure(@PathVariable Long resultId, HttpServletRequest request) {
        String currentUser = (String) request.getAttribute("currentUser");
        return Result.ok(aiAnalysisService.analyze(resultId, currentUser));
    }
}
