package com.wen.aitest.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * HTTP 客户端超时兜底：默认无读超时——AI 生成可能等几十秒，慢请求会无限转圈。
 * 统一给 RestClient 系（含 Spring AI 调智谱）加：连接 10s / 读取 120s，超时快速失败。
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.detect()
                        .build(HttpClientSettings.defaults()
                                .withConnectTimeout(Duration.ofSeconds(10))
                                .withReadTimeout(Duration.ofSeconds(120))));
    }
}
