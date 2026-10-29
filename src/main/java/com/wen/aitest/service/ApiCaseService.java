package com.wen.aitest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wen.aitest.common.BizException;
import com.wen.aitest.dto.ApiCaseRequest;
import com.wen.aitest.entity.ApiCase;
import com.wen.aitest.mapper.ApiCaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/** 用例业务：CRUD + 分页筛选 + 批量导入 */
@Service
@RequiredArgsConstructor
public class ApiCaseService {

    private final ApiCaseMapper apiCaseMapper;

    /** 新增。currentUser 来自拦截器写入的当前登录用户 */
    public void create(ApiCaseRequest req, String currentUser) {
        ApiCase c = new ApiCase();
        c.setCaseName(req.getCaseName());
        c.setModule(req.getModule() == null ? "默认" : req.getModule());
        c.setPriority(req.getPriority() == null ? 2 : req.getPriority());
        c.setMethod(req.getMethod().toUpperCase());
        c.setUrl(req.getUrl());
        c.setHeaders(req.getHeaders());
        c.setParams(req.getParams());
        c.setBody(req.getBody());
        c.setAsserts(req.getAsserts());
        c.setRemark(req.getRemark());
        c.setStatus(1);
        c.setCreateBy(currentUser);
        apiCaseMapper.insert(c);
    }

    /** 修改：仅更新业务字段（不碰 createBy/createTime），MP 只 SET 非 null 字段 */
    public void update(Long id, ApiCaseRequest req) {
        checkExist(id);
        ApiCase c = new ApiCase();
        c.setId(id);
        c.setCaseName(req.getCaseName());
        c.setModule(req.getModule() == null ? "默认" : req.getModule());
        c.setPriority(req.getPriority() == null ? 2 : req.getPriority());
        c.setMethod(req.getMethod().toUpperCase());
        c.setUrl(req.getUrl());
        c.setHeaders(req.getHeaders());
        c.setParams(req.getParams());
        c.setBody(req.getBody());
        c.setAsserts(req.getAsserts());
        c.setRemark(req.getRemark());
        apiCaseMapper.updateById(c);
    }

    public void delete(Long id) {
        checkExist(id);
        apiCaseMapper.deleteById(id);
    }

    public ApiCase getById(Long id) {
        return checkExist(id);
    }

    /**
     * 分页 + 可选筛选：module 精确、caseName 模糊、priority 精确、method 精确、source 来源。
     * eq/like 首参为布尔条件——成立才拼进 SQL，实现动态查询。
     * 筛选下推数据库而非前端过滤：前端只有当前页 10 条，过滤会破坏分页语义。
     * source 语义：AI = 大模型生成（create_by='AI'）；MANUAL = 人工创建（create_by<>'AI'）。
     */
    public Page<ApiCase> page(long pageNum, long pageSize, String module, String caseName,
                              Integer priority, String method, String source) {
        // 方法名统一大写再比对（库里存的是大写，前端下拉也是大写，此处兜底）
        String methodUpper = StringUtils.hasText(method) ? method.toUpperCase() : null;
        LambdaQueryWrapper<ApiCase> wrapper = new LambdaQueryWrapper<ApiCase>()
                .eq(StringUtils.hasText(module), ApiCase::getModule, module)
                .like(StringUtils.hasText(caseName), ApiCase::getCaseName, caseName)
                .eq(priority != null, ApiCase::getPriority, priority)
                .eq(methodUpper != null, ApiCase::getMethod, methodUpper)
                .eq("AI".equals(source), ApiCase::getCreateBy, "AI")
                .ne("MANUAL".equals(source), ApiCase::getCreateBy, "AI")
                .orderByDesc(ApiCase::getId);
        return apiCaseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /** 模块下拉选项：库里现存的模块名（去重），供前端筛选器使用 */
    public List<String> listModules() {
        return apiCaseMapper.selectDistinctModules();
    }

    /**
     * 批量导入。@Transactional 保证原子性：任一条校验失败，整批回滚。
     * 校验手写在循环里：@RequestBody List 上的 @Valid 不逐元素校验（Spring 已知行为）。
     * ⚠️ 此处不可用 this.create(...) 复用单条逻辑——自调用会导致事务失效。
     */
    @Transactional
    public int importCases(List<ApiCaseRequest> list, String currentUser) {
        if (list == null || list.isEmpty()) {
            throw new BizException(2002, "导入列表为空");
        }
        int count = 0;
        for (ApiCaseRequest req : list) {
            if (!StringUtils.hasText(req.getCaseName())
                    || !StringUtils.hasText(req.getMethod())
                    || !StringUtils.hasText(req.getUrl())
                    || !StringUtils.hasText(req.getAsserts())) {
                throw new BizException(2002, "第 " + (count + 1) + " 条用例缺少必填字段（caseName/method/url/asserts）");
            }

            ApiCase c = new ApiCase();
            c.setCaseName(req.getCaseName());
            c.setModule(StringUtils.hasText(req.getModule()) ? req.getModule() : "默认");
            c.setPriority(req.getPriority() == null ? 2 : req.getPriority());
            c.setMethod(req.getMethod().toUpperCase());
            c.setUrl(req.getUrl());
            c.setHeaders(req.getHeaders());
            c.setParams(req.getParams());
            c.setBody(req.getBody());
            c.setAsserts(req.getAsserts());
            c.setRemark(req.getRemark());
            c.setStatus(1);
            c.setCreateBy(currentUser);
            apiCaseMapper.insert(c);
            count++;
        }
        return count;
    }

    private ApiCase checkExist(Long id) {
        ApiCase c = apiCaseMapper.selectById(id);
        if (c == null) {
            throw new BizException(2001, "用例不存在");
        }
        return c;
    }
}
