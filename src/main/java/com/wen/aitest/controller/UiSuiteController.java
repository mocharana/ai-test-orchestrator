package com.wen.aitest.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wen.aitest.common.Result;
import com.wen.aitest.dto.UiSuiteRequest;
import com.wen.aitest.entity.UiResult;
import com.wen.aitest.entity.UiSuite;
import com.wen.aitest.service.UiSuiteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * UI 套件接口：
 * POST /ui-suite 新增 | PUT/DELETE /{id} 改删 | GET /{id} 单查 | GET /page 分页
 * POST /{id}/run 执行（异步秒回批次 ID）| GET /results/{executionId} 结果明细
 * 批次状态轮询复用 GET /execution/{id}。
 */
@RestController
@RequestMapping("/ui-suite")
@RequiredArgsConstructor
public class UiSuiteController {

    private final UiSuiteService uiSuiteService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody UiSuiteRequest req, HttpServletRequest request) {
        String currentUser = (String) request.getAttribute("currentUser");
        return Result.ok(uiSuiteService.create(req, currentUser));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UiSuiteRequest req) {
        uiSuiteService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        uiSuiteService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<UiSuite> getById(@PathVariable Long id) {
        return Result.ok(uiSuiteService.getById(id));
    }

    @GetMapping("/page")
    public Result<Page<UiSuite>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(uiSuiteService.page(pageNum, pageSize));
    }

    /** 执行：异步启动 pytest 子进程，立即返回批次 ID */
    @PostMapping("/{id}/run")
    public Result<Long> run(@PathVariable Long id, HttpServletRequest request) {
        String currentUser = (String) request.getAttribute("currentUser");
        return Result.ok(uiSuiteService.start(id, currentUser));
    }

    /** 某批次的 UI 执行结果明细（pytest 输出快照/退出码/耗时） */
    @GetMapping("/results/{executionId}")
    public Result<List<UiResult>> results(@PathVariable Long executionId) {
        return Result.ok(uiSuiteService.getResults(executionId));
    }
}
