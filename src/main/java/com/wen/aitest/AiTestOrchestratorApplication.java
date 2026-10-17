package com.wen.aitest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 平台启动入口。
 * 必须位于 com.wen.aitest 根包：组件扫描默认覆盖本包及所有子包。
 */
@SpringBootApplication
public class AiTestOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTestOrchestratorApplication.class, args);
    }
}
