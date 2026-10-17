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

/**
 * 用例业务：CRUD + 分页筛选。
 */
@Service
@RequiredArgsConstructor
public class ApiCaseService {

    private final ApiCaseMapper apiCaseMapper;

    /**
     * 新增。currentUser 从 Controller 一路传来（拦截器从票里取出的名字）——
     * 这就是"create_by 从哪来"的正确答案链的最后一环。
     */
    public void create(ApiCaseRequest req, String currentUser) {
        ApiCase c = new ApiCase();
        c.setCaseName(req.getCaseName());
        c.setModule(req.getModule() == null ? "默认" : req.getModule());
        c.setPriority(req.getPriority() == null ? 2 : req.getPriority());
        c.setMethod(req.getMethod().toUpperCase());   // 统一大写，避免 get/get 混用
        c.setUrl(req.getUrl());
        c.setHeaders(req.getHeaders());
        c.setParams(req.getParams());
        c.setBody(req.getBody());
        c.setAsserts(req.getAsserts());
        c.setRemark(req.getRemark());
        c.setStatus(1);
        c.setCreateBy(currentUser);                   // ← 伏笔回收：M1 拦截器的 currentUser
        apiCaseMapper.insert(c);
    }

    /** 修改：先确认存在，再只更新业务字段（不碰 createBy/createTime） */
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
        apiCaseMapper.updateById(c);   // MP 只 SET 非 null 字段
    }

    /** 删除 */
    public void delete(Long id) {
        checkExist(id);
        apiCaseMapper.deleteById(id);
    }

    /** 单查 */
    public ApiCase getById(Long id) {
        return checkExist(id);
    }

    /**
     * 分页 + 筛选：module 精确、caseName 模糊、priority 精确，条件全部可选。
     *
     * 技巧：eq/like 的第一个参数是布尔条件——成立才把该条件拼进 SQL，
     * 天然实现"传了才筛"的动态查询，不用 if-else 拼 SQL。
     */
    public Page<ApiCase> page(long pageNum, long pageSize, String module, String caseName, Integer priority) {
        LambdaQueryWrapper<ApiCase> wrapper = new LambdaQueryWrapper<ApiCase>()
                .eq(StringUtils.hasText(module), ApiCase::getModule, module)
                .like(StringUtils.hasText(caseName), ApiCase::getCaseName, caseName)
                .eq(priority != null, ApiCase::getPriority, priority)
                .orderByDesc(ApiCase::getId);
        // 分页插件（MybatisPlusConfig 里配的）会自动把这条查询改写成 LIMIT 并另发 COUNT
        return apiCaseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    private ApiCase checkExist(Long id) {
        ApiCase c = apiCaseMapper.selectById(id);
        if (c == null) {
            throw new BizException(2001, "用例不存在");
        }
        return c;
    }

    /**
     * 批量导入。@Transactional = 原子性：要么全部入库，要么全部回滚。
     * 场景：导 10 条用例，第 7 条缺必填字段 → 前 6 条也回滚，绝不留"半拉子"数据。
     *
     * 原理（兑现 M0 埋的 AOP 钩子）：Spring 用动态代理把本方法包了一层——
     *   开启事务 → 执行方法体 → 正常返回 = 提交 / 抛 RuntimeException = 回滚。
     * 这就是"注解=标签、框架包装增强"的实战样本。
     *
     * 校验为什么放 service 手写而不是 @Valid？——
     * @RequestBody List<ApiCaseRequest> 上的 @Valid 不会逐元素校验（Spring 已知行为），
     * 批量场景的校验必须下沉到 service 循环里做。
     *
     * ⚠️ 埋个思考题：这里如果写 this.create(req, currentUser) 复用单条逻辑，
     *    且 create 上也有 @Transactional——事务会失效！为什么？（自调用不走代理）
     */
    @Transactional
    public int importCases(List<ApiCaseRequest> list, String currentUser) {
        if (list == null || list.isEmpty()) {
            throw new BizException(2002, "导入列表为空");
        }
        int count = 0;
        for (ApiCaseRequest req : list) {
            // 手动校验必填（带序号，失败时能定位是第几条）
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
}
