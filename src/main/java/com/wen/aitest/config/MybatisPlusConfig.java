package com.wen.aitest.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置。
 *
 * 分页插件不配的后果：Page 参数照传，但 MP 不会拼 LIMIT——
 * 全表查回来在内存里"假分页"，数据量一大就是性能炸弹。
 *
 * 原理（面试高频）：PaginationInnerInterceptor 拦截你的查询 SQL，
 * 自动改写拼上 LIMIT offset, size，并另发一条 SELECT COUNT(*) 算总数。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
