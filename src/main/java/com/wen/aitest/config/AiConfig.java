package com.wen.aitest.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 客户端装配：Spring AI 检测到依赖后自动创建 ChatClient.Builder，
 * 配置里的 base-url / api-key / model 已被 Builder 带着走。
 * 这里只 build() 一次，全项目共用同一个 ChatClient（M5-2/3 都注入它）。
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
