-- ============================================================
-- AI-Test-Orchestrator 平台数据库
-- 实例：localhost:3306（与 RuoYi 同一 MySQL 实例，库完全独立）
-- 字符集：utf8mb4（完整 UTF-8，支持 emoji 等特殊字符；
--         MySQL 里叫 "utf8" 的字符集是残缺的 3 字节版，别用）
-- 排序规则：utf8mb4_0900_ai_ci（MySQL 8 默认，大小写不敏感）
-- ============================================================

CREATE DATABASE IF NOT EXISTS `ai_test_platform`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
