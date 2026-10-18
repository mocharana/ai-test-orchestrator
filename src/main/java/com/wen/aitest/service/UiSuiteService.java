package com.wen.aitest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wen.aitest.common.BizException;
import com.wen.aitest.dto.UiSuiteRequest;
import com.wen.aitest.entity.Execution;
import com.wen.aitest.entity.UiResult;
import com.wen.aitest.entity.UiSuite;
import com.wen.aitest.mapper.ExecutionMapper;
import com.wen.aitest.mapper.UiResultMapper;
import com.wen.aitest.mapper.UiSuiteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UI 套件管理（同步侧）：CRUD / 发起执行 / 查结果。
 * 与 UiRunner 拆两类，规避 @Async 同类自调用失效（同 M3 的 ExecutionService/Runner）。
 */
@Service
@RequiredArgsConstructor
public class UiSuiteService {

    private final UiSuiteMapper uiSuiteMapper;
    private final UiResultMapper uiResultMapper;
    private final ExecutionMapper executionMapper;
    private final UiRunner uiRunner;

    /** 新增套件 */
    public Long create(UiSuiteRequest req, String currentUser) {
        UiSuite s = new UiSuite();
        s.setSuiteName(req.getSuiteName());
        s.setPyPath(req.getPyPath());
        s.setDescription(req.getDescription());
        s.setCreateBy(currentUser);
        uiSuiteMapper.insert(s);
        return s.getId();
    }

    /** 修改 */
    public void update(Long id, UiSuiteRequest req) {
        checkExist(id);
        UiSuite s = new UiSuite();
        s.setId(id);
        s.setSuiteName(req.getSuiteName());
        s.setPyPath(req.getPyPath());
        s.setDescription(req.getDescription());
        uiSuiteMapper.updateById(s);
    }

    public void delete(Long id) {
        checkExist(id);
        uiSuiteMapper.deleteById(id);
    }

    public UiSuite getById(Long id) {
        return checkExist(id);
    }

    /** 套件分页 */
    public Page<UiSuite> page(long pageNum, long pageSize) {
        return uiSuiteMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UiSuite>().orderByDesc(UiSuite::getId));
    }

    /** 发起 UI 执行：建批次后交线程池，立即返回批次 ID */
    public Long start(Long suiteId, String currentUser) {
        checkExist(suiteId);
        Execution ex = new Execution();
        ex.setStatus("RUNNING");
        ex.setTotalCount(1);
        ex.setCreateBy(currentUser);
        executionMapper.insert(ex);
        uiRunner.runUiAsync(ex.getId(), suiteId, currentUser);
        return ex.getId();
    }

    /** 某批次下的 UI 执行结果明细 */
    public List<UiResult> getResults(Long executionId) {
        return uiResultMapper.selectList(new LambdaQueryWrapper<UiResult>()
                .eq(UiResult::getExecutionId, executionId)
                .orderByAsc(UiResult::getId));
    }

    private UiSuite checkExist(Long id) {
        UiSuite s = uiSuiteMapper.selectById(id);
        if (s == null) {
            throw new BizException(6001, "套件不存在");
        }
        return s;
    }
}
