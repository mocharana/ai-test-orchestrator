package com.wen.aitest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步执行配置：给"跑用例"配一个专用线程池。
 *
 * 为什么自定义而不是用 @Async 默认线程池？
 *   Spring Boot 的 @Async 默认用 SimpleAsyncTaskExecutor——每来一个任务就 new 一个线程，
 *   不复用、无上限——批量执行时就是线程爆炸。生产姿势永远是自定义池。
 *
 * 七参数与执行流程（八股主场，背下这张图）：
 *   来任务 → 核心线程有空闲？用核心
 *         → 核心满？进队列排队
 *         → 队列满？开新线程直到 maxPoolSize
 *         → max 也满？触发拒绝策略
 *   ⚠️ 顺序是"先排队再扩容"——和直觉相反，面试常挖这个坑。
 *
 * 参数怎么定（IO 密集 vs CPU 密集）：
 *   跑用例 = 发 HTTP 等响应 = IO 密集（线程大部分时间在等网络），
 *   线程数可以比 CPU 核数多；本项目是本地学习平台，2~4 保守够用。
 */
@EnableAsync          // 激活 @Async：让 Spring 扫描异步方法并代理到线程池
@Configuration
public class AsyncConfig {

    @Bean("executionExecutor")
    public ThreadPoolTaskExecutor executionExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);            // 常驻线程：同时最多跑 2 个批次
        ex.setMaxPoolSize(4);             // 队列也满时最多扩到 4
        ex.setQueueCapacity(50);          // 等待队列：50 个批次排队
        ex.setKeepAliveSeconds(60);       // 非核心线程空闲 60s 回收
        ex.setThreadNamePrefix("exec-");  // 线程名前缀：日志里看到 exec-1/exec-2 就知道是执行引擎
        // 拒绝策略：池满且队列满时怎么办（四选一）
        //   AbortPolicy   抛异常（默认）——本平台选它：批次被拒要让用户知道，不能静默丢
        //   CallerRunsPolicy 让提交者自己跑 / DiscardPolicy 静默丢 / DiscardOldestPolicy 丢最老
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return ex;
    }
}
