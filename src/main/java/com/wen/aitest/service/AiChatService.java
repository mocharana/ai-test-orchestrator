package com.wen.aitest.service;

import com.wen.aitest.common.BizException;
import com.wen.aitest.entity.AiLog;
import com.wen.aitest.mapper.AiLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI 统一调用通道：所有业务（生成用例 / 失败归因）都从这里调智谱——
 * 调用前后自动写 t_ai_log（prompt/响应/耗时/token），成本可观测。
 * 业务层只给 bizType + system + user，不直接摸 ChatClient。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient chatClient;
    private final AiLogMapper aiLogMapper;

    /** 模型编码从配置读取（application.yml 里定过 glm-4.5-flash） */
    @Value("${spring.ai.openai.chat.options.model}")
    private String modelName;

    /** 调用结果：回复正文 + 消耗 token + 耗时 */
    public record AiCallResult(String reply, Integer totalTokens, long costMs) {
    }

    /**
     * 调用智谱并统一留痕。成功失败都写 t_ai_log；失败抛 BizException 交全局异常处理器。
     *
     * @param bizType     业务类型：GENERATE / ANALYZE
     * @param bizId       业务对象 ID：ANALYZE 传 t_api_result.id（供历史归因回查）；GENERATE 传 null
     * @param system      角色与游戏规则（稳定）
     * @param user        本次输入（每次变）
     * @param createBy    发起人
     * @param temperature 采样温度：结构化输出求稳用低值
     */
    public AiCallResult call(String bizType, Long bizId, String system, String user,
                             String createBy, double temperature) {
        long t0 = System.currentTimeMillis();
        String reply = null;
        Integer totalTokens = null;
        boolean success = true;
        String errorMsg = null;

        try {
            ChatResponse resp = chatClient.prompt()
                    .system(system)
                    .user(user)
                    .options(OpenAiChatOptions.builder()
                            .temperature(temperature)
                            // 关闭 GLM-4.5 默认开启的深度思考：结构化输出求稳求快（实测 50s→15s）
                            .extraBody(Map.of("thinking", Map.of("type", "disabled"))))
                    .call()
                    .chatResponse();
            reply = resp.getResult().getOutput().getText();
            totalTokens = resp.getMetadata().getUsage().getTotalTokens();
        } catch (Exception e) {
            success = false;
            errorMsg = e.getClass().getSimpleName() + "：" + e.getMessage();
            log.warn("AI 调用失败（{}）：{}", bizType, errorMsg);
        }
        long costMs = System.currentTimeMillis() - t0;

        // 统一留痕：成功失败都写——失败记录同样是成本的一部分
        AiLog row = new AiLog();
        row.setBizType(bizType);
        row.setBizId(bizId);
        row.setModel(modelName);
        row.setPrompt(system + "\n-----\n" + user);
        row.setResponse(reply);
        row.setSuccess(success ? 1 : 0);
        row.setErrorMsg(errorMsg);
        row.setCostMs(costMs);
        row.setTotalTokens(totalTokens);
        row.setCreateBy(createBy);
        aiLogMapper.insert(row);

        if (!success) {
            throw new BizException(5003, "AI 调用失败：" + errorMsg);
        }
        return new AiCallResult(reply, totalTokens, costMs);
    }
}
