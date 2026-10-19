package com.wen.aitest.controller;

import com.wen.aitest.common.Result;
import com.wen.aitest.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报告统计接口（M6）：全部需登录（不在拦截器白名单）。
 * GET /report/summary 总览 | GET /report/trend 按天趋势 | GET /report/top-failed 失败 Top 用例
 */
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public Result<ReportService.Summary> summary() {
        return Result.ok(reportService.summary());
    }

    @GetMapping("/trend")
    public Result<List<ReportService.TrendDay>> trend() {
        return Result.ok(reportService.trend());
    }

    @GetMapping("/top-failed")
    public Result<List<ReportService.TopFailCase>> topFailedCases() {
        return Result.ok(reportService.topFailedCases());
    }
}
