package com.wen.aitest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wen.aitest.entity.ApiCase;
import org.apache.ibatis.annotations.Mapper;

/** t_api_case 数据访问接口 */
@Mapper
public interface ApiCaseMapper extends BaseMapper<ApiCase> {
}
