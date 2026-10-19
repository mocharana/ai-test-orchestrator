package com.wen.aitest.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 报告聚合查询（M6）：GROUP BY 统计走自定义 SQL——
 * MP 的 Wrapper 擅长单表 CRUD，聚合统计手写 SQL 更直白（也方便后面 EXPLAIN 优化）。
 * 聚合行以 Map 返回（列名→值），清洗成 DTO 的活交 service 层。
 */
@Mapper
public interface ReportMapper {

    /** 总览：批次数 / 用例执行次数 / 通过 / 失败（只统计跑完的批次） */
    @Select("SELECT COUNT(*) AS batches, COALESCE(SUM(total_count), 0) AS cases, " +
            "COALESCE(SUM(passed_count), 0) AS passed, COALESCE(SUM(failed_count), 0) AS failed " +
            "FROM t_execution WHERE status = 'FINISHED'")
    Map<String, Object> summary();

    /** 按天趋势（最近 14 天）：按创建日期分组 */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS day, COUNT(*) AS batches, " +
            "COALESCE(SUM(passed_count), 0) AS passed, COALESCE(SUM(failed_count), 0) AS failed " +
            "FROM t_execution WHERE status = 'FINISHED' " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') ORDER BY day DESC LIMIT 14")
    List<Map<String, Object>> trend();

    /** 失败 Top5 用例（接口用例明细；UI 明细无用例粒度不参与） */
    @Select("SELECT case_id AS caseId, case_name AS caseName, COUNT(*) AS failTimes " +
            "FROM t_api_result WHERE passed = 0 " +
            "GROUP BY case_id, case_name ORDER BY failTimes DESC LIMIT 5")
    List<Map<String, Object>> topFailedCases();
}
