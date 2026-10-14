-- ============================================================
-- M1-1：平台用户表
-- 设计要点：
--   1. password 存 BCrypt 密文（60 字符固定长度），绝不存明文
--   2. username 唯一索引：登录账号不可重复——数据库层兜底，
--      而不是只靠代码判断（两个请求同时注册也防得住）
--   3. status 软状态：禁用用户不删数据，只挡登录
--   4. create/update_time 数据库自动填，代码不用管
--   5. 开头 USE 指定库：脚本自包含，谁执行都不会报
--      "ERROR 1046: No database selected"
-- ============================================================

USE `ai_test_platform`;

CREATE TABLE IF NOT EXISTS `t_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`    VARCHAR(50)  NOT NULL                COMMENT '用户名（登录账号）',
  `password`    VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt 密文，非明文）',
  `nickname`    VARCHAR(50)  NULL                    COMMENT '昵称（展示用）',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1=启用 0=禁用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台用户表';
