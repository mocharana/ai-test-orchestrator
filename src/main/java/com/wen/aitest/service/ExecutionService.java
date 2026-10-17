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
 * 与 ExecutionRunner 拆为两类，规避 @Async 同类自调用失效。
 */
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final ExecutionMapper executionMapper;
    private final ApiResultMapper apiResultMapper;
    private final ExecutionRunner runner;

    /** 发起执行：建批次后交线程池异步执行，立即返回批次 ID */
    public Long start(RunRequest req, String currentUser) {
        Execution ex = new Execution();
        ex.setStatus("RUNNING");
        ex.setTotalCount(req.getCaseIds().size());
        ex.setCreateBy(currentUser);
        executionMapper.insert(ex);
        runner.runAsync(ex.getId(), req.getCaseIds(), currentUser);
        return ex.getId();
    }

    /** 批次状态与统计（前端轮询 RUNNING → FINISHED） */
    public Execution getBatch(Long id) {
        Execution ex = executionMapper.selectById(id);
        if (ex == null) {
            throw new BizException(4001, "批次不存在");
        }
        return ex;
    }

    /** 结果明细列表 */
    public List<ApiResult> getResults(Long executionId) {
        getBatch(executionId);
        return apiResultMapper.selectList(new LambdaQueryWrapper<ApiResult>()
                .eq(ApiResult::getExecutionId, executionId)
                .orderByAsc(ApiResult::getId));
    }

    /** 批次历史（分页，最新在前） */
    public Page<Execution> page(long pageNum, long pageSize) {
        return executionMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Execution>().orderByDesc(Execution::getId));
    }
}
