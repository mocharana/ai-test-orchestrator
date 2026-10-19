package com.wen.aitest.service;

import com.wen.aitest.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 报告统计（M6）：聚合行（Map）在 service 层清洗成结构化 DTO——
 * 类型转换/通过率计算/除零防护都属于业务规则，不落在 SQL 里。
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;

    /** 总览：批次数 / 用例执行次数 / 通过 / 失败 / 通过率（百分比一位小数） */
    public record Summary(long batches, long cases, long passed, long failed, double passRate) {
    }

    public record TrendDay(String day, long batches, long passed, long failed) {
    }

    public record TopFailCase(Long caseId, String caseName, long failTimes) {
    }

    public Summary summary() {
        Map<String, Object> m = reportMapper.summary();
        long cases = num(m.get("cases"));
        long passed = num(m.get("passed"));
        // 通过率 = 用例级通过数 / 用例执行总数；零数据防除零
        double rate = cases == 0 ? 0.0 : Math.round(passed * 1000.0 / cases) / 10.0;
        return new Summary(num(m.get("batches")), cases, passed, num(m.get("failed")), rate);
    }

    public List<TrendDay> trend() {
        return reportMapper.trend().stream()
                .map(m -> new TrendDay(str(m.get("day")), num(m.get("batches")),
                        num(m.get("passed")), num(m.get("failed"))))
                .toList();
    }

    public List<TopFailCase> topFailedCases() {
        return reportMapper.topFailedCases().stream()
                .map(m -> new TopFailCase(
                        m.get("caseId") == null ? null : ((Number) m.get("caseId")).longValue(),
                        str(m.get("caseName")),
                        num(m.get("failTimes"))))
                .toList();
    }

    /** MySQL 聚合返回统一是 Number 家族（Long/BigDecimal…），统一转 long */
    private long num(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
