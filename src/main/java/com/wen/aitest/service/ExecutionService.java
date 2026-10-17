package com.wen.aitest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wen.aitest.common.BizException;
import com.wen.aitest.dto.RunRequest;
import com.wen.aitest.entity.ApiResult;
import com.wen.aitest.entity.Execution;
import com.wen.aitest.mapper.ApiResultMapper;
import com.wen.aitest.mapper.ExecutionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 执行管理（同步侧）：发起执行 / 查批次 / 查明细 / 批次历史。
 *
 * 为什么和 ExecutionRunner 拆成两个类？
 *   @Async 的失效场景①：同类里 this.runAsync() 自调用不走代理 → 异步退化成同步。
 *   拆类 = 从结构上杜绝这个坑（M2-3 事务失效四场景的正面规避，面试可讲）。
 */
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final ExecutionMapper executionMapper;
    private final ApiResultMapper apiResultMapper;
    private final ExecutionRunner runner;

    /**
     * 发起执行：建批次（RUNNING）→ 丢进线程池 → 立即返回批次 ID。
     * 异步的意义：跑 10 条用例可能要几秒，同步会让前端干等；异步秒回，前端轮询进度。
     */
    public Long start(RunRequest req, String currentUser) {
        Execution ex = new Execution();
        ex.setStatus("RUNNING");
        ex.setTotalCount(req.getCaseIds().size());
        ex.setCreateBy(currentUser);
        executionMapper.insert(ex);
        // 跨类调用 → 走代理 → @Async 生效（自调用的反面）
        runner.runAsync(ex.getId(), req.getCaseIds(), currentUser);
        return ex.getId();
    }

    /** 批次状态与统计（前端轮询这个接口看 RUNNING → FINISHED） */
    public Execution getBatch(Long id) {
        Execution ex = executionMapper.selectById(id);
        if (ex == null) {
            throw new BizException(4001, "批次不存在");
        }
        return ex;
    }

    /** 结果明细列表 */
    public List<ApiResult> getResults(Long executionId) {
        getBatch(executionId);   // 顺带校验批次存在
        return apiResultMapper.selectList(new LambdaQueryWrapper<ApiResult>()
                .eq(ApiResult::getExecutionId, executionId)
                .orderByAsc(ApiResult::getId));
    }

    /** 批次历史（分页，最新的在前） */
    public Page<Execution> page(long pageNum, long pageSize) {
        return executionMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Execution>().orderByDesc(Execution::getId));
    }
}
