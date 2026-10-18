package com.wen.aitest.dto;

/**
 * AI 冒测请求体：record 一行顶传统类的 getter/构造器/equals（Java 16+ 特性，JDK 25 随便用）
 */
public record AiChatRequest(String message) {
}
