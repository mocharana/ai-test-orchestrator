-- ============================================================
-- M4-1：UI 套件两表
--   t_ui_suite：套件配置——指向磁盘上一个 pytest 用例路径
--   t_ui_result：UI 执行结果——pytest 输出快照 + 退出码
-- ============================================================

USE `ai_test_platform`;

CREATE TABLE IF NOT EXISTS `t_ui_suite` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '套件ID',
  `suite_name`  VARCHAR(100) NOT NULL COMMENT '套件名称',
  `py_path`     VARCHAR(500) NOT NULL COMMENT 'pytest 目标路径（目录或单个用例文件，相对 work-dir）',
  `description` VARCHAR(255) NULL COMMENT '备注',
  `create_by`   VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UI 套件配置表';

CREATE TABLE IF NOT EXISTS `t_ui_result` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `execution_id` BIGINT      NOT NULL COMMENT '所属批次（复用 t_execution）',
  `suite_id`     BIGINT      NOT NULL COMMENT '套件ID',
  `suite_name`   VARCHAR(100) NOT NULL COMMENT '套件名称（冗余）',
  `passed`       TINYINT     NOT NULL DEFAULT 0 COMMENT '1=全部通过 0=存在失败',
  `exit_code`    INT         NOT NULL DEFAULT -1 COMMENT 'pytest 退出码：0=全过 1=有失败 其他=异常',
  `summary`      VARCHAR(255) NULL COMMENT '统计摘要（如 3 passed, 1 failed in 12.3s）',
  `output`       MEDIUMTEXT  NULL COMMENT 'pytest 控制台输出快照',
  `cost_ms`      BIGINT      NOT NULL DEFAULT 0 COMMENT '耗时（毫秒）',
  `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '落库时间',
  PRIMARY KEY (`id`),
  KEY `idx_execution` (`execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UI 套件执行结果表';
