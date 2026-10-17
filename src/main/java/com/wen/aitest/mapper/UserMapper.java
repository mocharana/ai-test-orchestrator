package com.wen.aitest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wen.aitest.entity.User;
import org.apache.ibatis.annotations.Mapper;

/** t_user 数据访问接口：继承 BaseMapper 获得全套 CRUD 方法 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
