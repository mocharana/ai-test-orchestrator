package com.wen.aitest.service;

import com.wen.aitest.entity.Execution;
import com.wen.aitest.entity.UiResult;
import com.wen.aitest.entity.UiSuite;
import com.wen.aitest.mapper.ExecutionMapper;
import com.wen.aitest.mapper.UiResultMapper;
import com.wen.aitest.mapper.UiSuiteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UI 异步执行体：用 ProcessBuilder 启动 pytest 子进程跑阶段 3 的 Selenium 用例。
 * 三个工程要点：独立线程实时读流（防缓冲区死锁）、超时强杀（防用例卡死）、
 * PYTHONIOENCODING=utf-8（防 Windows 子进程中文乱码）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UiRunner {

    private final UiSuiteMapper uiSuiteMapper;
    private final UiResultMapper uiResultMapper;
    private final ExecutionMapper executionMapper;

    @Value("${pytest.python-path}")
    private String pythonPath;
    @Value("${pytest.work-dir}")
    private String workDir;

    private static final Pattern PASSED_P = Pattern.compile("(\\d+) passed");
    private static final Pattern FAILED_P = Pattern.compile("(\\d+) failed");
    private static final long TIMEOUT_SECONDS = 300;
    private static final int OUTPUT_KEEP = 60000;

    @Async("executionExecutor")
    public void runUiAsync(Long executionId, Long suiteId, String currentUser) {
        // 兜底：任何意外不能让批次卡在 RUNNING
        try {
            doRun(executionId, suiteId);
        } catch (Exception e) {
            log.error("UI 批次 {} 执行异常", executionId, e);
            finishBatch(executionId, 0, 1);
        }
    }

    private void doRun(Long executionId, Long suiteId) {
        UiSuite suite = uiSuiteMapper.selectById(suiteId);
        long t0 = System.currentTimeMillis();
        UiResult row = new UiResult();
        row.setExecutionId(executionId);
        row.setSuiteId(suiteId);

        if (suite == null) {
            row.setSuiteName("套件#" + suiteId);
            row.setPassed(0);
            row.setExitCode(-1);
            row.setSummary("套件不存在");
            row.setCostMs(0L);
            uiResultMapper.insert(row);
            finishBatch(executionId, 0, 1);
            return;
        }
        row.setSuiteName(suite.getSuiteName());

        // ① 组装命令：python -m pytest <目标> -v --tb=short（工作目录 = 项目根，conftest/pytest.ini 生效）
        List<String> cmd = List.of(pythonPath, "-m", "pytest", suite.getPyPath(), "-v", "--tb=short");

        int exitCode;
        String summary;
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new java.io.File(workDir));
            pb.redirectErrorStream(true);
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            Process p = pb.start();

            // ② 独立线程实时读输出：不消费子进程会撑满缓冲区而死锁
            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        synchronized (output) {
                            output.append(line).append('\n');
                        }
                    }
                } catch (Exception ignored) {
                }
            });
            reader.start();

            // ③ 超时保护：Selenium 卡死（浏览器弹窗等）时强杀
            boolean done = p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                synchronized (output) {
                    output.append("\n[超过 ").append(TIMEOUT_SECONDS).append(" 秒，已强制终止]");
                }
                exitCode = -1;
                summary = "执行超时";
            } else {
                exitCode = p.exitValue();
                summary = extractSummary(output.toString());
            }
            reader.join(5000);
        } catch (Exception e) {
            log.error("pytest 子进程执行失败", e);
            exitCode = -1;
            summary = "子进程异常：" + e.getMessage();
        }

        // ④ 落库：退出码 0 = 全部通过（失败也是正常测试结果，如实记录）
        boolean pass = exitCode == 0;
        row.setPassed(pass ? 1 : 0);
        row.setExitCode(exitCode);
        row.setSummary(summary);
        String out = output.toString();
        // 超长保留尾部（统计行在结尾）
        row.setOutput(out.length() > OUTPUT_KEEP ? out.substring(out.length() - OUTPUT_KEEP) : out);
        row.setCostMs(System.currentTimeMillis() - t0);
        uiResultMapper.insert(row);

        finishBatch(executionId, pass ? 1 : 0, pass ? 0 : 1);
        log.info("UI 批次 {} 完成：exit={}，{}", executionId, exitCode, summary);
    }

    /** 从输出提取统计，如 "2 passed in 5.3s" / "1 failed, 2 passed in 8s" */
    private String extractSummary(String output) {
        Matcher mp = PASSED_P.matcher(output);
        Matcher mf = FAILED_P.matcher(output);
        int passed = -1, failed = -1;
        while (mp.find()) passed = Integer.parseInt(mp.group(1));
        while (mf.find()) failed = Integer.parseInt(mf.group(1));
        if (passed < 0 && failed < 0) {
            return "未解析到统计行（可能收集阶段出错）";
        }
        StringBuilder sb = new StringBuilder();
        if (passed >= 0) sb.append(passed).append(" passed");
        if (failed >= 0) sb.append(sb.length() > 0 ? ", " : "").append(failed).append(" failed");
        return sb.toString();
    }

    private void finishBatch(Long executionId, int passed, int failed) {
        Execution upd = new Execution();
        upd.setId(executionId);
        upd.setStatus("FINISHED");
        upd.setPassedCount(passed);
        upd.setFailedCount(failed);
        upd.setFinishTime(LocalDateTime.now());
        executionMapper.updateById(upd);
    }
}
