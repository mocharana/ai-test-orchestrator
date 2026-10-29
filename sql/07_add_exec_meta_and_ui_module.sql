-- ============================================================
-- M8-4：执行记录区分执行器 + UI 套件分模块
-- ------------------------------------------------------------
-- 1) t_execution 补 exec_type / target_name（历史快照：名称落库，不依赖用例表当前状态）
-- 2) t_ui_suite 补 module
-- 3) 回填历史批次（17 条）：类型按明细落表判断，名称取该批次首条明细
-- 执行前确认已选中 ai_test_platform 库
-- ============================================================

USE ai_test_platform;

-- ---------- 1. 批次表：执行器类型 + 执行对象快照 ----------
ALTER TABLE t_execution
    ADD COLUMN exec_type VARCHAR(10) NOT NULL DEFAULT 'API'
        COMMENT '执行器类型：API / UI' AFTER status,
    ADD COLUMN target_name VARCHAR(200) NOT NULL DEFAULT ''
        COMMENT '执行对象快照：用例名（接口）或套件名（UI）' AFTER exec_type;

-- ---------- 2. UI 套件：所属模块 ----------
ALTER TABLE t_ui_suite
    ADD COLUMN module VARCHAR(50) NOT NULL DEFAULT '默认'
        COMMENT '所属模块' AFTER suite_name;

-- ---------- 3. 回填历史批次类型 ----------
-- 判定依据：明细落在哪张结果表（t_ui_result 有记录 = UI 批次）
UPDATE t_execution e
SET e.exec_type = 'UI'
WHERE EXISTS (SELECT 1 FROM t_ui_result r WHERE r.execution_id = e.id);

UPDATE t_execution e
SET e.exec_type = 'API'
WHERE EXISTS (SELECT 1 FROM t_api_result r WHERE r.execution_id = e.id);

-- ---------- 4. 回填历史批次名称（取该批次首条明细的名称） ----------
UPDATE t_execution e
    JOIN (SELECT execution_id, MIN(id) AS rid FROM t_api_result GROUP BY execution_id) x
        ON x.execution_id = e.id
    JOIN t_api_result r ON r.id = x.rid
SET e.target_name = r.case_name
WHERE e.exec_type = 'API';

UPDATE t_execution e
    JOIN (SELECT execution_id, MIN(id) AS rid FROM t_ui_result GROUP BY execution_id) x
        ON x.execution_id = e.id
    JOIN t_ui_result r ON r.id = x.rid
SET e.target_name = r.suite_name
WHERE e.exec_type = 'UI';

-- ---------- 5. 校验 ----------
SELECT '按类型统计' AS chk;
SELECT exec_type, COUNT(*) AS cnt FROM t_execution GROUP BY exec_type;

SELECT '最近 5 个批次' AS chk;
SELECT id, exec_type, target_name, status, total_count FROM t_execution ORDER BY id DESC LIMIT 5;

SELECT '套件模块' AS chk;
SELECT id, module, suite_name FROM t_ui_suite;

-- ---------- 回滚（如需） ----------
-- ALTER TABLE t_execution DROP COLUMN target_name, DROP COLUMN exec_type;
-- ALTER TABLE t_ui_suite DROP COLUMN module;
