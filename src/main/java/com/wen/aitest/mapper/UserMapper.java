package com.wen.aitest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wen.aitest.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对 t_user 做增删改查的"手"。
 *
 * 继承 BaseMapper&lt;User&gt; 后白得一整套方法：
 *   insert(user) / selectById(id) / selectList(...) / updateById(user) / deleteById(id) ...
 * 一行 SQL 都不用写——MyBatis-Plus 启动时按"实体↔表"的约定自动生成实现。
 *
 * @Mapper 是又一个"标签"：Spring 启动时读到它，
 * 用动态代理为这个接口生成实现类并注册为 Bean（呼应 M0 讲的注解机制）。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // 暂时空着：BaseMapper 自带的方法已够 M1 使用。
    // 以后复杂查询（如 M6 报表统计）才在这里写自定义 SQL。
}
