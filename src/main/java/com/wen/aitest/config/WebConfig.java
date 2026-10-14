package com.wen.aitest.config;

import com.wen.aitest.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器注册处：告诉 Spring "哪个门卫上岗、管哪些门、豁免哪些门"。
 *
 * 实现 WebMvcConfigurer = 往 Spring MVC 的自动配置里"加料"
 * （它把 MVC 的配置点以接口方法的形式开放出来，我们重写需要的那一个）。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")            // 默认：所有接口都要票
                .excludePathPatterns(              // 白名单：这些门不查票
                        "/auth/login",             // 登录本身不能要票——不然死锁：没票怎么进买票口？
                        "/auth/register",          // 注册同上
                        "/hello",                  // 公开测试接口（M0 的老朋友，方便对比）
                        "/boom",                   // 公开测试接口
                        "/v3/api-docs/**",         // Swagger 的接口描述数据
                        "/swagger-ui/**",          // Swagger 网页
                        "/swagger-ui.html");
    }
}
