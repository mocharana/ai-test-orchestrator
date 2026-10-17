package com.wen.aitest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wen.aitest.entity.ApiCase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对 t_api_case 做增删改查的"手"。
 * 和 UserMapper 同款：@Mapper 标签 + 继承 BaseMapper 白送全套方法。
 */
@Mapper
public interface ApiCaseMapper extends BaseMapper<ApiCase> {
}
