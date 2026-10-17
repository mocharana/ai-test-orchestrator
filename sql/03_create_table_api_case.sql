-- ============================================================
-- M2-1：接口用例表 —— "用例即数据"的实体化
-- 设计要点：
--   1. 一条用例 = 一行数据：method/url/params/body/asserts 全是字段
--      （对照阶段 4 JMeter：你在 GUI 填的每一样东西，这里都有对应的列）
--   2. headers/params/body/asserts 用 TEXT 存 JSON 字符串：
--      灵活——用例结构千变万化，不必为每种参数建列；
--      代价——JSON 内部无法加普通索引、数据库不校验其结构（由代码层保证）
--   3. {{token}} 变量占位写在数据里，M3 执行时由 VariableResolver 替换
--   4. create_by 记录创建人：值来自 M1 拦截器挂的 currentUser（伏笔回收）
--   5. priority 对应测试用例 P0/P1/P2 分级（冒烟跑 P0，回归跑 P0+P1）
-- ============================================================

USE `ai_test_platform`;

CREATE TABLE IF NOT EXISTS `t_api_case` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用例ID',
  `case_name`   VARCHAR(100) NOT NULL                COMMENT '用例名称',
  `module`      VARCHAR(50)  NOT NULL DEFAULT '默认' COMMENT '所属模块（登录/公告/用户管理...）',
  `priority`    TINYINT      NOT NULL DEFAULT 2      COMMENT '优先级：0=P0 1=P1 2=P2',
  `method`      VARCHAR(10)  NOT NULL                COMMENT '请求方法：GET/POST/PUT/DELETE',
  `url`         VARCHAR(500) NOT NULL                COMMENT '完整请求地址（如 http://localhost:8080/system/notice/list）',
  `headers`     TEXT         NULL                    COMMENT '请求头 JSON：[{"key":"Authorization","value":"Bearer {{token}}"}]',
  `params`      TEXT         NULL                    COMMENT '查询参数 JSON：{"pageNum":1,"pageSize":10}',
  `body`        TEXT         NULL                    COMMENT '请求体 JSON（POST/PUT 使用）',
  `asserts`     TEXT         NOT NULL                COMMENT '断言规则 JSON：[{"type":"http_status","expect":200},{"type":"json_path","path":"code","expect":200}]',
  `remark`      VARCHAR(255) NULL                    COMMENT '备注',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1=启用 0=停用',
  `create_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人（登录用户名）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_module` (`module`),
  KEY `idx_case_name` (`case_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口用例表（用例即数据）';
