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
 * 用例管理五接口——RESTful 风格：
 * POST   /api-case          新增
 * PUT    /api-case/{id}     修改
 * DELETE /api-case/{id}     删除
 * GET    /api-case/{id}     单查
 * GET    /api-case/page     分页筛选
 *
 * 全部不在白名单 → 必须带票（M1 拦截器保护的第一批真实业务接口）。
 */
@RestController
@RequestMapping("/api-case")
@RequiredArgsConstructor
public class ApiCaseController {

    private final ApiCaseService apiCaseService;

    /** 新增。HttpServletRequest 由框架注入，getAttribute 取门卫挂的用户名 */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ApiCaseRequest req, HttpServletRequest request) {
        String currentUser = (String) request.getAttribute("currentUser");
        apiCaseService.create(req, currentUser);
        return Result.ok();
    }

    /** {id} 是路径占位符：请求 /api-case/5 → @PathVariable 拿到 5 */
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

    @GetMapping("/{id}")
    public Result<ApiCase> getById(@PathVariable Long id) {
        return Result.ok(apiCaseService.getById(id));
    }

    /**
     * 批量导入。请求体是 JSON 数组（不是单个对象）：
     * [ {...}, {...} ] —— 每个元素结构和单条新增一致。
     * 事务在 service 层：一条坏数据 = 整批回滚。
     */
    @PostMapping("/import")
    public Result<String> importCases(@RequestBody List<ApiCaseRequest> list, HttpServletRequest request) {
        String currentUser = (String) request.getAttribute("currentUser");
        int count = apiCaseService.importCases(list, currentUser);
        return Result.ok("成功导入 " + count + " 条");
    }

    /**
     * 分页筛选。@RequestParam 读 URL 问号后的参数：
     * GET /api-case/page?pageNum=1&pageSize=10&module=公告模块
     * defaultValue = 不传给默认值；required=false = 可选参数
     */
    @GetMapping("/page")
    public Result<Page<ApiCase>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String caseName,
            @RequestParam(required = false) Integer priority) {
        return Result.ok(apiCaseService.page(pageNum, pageSize, module, caseName, priority));
    }
}
