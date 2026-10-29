package com.wen.aitest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wen.aitest.entity.ApiCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** t_api_case 数据访问接口 */
@Mapper
public interface ApiCaseMapper extends BaseMapper<ApiCase> {

    /**
     * 模块下拉选项：库里现存的模块名（去重排序）。
     * 不硬编码——模块是业务数据，新增模块后下拉自动出现。
     */
    @Select("SELECT DISTINCT module FROM t_api_case WHERE module IS NOT NULL AND module <> '' ORDER BY module")
    List<String> selectDistinctModules();
}
