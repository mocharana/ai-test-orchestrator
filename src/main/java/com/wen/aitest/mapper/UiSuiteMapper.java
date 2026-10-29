package com.wen.aitest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wen.aitest.entity.UiSuite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** t_ui_suite 数据访问接口 */
@Mapper
public interface UiSuiteMapper extends BaseMapper<UiSuite> {

    /**
     * 模块下拉选项：库里现存的模块名（去重排序）。
     * 与 ApiCaseMapper.selectDistinctModules 同款做法——模块是业务数据，不硬编码。
     */
    @Select("SELECT DISTINCT module FROM t_ui_suite WHERE module IS NOT NULL AND module <> '' ORDER BY module")
    List<String> selectDistinctModules();
}
