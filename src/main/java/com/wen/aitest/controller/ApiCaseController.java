package com.wen.aitest.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wen.aitest.common.Result;
import com.wen.aitest.dto.ApiCaseRequest;
import com.wen.aitest.entity.ApiCase;
import com.wen.aitest.service.ApiCaseService;
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
 * 用例管理接口（RESTful）：
 * POST /api-case 新增 | PUT /api-case/{id} 修改 | DELETE /api-case/{id} 删除
 * GET /api-case/{id} 单查 | GET /api-case/page 分页筛选 | POST /api-case/import 批量导入
 * 全部需登录（不在拦截器白名单）。
 */
@RestController
@RequestMapping("/api-case")
@RequiredArgsConstructor
public class ApiCaseController {

    private final ApiCaseService apiCaseService;

    /** 新增：createBy 取拦截器写入的当前登录用户 */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ApiCaseRequest req, HttpServletRequest request) {
        String currentUser = (String) request.getAttribute("currentUser");
        apiCaseService.create(req, currentUser);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ApiCaseRequest req) {
        apiCaseService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        apiCaseService.delete(id);
        return Result.ok();
    }

    /**
     * 模块下拉选项（筛选器数据源）。
     * 注意放在 /{id} 之前：字面量路径优先于路径变量，避免被当成 id=modules 解析。
     */
    @GetMapping("/modules")
    public Result<List<String>> modules() {
        return Result.ok(apiCaseService.listModules());
    }

    @GetMapping("/{id}")
    public Result<ApiCase> getById(@PathVariable Long id) {
        return Result.ok(apiCaseService.getById(id));
    }

    /** 批量导入：请求体为 JSON 数组，任一条失败整批回滚（事务在 service 层） */
    @PostMapping("/import")
    public Result<String> importCases(@RequestBody List<ApiCaseRequest> list, HttpServletRequest request) {
        String currentUser = (String) request.getAttribute("currentUser");
        int count = apiCaseService.importCases(list, currentUser);
        return Result.ok("成功导入 " + count + " 条");
    }

    /** 分页筛选：pageNum/pageSize 有默认值，module/caseName/priority/method/source 可选 */
    @GetMapping("/page")
    public Result<Page<ApiCase>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String caseName,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String source) {
        return Result.ok(apiCaseService.page(pageNum, pageSize, module, caseName, priority, method, source));
    }
}
