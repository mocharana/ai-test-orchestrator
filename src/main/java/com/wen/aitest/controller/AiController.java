package com.wen.aitest.controller;

import com.wen.aitest.common.Result;
import com.wen.aitest.dto.AiChatRequest;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/chat")
    public Result<String> chat(@RequestBody AiChatRequest req) {
        String reply = chatClient.prompt()
                .user(req.message())
                .call()      // 同步调用：发出去 → 等模型写完 → 一次性拿结果
                .content();  // 取回复正文（不含元信息）
        return Result.ok(reply);
    }
}
