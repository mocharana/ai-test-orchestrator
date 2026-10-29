# AI-Test-Orchestrator — AI 智能测试平台

基于 **Spring Boot 4.1 + Vue 3** 的测试平台：**用例即数据**（请求方法、URL、断言规则全部是数据库字段）+ **混合执行引擎**（接口用例 Java 内置引擎执行，UI 用例桥接 Python+Selenium 存量资产）+ **AI 加持**（大模型生成测试用例、失败归因）。被测对象为开源项目 **RuoYi-Vue v3.9.2**。

## 功能一览

- **账号体系**：JWT 登录 + BCrypt 注册（自动加盐，密文自描述）
- **接口用例管理**：CRUD、**四维下拉筛选**（模块 / 优先级 / 来源 / 方法，条件全部下推数据库）、批量导入（事务原子性）
- **接口执行引擎**：异步批次（点执行秒回批次 ID）、变量替换（`{{token}}`）、HTTP 状态码 + JSON 路径双断言
- **UI 测试执行**：ProcessBuilder 桥接 pytest，复用存量 Selenium 用例，输出快照落库；套件按模块归类
- **执行记录**：批次区分接口 / UI 引擎，显示执行对象快照（用例名 / 套件名）
- **AI 生成用例**：需求描述 → 智谱 GLM → 数据清洗 → 五规则校验 → 入库（createBy=AI）
- **AI 失败归因**：证据三件套喂给 AI，结果**行内展开**在失败行下方，并落 `t_ai_log` 支持回看——首次 5.7s / 537 token，回看 77ms / 0 token
- **测试报告**：通过率、14 天趋势、失败 Top 用例（GROUP BY 聚合 + 覆盖索引优化）

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot **4.1.1**（Java 25）· MyBatis-Plus **3.5.17**（boot4 starter）· Spring AI **2.0.1** · JJWT 0.13.0 · springdoc 3.0.2 |
| AI | 智谱 GLM（`glm-4.5-flash`，OpenAI 兼容协议接入） |
| 前端 | Vue **3.5** · Vite **4.5** · Element Plus 2.9 · axios |
| 数据库 | MySQL 8（独立库 `ai_test_platform`，七表） |
| CI | GitHub Actions（push 自动构建 + 运行 13 个 JUnit 单元测试） |

## 设计选型

### 为什么 Spring Boot 4.1

Spring 官方当前主版本线；与本地被测系统 RuoYi（同代 Spring Boot 4 系）技术形态一致，排障经验互通。生态适配已验证：MyBatis-Plus、Spring AI 均有 SB4 正式适配（分页插件拆分、Jackson 3 换包名两个坑踩过并沉淀）。

### 为什么混合引擎（重写 vs 桥接）

阶段 1~3 已沉淀一套 pytest+Selenium 的 UI 测试框架（13 用例全绿）——**用 Java 重写等于抛弃存量资产**。采用 ProcessBuilder 桥接：Java 拼"Python 环境 + 用例路径"命令行起子进程，平台只做调度与结果回收，Python 代码零改动。接口用例则是"用例即数据"路线，内置引擎原生执行。

### 为什么 Spring AI（而非智谱专用 SDK）

走 **OpenAI 兼容协议**——大模型圈的通用方言，各厂商端点格式一致。接智谱、DeepSeek 或任何兼容厂商 = 只改 base-url / model / api-key 三行配置，Java 代码零改动。AI Key 通过 `application-local.yml` 本地化（gitignore），不进仓库。

### 为什么暂不上 Redis

登录采用 **JWT 无状态设计**——服务端不保存会话，天然不依赖 Redis；当前数据量也没有热点读缓存需求。**此时引入 Redis 只会增加运维成本而没有收益**（YAGNI）。演进路线已预留：接入 Redis 容器做缓存层、分布式会话改造。

### AI 输出可信：四层防线

AI 的输出是自由文本，不可直接信任。四层防线保证入库用例可信：

1. **清洗**：截取方括号间的 JSON（剥离 markdown 围栏与客套话），找不到则整体丢弃；
2. **格式/结构校验**：必填、method 白名单、url 合法；
3. **契约校验**：字段形态与执行引擎约定一致（headers 数组、json_path 必带 path 等）——曾拦截三种真实的 AI 契约漂移（字段错位 / 形态漂移 / 记法漂移）；
4. **语义验证**（运行时）：期望值是否符合被测系统真实行为，机器入库前判不了——由执行时断言暴露、AI 归因分析、人工决策修正，形成闭环。**AI 出建议、人做决策**。

### UI 执行器：ProcessBuilder 三坑

跨进程桥接 Python 的三个工程问题及对策：①不读子进程输出流 → 缓冲区写满死锁 → **独立线程持续读流**；②用例死循环拖死线程 → **300s 超时强制杀进程**；③Windows 管道 GBK 乱码 → **PYTHONIOENCODING=utf-8**。失败现场以输出快照落库（尾部 60KB），排错零重跑。

### 报告 SQL：覆盖索引回马枪

统计 SQL 在 46 万行数据下全表扫描（EXPLAIN：type=ALL / 临时表 / filesort）。按阶段 4 的排查链定位后，建**覆盖索引** `(passed, case_id, case_name)`（WHERE+GROUP BY+SELECT 三合一），查询耗时 **0.15s → 0.027s（约 5 倍）**。边界认知：ORDER BY 聚合结果无法走索引（temporary/filesort 仍在，但排序对象降为 31 组聚合行）。

### 前端：backdrop-filter 的包含块陷阱

毛玻璃卡片（`backdrop-filter: blur`）不只是"加个模糊"——它会让元素成为 `position: fixed` 的**包含块**，内部浮层的定位参照从视口变成这个元素。本项目同一个根因连踩三次：**弹窗被裁在卡片内**（遮罩只盖住卡片）、**表格 tooltip 跑到页面角落**、**悬浮提示彻底不触发**。

修法固定两条：浮层加 `append-to-body`（Vue 原生 `<Teleport to="body">`），或不使用 backdrop-filter。同类属性还有 `filter` / `transform` / `perspective` / `will-change` / `contain`——凡在其中放 fixed 浮层都要警惕。

### 归因成本：读穿透缓存

AI 归因结果落 `t_ai_log`（`biz_type + biz_id` 联合索引），再次查看时先查历史、命中即返回：一次归因约 **5732ms + 537 token**，回看 **77ms + 0 token**。批次级回看用一次 `IN` 查询取回全部，避免 N+1。这也让"刷新页面后归因消失"的问题一并解决。

## 本地运行

**前置**：JDK 25、MySQL 8、Node 16+、Python venv（仅 UI 用例需要）

1. 新建库：按序执行 `sql/01~06` 建表脚本；已有库：按序补跑 `sql/07~08` 增量迁移（执行器类型 / 套件模块 / AI 归因 `biz_id`）；
2. 复制 `application.yml` 同级创建 `application-local.yml`，填入智谱 API Key：
   ```yaml
   zhipu:
     api-key: <你的Key>
   ```
3. 口令与密钥按需用环境变量覆盖（`DB_USERNAME` / `DB_PASSWORD` / `JWT_SECRET`），不设置则使用 `application.yml` 里的本地默认值；
4. 后端：IDEA 打开项目，运行 `AiTestOrchestratorApplication`（端口 8081）；
5. 前端：进入 `web/` 目录执行 `start-frontend.bat`（端口 5173）；
6. UI 用例：`application.yml` 中配置 `pytest.python-path`（venv 解释器）与 `work-dir`（pytest 项目根）。

## CI

GitHub Actions：push 到 main 自动构建并运行 13 个 JUnit 单元测试（见 `.github/workflows/build.yml`）。

## 已知边界

- UI 执行器依赖宿主机 Python/浏览器生态，未容器化；
- 数据库口令与 JWT 密钥使用**环境变量占位**（`${DB_USERNAME:root}` / `${DB_PASSWORD:...}` / `${JWT_SECRET:...}`），默认值仅为本地免配置启动；真实环境通过环境变量注入，不写进代码；
- 单批次内多用例串行执行（并发执行为演进项）；
- 归因结果默认命中 `t_ai_log` 历史缓存，暂无"强制重新分析"入口；
- 后台界面按桌面端设计，未做移动端适配。

## 后续规划

- [ ] 用例批量执行（后端 `caseIds` 数组已支持，前端待加多选）
- [ ] 执行中批次自动轮询（当前需手动刷新）
- [ ] 批量导入的前端入口（后端 `/api-case/import` 已就绪）
- [ ] 归因强制重算（`?force=true` 跳过缓存）
- [ ] 单批次内多用例并发执行（CompletableFuture）
- [ ] Docker Compose 编排（app + MySQL + Redis 容器组网）
- [ ] Redis 缓存层接入（演进故事落地）

## License

[MIT](LICENSE) © 2026 Wen Jiayi
