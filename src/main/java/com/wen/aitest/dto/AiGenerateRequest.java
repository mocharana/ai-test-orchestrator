package com.wen.aitest.dto;

/**
 * AI 生成用例请求：requirement = 自然语言需求描述（喂给大模型的原料）
 */
public record AiGenerateRequest(String requirement) {
}
