package com.wen.aitest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 平台启动入口。
 *
 * 必须！放在 com.wen.aitest 根包下：
 * @SpringBootApplication 自带"扫描当前包及所有子包"的约定，
 * 放到更深的子包（如 util）会导致兄弟包里的组件扫描不到。
 */
@SpringBootApplication
public class AiTestOrchestratorApplication {

    public static void main(String[] args) {
        // 启动 = 拉起 Spring 容器（装配所有 Bean）+ 启动内嵌 Tomcat（开始监听 8081）
        SpringApplication.run(AiTestOrchestratorApplication.class, args);
    }
}
