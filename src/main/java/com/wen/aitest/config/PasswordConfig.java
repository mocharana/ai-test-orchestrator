package com.wen.aitest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Bean 的两种来源之一：@Configuration + @Bean 手工声明
 * （另一种是 @Service/@Component 等标签被组件扫描自动发现）。
 *
 * 为什么它需要手工声明？BCryptPasswordEncoder 是第三方库的类，
 * 类上没贴任何 Spring 标签（库写的时候根本不知道我们项目存在），
 * 组件扫描扫不到它——只能由我们亲手把它"登记"成 Bean。
 */
@Configuration  // 标签：我是个配置类，里面可能有 @Bean 方法
public class PasswordConfig {

    @Bean  // 标签：本方法返回值登记为 Bean，名字默认叫方法名 passwordEncoder
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
