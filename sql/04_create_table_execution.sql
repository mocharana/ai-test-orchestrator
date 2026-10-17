-- ============================================================
-- M3-3：执行记录两表（批次 / 明细 两层设计）
--   一次"点执行" = t_execution 一行（批次）+ t_api_result N 行（每用例一条明细）
--   报告页两层展示：先看批次（通过率），点进去看每条用例的明细
--   结果表只增不改（审计友好）——失败原因、响应快照都留在明细里
-- ============================================================

USE `ai_test_platform`;

CREATE TABLE IF NOT EXISTS `t_execution` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `status`       VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT '状态：RUNNING/FINISHED',
  `total_count`  INT         NOT NULL DEFAULT 0 COMMENT '用例总数',
  `passed_count` INT         NOT NULL DEFAULT 0 COMMENT '通过数',
  `failed_count` INT         NOT NULL DEFAULT 0 COMMENT '失败数',
  `create_by`    VARCHAR(50) NOT NULL DEFAULT '' COMMENT '发起人',
  `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `finish_time`  DATETIME    NULL COMMENT '完成时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行批次表';

CREATE TABLE IF NOT EXISTS `t_api_result` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `execution_id`  BIGINT       NOT NULL COMMENT '所属批次',
  `case_id`       BIGINT       NOT NULL COMMENT '用例ID',
  `case_name`     VARCHAR(100) NOT NULL COMMENT '用例名称（冗余存储：报告页免联表）',
  `passed`        TINYINT      NOT NULL DEFAULT 0 COMMENT '1=通过 0=失败',
  `http_status`   INT          NOT NULL DEFAULT 0 COMMENT 'HTTP 状态码（0=网络失败）',
  `assert_summary` VARCHAR(500) NULL COMMENT '断言结果摘要（如 3/3 通过 或 失败原因列表）',
  `response_body` TEXT         NULL COMMENT '响应体快照（排障用）',
  `cost_ms`       BIGINT       NOT NULL DEFAULT 0 COMMENT '耗时（毫秒）',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '落库时间',
  PRIMARY KEY (`id`),
  KEY `idx_execution` (`execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口用例执行结果明细表';
