package com.wen.aitest.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wen.aitest.common.Result;
import com.wen.aitest.dto.RunRequest;
import com.wen.aitest.entity.ApiResult;
import com.wen.aitest.entity.Execution;
import com.wen.aitest.service.ExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 执行引擎接口：
 * POST /execution/run 发起执行（异步，秒回批次 ID）| GET /execution/{id} 轮询状态
 * GET /execution/{id}/results 明细 | GET /execution/page 批次历史
 */
@RestController
@RequestMapping("/execution")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    @PostMapping("/run")
    public Result<Long> run(@Valid @RequestBody RunRequest req, HttpServletRequest request) {
        String currentUser = (String) request.getAttribute("currentUser");
        return Result.ok(executionService.start(req, currentUser));
    }

    @GetMapping("/{id}")
    public Result<Execution> getBatch(@PathVariable Long id) {
        return Result.ok(executionService.getBatch(id));
    }

    @GetMapping("/{id}/results")
    public Result<List<ApiResult>> results(@PathVariable Long id) {
        return Result.ok(executionService.getResults(id));
    }

    @GetMapping("/page")
    public Result<Page<Execution>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(executionService.page(pageNum, pageSize));
    }
}
