-- ============================================================
-- M5-3：AI 调用日志表
--   t_ai_log：每次调智谱都留痕——prompt/响应/耗时/token
--   成本可观测的硬证据：优化前先看得见每次调用花了多少
-- ============================================================

USE `ai_test_platform`;

CREATE TABLE IF NOT EXISTS `t_ai_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `biz_type`     VARCHAR(20)  NOT NULL COMMENT '业务类型：GENERATE=生成用例 ANALYZE=失败归因',
  `model`        VARCHAR(50)  NOT NULL COMMENT '模型编码（如 glm-4.5-flash）',
  `prompt`       MEDIUMTEXT   NOT NULL COMMENT '发送的完整提示词（system + user）',
  `response`     MEDIUMTEXT   NULL COMMENT 'AI 回复原文（失败时为空）',
  `success`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1=成功 0=失败',
  `error_msg`    VARCHAR(500) NULL COMMENT '失败原因',
  `cost_ms`      BIGINT       NOT NULL DEFAULT 0 COMMENT '调用耗时（毫秒）',
  `total_tokens` INT          NULL COMMENT '本次消耗 token 数',
  `create_by`    VARCHAR(50)  NULL COMMENT '发起人（登录用户）',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz_type` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 调用日志表';
