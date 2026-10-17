package com.wen.aitest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 执行专用线程池配置。
 * 自定义原因：@Async 默认池不复用线程、无上限；跑用例属 IO 密集，2~4 线程够用。
 * 执行顺序：核心线程 → 队列(50) → 扩容至 max(4) → 拒绝策略（Abort 抛异常）。
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean("executionExecutor")
    public ThreadPoolTaskExecutor executionExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(50);
        ex.setKeepAliveSeconds(60);
        ex.setThreadNamePrefix("exec-");
        // 批次被拒必须让用户知道，不能用静默丢弃类策略
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return ex;
    }
}
