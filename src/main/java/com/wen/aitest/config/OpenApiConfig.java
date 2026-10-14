package com.wen.aitest.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 配置：向文档页声明"本平台的接口用 Bearer token 认证"。
 *
 * 声明后 Swagger 页面右上角出现 Authorize 按钮：
 * 登录拿到 token → 粘贴一次 → 之后每个接口试调都自动带上请求头，
 * 不用手动一个一个加 Authorization 头。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")          // HTTP Bearer 方案
                                .bearerFormat("JWT")));    // 文档展示用标注
    }
}
