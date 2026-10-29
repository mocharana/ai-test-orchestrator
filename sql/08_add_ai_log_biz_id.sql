-- ============================================================
-- M8-6：AI 归因结果落库回看
--   t_ai_log 补 biz_id：记录这次调用属于哪个业务对象
--     - ANALYZE  -> t_api_result.id（哪条失败明细）
--     - GENERATE -> NULL（生成用例暂无单对象语义）
--   配合 (biz_type, biz_id) 联合索引，支持"按明细回查历史归因"
-- ============================================================

USE ai_test_platform;

-- ---------- 1. 补业务对象 ID ----------
ALTER TABLE t_ai_log
    ADD COLUMN biz_id BIGINT NULL
        COMMENT '业务对象 ID：ANALYZE 时为 t_api_result.id；GENERATE 为空'
        AFTER biz_type;

-- ---------- 2. 联合索引：按 bizType + bizId 回查最新一条 ----------
CREATE INDEX idx_ai_log_biz ON t_ai_log (biz_type, biz_id);

-- ---------- 3. 校验 ----------
SELECT '日志总数 / 已有 biz_id 的条数' AS chk;
SELECT COUNT(*) AS total, SUM(biz_id IS NOT NULL) AS with_biz FROM t_ai_log;

SELECT '按业务类型' AS chk;
SELECT biz_type, COUNT(*) AS cnt FROM t_ai_log GROUP BY biz_type;

-- 说明：存量 ANALYZE 日志（M5-3 时代）prompt 里只有证据包、没有 resultId，
--       无法可靠回填，保持 NULL——历史归因不可回看，新产生的可以。

-- ---------- 回滚（如需） ----------
-- DROP INDEX idx_ai_log_biz ON t_ai_log;
-- ALTER TABLE t_ai_log DROP COLUMN biz_id;
