# 变更日志 (CHANGELOG)

> 本文件记录 erupt-cloud 项目的迭代变更。
> 版本号遵循 [语义化版本](https://semver.org/)。

---

## [1.5.0] - 2026-09-01

### 升级概要

ERPNext 数据模型移植 **第二批（组织与质量域）**：新增 3 个业务模块 **HR 人力资源 / Pay 薪酬管理 / Qal 质量管理**，延续 1.4.0 的「原生化建模 + 完整级实现」路线（状态机 DataProxy + 行按钮 Handler + erupt-report 报表 + erupt-print 打印模板 + 跨模块 GL 集成），不做 REST API 集成。

- 新增 3 个业务模块（`ep-module-hr` / `ep-module-pay` / `ep-module-qal`），父 pom `<modules>`/`<dependencyManagement>` 与 ep-boot 依赖同步登记；
- 跨模块集成：Pay 工资单过账复用 `FinPostingFacade`（GL 借 工资费用 = 贷 实发 + 贷 代扣），`JournalSourceType` 扩展 `SALARY`；Qal 自包含（物料快照），HR 引用自身实体；
- 总 SmokeTests 从原 124 → **现 146**，新增 22 条，全量 BUILD SUCCESS。

**数字概览**：
- Maven 模块：12 个业务模块 + ep-boot（父 pom `<modules>` 17 → 20，含原 12 + 新 3）；
- ep-boot 报表种子：18 → 21 张（+HR 员工花名册 / Pay 薪酬月度汇总 / Qal 质检合格率）；
- ep-boot 打印模板种子：9 → 12 张（+HR 员工档案 / Pay 工资单 / Qal 质检报告）；
- SmokeTests 增量：HR 6 + Pay 6 + Qal 6 + ep-boot Report 8→9(+1) + Print 8→11(+3) = 新增 22 条。

### 新增模块速览

| 模块 | 包名 | 核心实体 | 状态机 / 关键能力 | 跨模块集成 | 测试 |
|---|---|---|---|---|---|
| **HR 人力资源** | `xyz.herz.ep.hr` | HrEmployee / HrDepartment / HrDesignation / HrAttendance / HrLeaveType / HrLeaveApplication 6 实体 | 员工生命周期(入职/离职/停用行按钮) · 考勤打卡 · 请假申请工作流(申请/批准/拒绝/取消) | Pay 员工 REF / ep-boot 员工花名册报表+档案打印 | HrSmokeTests 6/6 ✅ |
| **Pay 薪酬管理** | `xyz.herz.ep.pay` | PaySalaryComponent / PaySalaryStructure / PaySalaryStructureItem / PaySalarySlip / PaySalarySlipItem 5 实体 | 工资单状态机(草稿→已提交→已过账→已取消) · 提交时从工资结构快照生成明细并派生 gross/deductions/net · 取消反向冲销 | **FinPostingFacade** 工资 GL 过账(JournalSourceType.SALARY) + HR 员工 REF | PaySmokeTests 6/6 ✅ |
| **Qal 质量管理** | `xyz.herz.ep.qal` | QalCriteria / QalInspection / QalInspectionItem / QalNonConformance / QalFeedback 5 实体 | 质检单状态机(草稿→待检→合格/不合格→已取消) · 提交时按启用参数快照读数明细 · 上下限自动判定 · 判不合格自动生成 N/C 单 · N/C 生命周期(提交→处理→关闭) | 自包含(物料编码/名称快照,免跨模块依赖) | QalSmokeTests 6/6 ✅ |

### ep-boot 报表 / 打印扩展

| 类型 | 新增 | 覆盖 | 验收测试 |
|---|---|---|---|
| erupt-report | HR 员工花名册 + Pay 薪酬月度汇总 + Qal 质检合格率 = 3 张 | `EruptReportInitializer` 21 张种子，SQL 全小写 H2 兼容 | EruptReportSmokeTest 9/9（count≥20 + 21 code 抽样 + SQL EXPLAIN） |
| erupt-print | HR 员工档案 + Pay 工资单 + Qal 质检报告 = 3 模板 | `EruptPrintInitializer` 12 张种子 + `EruptPrintRendererService.renderXxx` | EruptPrintSmokeTest 11/11（count≥12 + 12 code 抽样 + 9 模块渲染 contains 关键字段 + null 统一 IAE） |

### 关键技术决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 工资 GL 分录 | 三行：借 SALARY_EXP 总额 / 贷 SALARY_PAY 实发 / 贷 SALARY_WITHHOLD 代扣 | 借贷平衡(gross = net + deductions)，代扣款项独立科目便于对账 |
| 工资明细生成 | 提交时从工资结构快照(编码/名称/类型/金额) | 结构后续调整不影响已生成工资单，快照可追溯 |
| 质检判定 | 有上下限自动判(lower ≤ reading ≤ upper)；无上下限(目测)采信人工勾选 | 数值型消除人为误差，目测型保留检验员判断 |
| N/C 自动生成 | 质检单判不合格时按「NC-{质检单号}」幂等生成 | 质量追溯闭环，重复判定不产生重复 N/C |
| 模块独立性 | Qal 物料用编码/名称快照(免 erp 依赖)，Pay 依赖 hr/fin | 遵循最小依赖原则，与 WMS customerName 快照风格一致 |

### 测试用例概览（原 124 → 现 146）

| 模块 | 1.4.0 | 1.5.0 | 增量 |
|---|---|---|---|
| ep-module-hr | 0 | 6 | +6 |
| ep-module-pay | 0 | 6 | +6 |
| ep-module-qal | 0 | 6 | +6 |
| ep-boot（Report） | 8 | 9 | +1 |
| ep-boot（Print） | 8 | 11 | +3 |
| 其余模块 | 102 | 102 | 0 |
| **合计** | **124** | **146** | **+22** |

- 全量回归：`mvn test` → **Tests run: 146, Failures: 0, Errors: 0, Skipped: 0 · BUILD SUCCESS**。
- Reactor 17 单元全 SUCCESS（12 业务模块 + ep-boot + 父 + 中间模块）。

### 完整升级命令

```bash
# ========= macOS + Homebrew openjdk@21 =========
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# ========= 1) 刷新依赖（新增 3 业务模块）=========
mvn clean install -N            # 安装父 pom
mvn clean install -U -T 1C      # 全模块 + 强制更新 SNAPSHOT

# ========= 2) 全量回归 =========
mvn test
# 验收: 各模块 Tests run 合计 146, 0 Failures, 0 Errors

# ========= 3) 单模块测试（TDD 增量验证）=========
mvn test -pl ep-module-hr -am
mvn test -pl ep-module-pay -am
mvn test -pl ep-module-qal -am
mvn test -pl ep-boot -Dtest=EruptReportSmokeTest,EruptPrintSmokeTest -Dsurefire.failIfNoSpecifiedTests=false

# ========= 4) 启动后台 =========
mvn spring-boot:run -pl ep-boot
# 控制台: http://localhost:8080/erupt
```

---

## [1.4.0] - 2026-08-31

### 升级概要

基于 ERPNext 16 核心模块的 **数据模型移植**（用户指令「调用 ERPNext 完整功能，能实现的情况下覆盖 ERPNext 所有功能」）。以 ERPNext DocType 字段/状态机/工作流定义为蓝本，在 erupt-cloud 上做**原生化建模 + 完整级实现**（不做 REST API 集成，不做双系统并存）。首批落地 5 个新业务模块，覆盖财务会计 / 生产制造 / 项目管理 / 服务支持 / 资产管理，每模块实现到「业务级 + erupt-report 报表 + erupt-print 打印模板 + 跨模块集成接口」深度。

- 新增 5 个业务模块（`ep-module-fin` / `ep-module-mfg` / `ep-module-proj` / `ep-module-sup` / `ep-module-ast`），父 pom `<modules>` 与 ep-boot 依赖同步登记；
- 跨模块集成采用 Facade 模式（`FinPostingFacade` 对外统一过账），各模块 `@Autowired(required=false)` 可选注入，保持模块独立可测；
- 状态机统一通过 `XxxStateDataProxy` 锁定 status 字段，仅允许行按钮触发迁移；
- 总 SmokeTests 从原 85 → **现 124**，新增 39 条，全量 BUILD SUCCESS。

**数字概览**：
- Maven 模块：9 个业务模块 + ep-boot（父 pom `<modules>` 12 → 17，含原 7 + 新 5）；
- 新增 Java 源文件：5 模块（实体/Repository/Handler/Calculator/Facade/枚举/测试启动类）共 90+ 个；
- ep-boot 报表种子：7 → 18 张（+Fin×3 / Mfg×2 / Proj×2 / Sup×2 / Ast×2）；
- ep-boot 打印模板种子：3 → 9 张（+Fin×2 / Mfg / Proj / Sup / Ast）；
- SmokeTests 增量：Fin 6 + Mfg 6 + Proj 6 + Sup 6 + Ast 6 + ep-boot report 3→8(+5) + print 4→8(+4) = 新增 39 条。

### 新增模块速览

| 模块 | 包名 | 核心实体 | 状态机 / 关键能力 | 跨模块集成 | 测试 |
|---|---|---|---|---|---|
| **Fin 财务会计** | `xyz.herz.ep.fin` | FinAccount(科目树) / FinJournalEntry(凭证) / FinSalesInvoice / FinPurchaseInvoice / FinPaymentEntry / FinBudget / FinCostCenter | 科目启停 · 凭证平账+反向冲销 · 销售/采购发票提交→GL · 收付款单→GL · 预算批准 | **FinPostingFacade** 对外统一过账门面（post/cancel，借贷不平抛 IAE） | FinSmokeTests 6/6 ✅ |
| **Mfg 生产制造** | `xyz.herz.ep.mfg` | MfgWorkOrder / BOM / 生产计划 / 工序 / 领料 / 报工 等 8 实体 | 工单状态机(草稿→下达→生产→完工→停止) · BOM 展开 · 领料扣库存 | 调 ERP 库存 Facade | MfgSmokeTests 6/6 ✅ |
| **Proj 项目管理** | `xyz.herz.ep.proj` | ProjProject / Task / CashFlow / 费用 / 里程碑 等 7 实体 | 项目状态机 · 任务完工率 · 现金流(流入/流出/净流) | 项目费用过账 GL(JOURNALSourceType.PROJECT_EXPENSE) | ProjSmokeTests 6/6 ✅ |
| **Sup 服务支持** | `xyz.herz.ep.sup` | SupIssue / SupIssueAssignment / SupServiceLevelAgreement / SupKnowledgeBase / SupSupportSettings 等 6 实体 | 工单生命周期(回复/解决/关闭/重开/取消) · **SLA 引擎**(SupSlaEvaluator 多优先级时长解析) · 知识库发布归档 · 单例配置 | 工单关联 CrmCustomer(optional，快照) | SupSmokeTests 6/6 ✅ |
| **Ast 资产管理** | `xyz.herz.ep.ast` | AstAsset / AstAssetCategory / AstLocation / AstDepreciationSchedule / AstAssetMovement / AstAssetRepair 6 实体 | 资产生命周期(草稿→可用→部分折旧→完全折旧→出售/报废) · **折旧引擎**(AstDepreciationCalculator 直线法/余额递减/双倍余额递减+末期补齐消除截断误差) · 转移单同步保管人/成本中心 · 维修单状态机 | 折旧 GL(ASSET_DEPRECIATION) + 处置 GL(ASSET_DISPOSAL) 过账 fin | AstSmokeTests 6/6 ✅ |

### ep-boot 报表 / 打印扩展

| 类型 | 新增 | 覆盖 | 验收测试 |
|---|---|---|---|
| erupt-report | Fin×3（试算平衡表 / 损益表 / 应收账龄）+ Mfg×2（工单状态饼 / 工单进度表）+ Proj×2（现金流趋势 / 任务完工率）+ Sup×2（SLA 达成率 / 工单日趋势）+ Ast×2（资产折旧汇总表 / 资产状态分布饼）= 11 张 | `EruptReportInitializer` 18 张种子，SQL 全小写 H2 兼容 | EruptReportSmokeTest 8/8（count≥17 + 18 code 抽样 + fin/mfg/proj/sup/ast 5 组 SQL EXPLAIN） |
| erupt-print | Fin×2（销售/采购发票）+ Mfg（工单）+ Proj（验收单）+ Sup（工单 Ticket）+ Ast（资产卡片）= 6 模板 | `EruptPrintInitializer` 9 张种子 + `EruptPrintRendererService.renderXxx` | EruptPrintSmokeTest 8/8（count≥9 + 9 code 抽样 + 6 模块渲染 contains 关键字段 + null 统一 IAE） |

### 关键技术决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 移植方式 | 数据模型移植（非 REST 集成） | ERPNext 是 Python/Frappe，与 Java/Erupt 架构完全不同；原生化建模才能复用 erupt 状态机/行按钮/报表/打印 |
| 跨模块集成 | Facade 接口 + `@Autowired(required=false)` | 各模块独立可测，Facade 不可用时静默跳过，不阻断状态变更 |
| GL 过账 | 统一 FinPostingFacade（post/cancel） | 借贷平衡校验集中，来源可追溯（JournalSourceType），反向冲销幂等 |
| 折旧精度 | 末期补齐（remaining = 可折旧基数 - 累计折旧） | 消除直线法每期 833.33×12=9999.96 的截断误差，确保累计折旧精确等于可折旧基数 |
| SLA 计算 | SupSlaEvaluator 多级 fallback | 工单指定 SLA → 默认 SLA → 优先级表 → 硬编码，确保各种配置下均有合理截止时间 |
| 单例配置 | ApplicationReadyEvent 幂等插入 | SupSupportSettings / AstAssetCategory 等启动时插入默认值，业务方 `findFirstByOrderByIdAsc` 获取唯一配置 |

### 测试用例概览（原 85 → 现 124）

| 模块 | 1.3.0 | 1.4.0 | 增量 |
|---|---|---|---|
| ep-module-fin | 0 | 6 | +6 |
| ep-module-mfg | 0 | 6 | +6 |
| ep-module-proj | 0 | 6 | +6 |
| ep-module-sup | 0 | 6 | +6 |
| ep-module-ast | 0 | 6 | +6 |
| ep-boot（Report） | 3 | 8 | +5 |
| ep-boot（Print） | 4 | 8 | +4 |
| 其余模块 | 78 | 78 | 0 |
| **合计** | **85** | **124** | **+39** |

- 全量回归：`mvn test` → **Tests run: 124, Failures: 0, Errors: 0, Skipped: 0 · BUILD SUCCESS**。
- Reactor 14 模块全 SUCCESS（7 原业务 + 5 新业务 + ep-boot + 父）。

### 完整升级命令

```bash
# ========= macOS + Homebrew openjdk@21 =========
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
java -version   # openjdk version "21.0.12"

# ========= 1) 刷新依赖（新增 5 业务模块）=========
mvn clean install -N            # 安装父 pom
mvn clean install -U -T 1C      # 全模块 + 强制更新 SNAPSHOT

# ========= 2) 编译 =========
mvn clean compile -T 1C

# ========= 3) 全量回归 =========
mvn test
# 验收: grep -c "BUILD SUCCESS" 应 =1
# 各模块 Tests run 合计 124, 0 Failures, 0 Errors

# ========= 4) 单模块测试（TDD 增量验证）=========
mvn test -pl ep-module-ast -Dtest=AstSmokeTests -Dsurefire.failIfNoSpecifiedTests=false
mvn test -pl ep-boot -Dtest=EruptReportSmokeTest,EruptPrintSmokeTest -Dsurefire.failIfNoSpecifiedTests=false

# ========= 5) 启动后台 =========
mvn spring-boot:run -pl ep-boot
# 控制台: http://localhost:8080/erupt
```

---

## [1.3.0] - 2026-08-31

### 升级概要

基于 Erupt **2.0.3 → 2.1.0** 全面升级与重优化（本版本核心依据用户指令「先升级到最新版，然后原来的功能都基于新版本重新优化」）。在 1.2.0 基线（63 个 SmokeTests）100% 通过不回归的前提下，落地 Erupt 2.0.4/2.1.0 全部关键新特性：

- 5 类基础注解增强 + 4 只跨模块 BUTTON Handler（编辑页行内操作）；
- erupt-report 开源（原 erupt-bi）+ erupt-print 模板化打印接入；
- UPMS 密码哈希 SHA→PBKDF2 + erupt-jpa 更名为 erupt-data-jpa 的破坏性变更文档化；
- 总 SmokeTests 从原 63 → **现 85**，新增 22 条，全量 BUILD SUCCESS 耗时 32.828 s（≤ 基线 30s×120%=36s）。

**数字概览**：
- 9 个 Maven 模块（父 + 7 业务 + ep-boot）保持不变；
- pom.xml：erupt.version=2.1.0；artifact 重命名 erupt-jpa → erupt-data-jpa；新增 erupt-report(2.1.0) + erupt-print(2.1.0) 依赖；
- 新增 Java 源文件：4 个 BUTTON Handler + Report 3件套（Entity/Repo/Initializer）+ Print 4件套（Entity/Repo/Initializer/RendererService）共 11 个；
- 修改实体注解：16 个 Java 文件（PASSWORD×2/PROGRESS×2/DragSort×3/collapse×4/copy×5/BUTTON×4实体×10字段×价格字段1）；
- SmokeTests 测试增量：Task 2 2 + Task 3 3 + Task 4 4 + Task 5A 1 + Task 5B 1 + Task 6A 1 + Task 6B 1 + Task 7 3 + Task 8 4 = 新增 20 条（加其他 context 测试达到总 85-63=22 条，与 1.2.0 版本基线一致口径）。

### 破坏性变更（2 项）

1. **erupt-jpa → erupt-data-jpa 重命名**
   - 背景：Erupt 2.1.0 正式不再发布 `erupt-jpa` 2.1.0 版本（已停更），artifact 改名为 `erupt-data-jpa:2.1.0`（JPA 能力完整保留）。
   - 影响范围：父 pom.xml 的 dependencyManagement + 8 个模块的 pom.xml（7 业务 + ep-boot）共 8 处 `<artifactId>` 变更；
   - 升级指引：
     ```bash
     # macOS + JDK21（所有 mvn 命令必须前置 JAVA_HOME 到 JDK21）
     export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
     export PATH=$JAVA_HOME/bin:$PATH
     cd /path/to/erupt01
     # 改完 8 个 pom 的 erupt-jpa → erupt-data-jpa 后强制刷新依赖
     mvn clean install -pl ep-boot -am -U
     mvn clean compile -T 1C
     ```
   - 本次变更已在 8 个 pom 全部完成，9 个模块 compile OK。

2. **UPMS 密码哈希：SHA-512 → PBKDF2（Spring Security 默认）**
   - 背景：Erupt 2.1.0 UPMS 用户密码哈希算法升级，避免历史 SHA 的理论碰撞风险。
   - 影响范围：若生产环境 UPMS `e_upms_user` 表已有历史用户，直接启动后老用户将无法匹配新算法登录；
   - **生产迁移伪代码/建议方案（不自动执行，只做 CHANGELOG 指引）**：
     ```sql
     -- ① 打标: 标识出仍为 SHA 哈希的用户 (password 以 '{sha-512}' 或特定前缀开头, 需按实际部署确认)
     -- UPDATE e_upms_user SET algo='SHA_LEGACY' WHERE algo IS NULL AND password IS NOT NULL;

     -- ② 首次登录 rehash（建议在业务代码加 ApplicationReadyEvent 监听器 + 登录 filter）：
     --    用户登录时先 try PBKDF2 matches → 失败再 try SHA legacy → 成功就 PasswordEncoder(PBKDF2).encode(raw) 覆盖写回
     --    同时把 algo 更新为 'PBKDF2'；全部 SHA 用户登录过一遍就完成渐进迁移
     -- ③ 紧急回滚：若 SHA→PBKDF2 期间问题,临时切回 legacy encoder 可快速恢复业务登录
     ```
   - 本项目（测试环境 H2 create-drop）不生产 UPMS 用户数据，因此测试不影响；生产部署务必按 ② 做迁移。

### 优化要点（≥8 项，对应 tasks.md 5 注解 + 4 Handler + 2 集成，共 11 子项）

| # | 项 | 覆盖 | 文件/方法 | 验收 |
|---|---|---|---|---|
| 1 | EditType.PASSWORD 敏感掩码 | MpAccount.appSecret / MpAccount.token / IotDevice.deviceSecret 共 2 类 3 字段 | `MpAccount.java:39-51` / `IotDevice.java:77-82` | MpSmokeTests.password_field_assertions 等 2 个单测 |
| 2 | ViewType.PROGRESS 进度条（@Transient 计算） | CrmReceivablePlan.receivedProgress + WmsStockCheck.checkProgress 2 字段 | `CrmReceivablePlan.java:94-106` + getReceivedProgress（divide HALF_UP 2,除零空安全）/ `WmsStockCheck.java:84-99` 遍历 items | CrmSmokeTests progress 计算 + WmsSmokeTests progress 计算 |
| 3 | @Erupt.dragSort=@DragSort(field="sort") 拖拽排序 | MpMenu.sort（已存在）/ LandingTemplate 新增 sort 字段 + set0 / MallProductBrand.sort（已存在）共 3 实体 | `MpMenu.java:41` / `LandingTemplate.java:29-31 + sort Integer=0 新增` / `MallProductBrand.java:24-26` | 3 类 3 个测试通过排序断言 |
| 4 | @Layout(collapseActionButton=true) 折叠行按钮(≥3 折叠下拉) | MallTradeOrder(4按钮) / MallTradeAfterSale(5) / IotAlarm(3) / IotDevice(3) 共 4 类 | `MallTradeOrder.java:44` / `MallTradeAfterSale.java:41` / `IotAlarm.java:40` / `IotDevice.java:39` + import Layout | 4 类反射注解存在 + 4 后端复制等价测试 Task4 |
| 5 | @Power(copy=true) 复制行 + 后端等价 | MallProductSpu / CrmCustomer / LandingTemplate / WmsWarehouse / IotProduct 共 5 类 | 5 实体对应 @Erupt power 属性 + 4 类后端 setId(null)+save → id 新生+关键字段保留 | Task 4 TR-4.2 5/5 断言 |
| 6 | BUTTON Handler 1/4 — CRM CrmContractAutoPlan | 3 个 @Transient BUTTON 辅助字段：期数/起始日/间隔月，点击自动生成 N 条回款计划 | `CrmContract.java:L67-L190` / `CrmContractAutoPlanButtonHandler.java` / Handler.exec(periods,start,interval,contract)（IAE/ISE 8 个边界）| TR-5A.1 3000/3期/2026-01-01 interval=1 → 3 plans 1000.00 +0/+1/+2月；TR-5A.2 IAE+ISE |
| 7 | BUTTON Handler 2/4 — Mall MallSpuAutoSku | 1 持久化基准价 price + 2 BUTTON 辅助字段 skuCount(3)/deltas(10,-10,0)，点击批量生成 N 个 SKU | `MallProductSpu.java:L83-L130` / `MallSpuAutoSkuButtonHandler.java` / deltas split 严格 skuCount.length 匹配 | TR-5B.1 基准价 100 / deltas "10,-10,0" → 3 SKU 110/90/100，code "-SKU-1/2/3" specs "10%/-10%/0%"，stock 0；ISE/IAE 边界 3 类 |
| 8 | BUTTON Handler 3/4 — IoT IotAlarmRuleValidate | 1 BUTTON 字段 validateSample（警告⚠️风格按钮），阈值型规则 X ≥ T+MARGIN(5) 命中 | `IotAlarmRule.java:L82-L101` / `IotAlarmRuleValidateButtonHandler.java` MARGIN=BigDecimal("5")，非 threshold→UOE | TR-6A.1 T=30 样例 35/37→命中 / 28/25→不命中；sample null→IAE / threshold null→ISE / state→UOE |
| 9 | BUTTON Handler 4/4 — WMS WmsMoveRecommend | 1 BUTTON 字段 runRecommendQtyTrigger，根据每条明细 fromLocationId+skuCode 查源库位可用库存，qty=min(avail,req) clamp 并级联 save | `WmsStockMoveOrder.java:L67-L82` / `WmsMoveRecommendButtonHandler.java` @Transactional findByLocationSku(加锁) + Math.min | TR-6B.1 3明细(LOC-A SKUA 100req150→100 / LOC-A SKUB req20→20 / LOC-B SKUA req10→0) HashMap 聚合断言；fromLocationId=null→IAE |
| 10 | erupt-report 开源接入 7 报表×6 模块 | Initializer + Entity + Repository；7 报表覆盖 CRM/ERP/Mall×2/WMS/IoT/Landing | `EruptReportEntity.java` / `EruptReportInitializer.java` 7 SQL 全部 H2 小写标识符（DATABASE_TO_UPPER=FALSE）+ level 用双引号 | 3 测试 Initializer 存在 + count≥7 且 6 code 抽样通过 + 3 SQL H2 EXPLAIN 无语法错通过 |
| 11 | erupt-print 模板化打印 3 模板 | 3 模板 CRM合同/ERP采购单/WMS出库通知 + Renderer（POJO → HTML）；类名加 Renderer 避免与 erupt-print 原生 EruptPrintService Bean 名冲突 | `EruptPrintTemplate*` + `EruptPrintRendererService` 3 render 方法 + null 统一 IAE | 4 测试 Initializer存在 + count≥3+code抽样 + 3 渲染 contains 关键字段 ≥2 每类 + null IAE 3 个一致行为 |

### 测试用例概览（原 63 → 现 85）

- 基线(1.2.0)：原 63 SmokeTests，`/tmp/ep-210-task1-test.log` 全过；
- Task2/3/4 中间基线 2026-08-29：70/70 0F 0E 31.233 s（`/tmp/ep-210-task23-mid.log`）；
- Task5A/5B/6A/6B/7/8 新增并 GREEN 后本次最终：
  - CRM 11 / ERP 6 / Mall 7 / WMS 10 / MP 7 / IoT 8 / Landing 9 / ep-boot 27（4 Print + 19 SimpleEntity + 3 Report + 1 ContextLoads）
  - **合计 85 Tests / 0 Failures / 0 Errors / BUILD SUCCESS / 32.828 s**（`/tmp/ep-210-final.log`）。
  - AC-7 要求：Tests ≥ 83 ✓；耗时 ≤ 36 s ✓。

### 完整升级命令

```bash
# ========= macOS + Homebrew openjdk@21 21.0.12 =========
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
java -version   # openjdk version "21.0.12"

# ========= 1) 刷新依赖（erupt-jpa→data-jpa + erupt-report/print 新依赖）=========
mvn clean install -N            # 安装父 pom 到本地 m2
mvn clean install -U -T 1C      # 全模块 + 强制更新 SNAPSHOT / 新 jar

# ========= 2) 编译（mvn 必须 -am 才能从父聚合构建依赖树）=========
mvn clean compile -T 1C -pl ep-boot -am

# ========= 3) 全量回归（TDD 最终验收命令）=========
mvn clean test > /tmp/ep-210-final.log 2>&1
# 验收点: grep -c "BUILD SUCCESS" 应 =1；
# grep "Tests run:" | tail -1 → 应为 Tests run: X, Failures: 0, Errors: 0 (X=85)
# tail 看 Total time: 32.828 s < 36s

# ========= 4) 本地启动后台（erupt 控制台 UI 默认 /erupt 根路径）=========
mvn spring-boot:run -pl ep-boot
# 控制台: http://localhost:8080/erupt  若改端口则 application.yml server.port
```

---

## [1.2.0] - 2026-08-20

### 概要

新增第 7 个业务模块 `ep-module-landing`，基于 amis 实现可视化拖拽 H5 落地页 + 短链接 + magic-api 接口 + 留资闭环。利用 amis-editor 现成 SDK 嵌入 erupt-tpl/Thymeleaf，无需独立前端工程，4 天闭环。

**数字概览**：9 个 Maven 模块 · 265 个 Java 源文件 · 15 个测试类 · 63 个测试用例全部通过 · 20 个文件变更 · +1320 行代码

### 新增模块：ep-module-landing

| 包 | 核心类 | 说明 |
|---|---|---|
| `landing.enums` | LandingDictEnums | PageStatus(草稿/发布/下线) + TemplateCategory + LeadSource |
| `landing.core` | LandingStateDataProxy / LandingEnumChoiceFetchHandler | 状态机锁 + 下拉处理器(照搬 mall 模式) |
| `landing.entity` | LandingPage / LandingTemplate / LandingLead / LandingAccessLog | 4 个实体,UV 去重唯一约束 |
| `landing.jpa` | 4 个 Repository | 含 PV/UV 原子自增 @Modifying 查询 |
| `landing.handler` | LandingPublishHandler / LandingOfflineHandler | 草稿→发布(生成短码) / 发布→下线 |
| `landing.shorturl` | ShortCodeGenerator / ShortUrlController | Base62(id+1M偏移,6位) + /l/{code} 重定向 + PV/UV |
| `landing.web` | LandingRenderController / LandingApiController | /p/{slug} H5 渲染 + /api/landing/* 兜底 API |
| `landing.config` | LandingTemplateInitializer | 启动时插入 3 个预设 amis 模板 |

### 关键技术决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 短码表设计 | 合并到 LandingPage | 1:1 关系,独立表无收益 |
| 短码生成 | Base62(自增ID + 1M偏移),6 位 | 无碰撞、可逆、零查询 |
| magic-api 脚本 | jar 内打包 + maven-resources-plugin 复制 | 脚本版本化入仓 |
| 公开接口 | Spring MVC 兜底 API(/api/landing/*) | magic-api 首选,Spring MVC 兜底确保开箱即用 |
| 编辑器入口 | /static/landing-editor.html | amis-editor CDN 加载,无需 npm 工程 |
| H5 渲染 | 普通 @Controller + Thymeleaf | EruptSecurityInterceptor 只拦 @EruptRouter,C 端免登 |
| amis SDK | CDN 兜底 | 避免 50MB SDK 入库,离线部署时再下载 |

### magic-api 接口(6 个)

- `GET /api/landing/page/slug/{slug}` — 按 slug 取已发布页面(公开)
- `GET /api/landing/template/list` — 模板列表(公开)
- `POST /api/landing/lead` — 留资提交(公开)
- `GET /api/landing/page/{id}` — 按 ID 取页面(管理)
- `POST /api/landing/page` — 保存页面 JSON(管理,待 magic-api 实现)
- `GET /api/landing/stats/{pageId}` — 访问统计(管理,待 magic-api 实现)

### P1 完成（magic-api 白名单 + amis SDK 本地化）

- **magic-api 公开接口白名单**：公开接口走 Spring MVC 兜底 API（`/api/landing/*`，免登），magic-api 仅管理端需登录。避免覆盖 EruptMagicAPIRequestInterceptor 破坏 magic-api web UI 鉴权。
- **amis SDK 本地化**：amis 6.13.0 运行时 SDK（sdk.js 2MB + css 共 8MB）已下载到 `static/amis/`，H5 渲染页离线可用。编辑器仍用 CDN（React bundle 50MB 不入库），提供 `static/download-amis-editor.sh` 离线下载脚本。

### 测试用例(8 个)

- template_crud / page_draft_save_and_load / page_state_machine(发布/下线/复用短码)
- short_code_deterministic_unique_reversible(确定性+唯一性+可逆)
- lead_submit_persisted / uv_dedup / state_proxy_blocks_direct_status_edit / publish_rejected

---

## [1.1.0] - 2026-08-12

### 概要

本次迭代完成了全量 P0 + P1 业务模块的 MVP 交付，新增 4 个业务模块（Mall / WMS / MP / IoT），完成 CRM 和 ERP 的 P1 扩展功能，建立跨模块 Facade 接口体系，并将工程升级到 JDK 21 + Lombok + 模块独立测试架构。

**数字概览**：8 个 Maven 模块 · 248 个 Java 源文件 · 14 个测试类 · 55 个测试用例全部通过 · 210 个文件变更 · +10527 行代码

---

### 工程基础设施变更

#### JDK 17 → 21
- 父 pom `<java.version>` 从 17 升级到 21
- maven-compiler-plugin `<release>21</release>`
- 通过 Homebrew 安装 `openjdk@21`，编译/测试全量验证通过

#### Lombok 简化
- CRM 模块 8 个实体类（CrmClue / CrmCustomer / CrmContact / CrmBusiness / CrmBusinessStatus / CrmBusinessStatusType / CrmCustomerPoolConfig / CrmFollowUpRecord）添加 `@Getter @Setter`，删除全部手写 getter/setter
- ep-module-crm/pom.xml 新增 `lombok` 依赖（provided scope）

#### 移除 ep-business-common 模块
- 删除整个 `ep-business-common` 模块（原 8 个源文件 + pom.xml）
- `InventoryChangeFacade` + `InventoryShortageException` 迁移到 `ep-module-erp/xyz/herz/ep/erp/inventory/`
- `MallStockFacade` 迁移到 `ep-module-mall/xyz/herz/ep/mall/facade/`
- 删除未使用的 `WmsOperationFacade` / `WeChatApiFacade` / `MqttDeviceFacade` / `SpringContextHolder`
- 9 个 Java 文件更新 import 语句（5 ERP + 4 Mall）
- 7 个 pom.xml 移除 ep-business-common 依赖（父 pom + 6 个业务模块）
- 各业务模块完全自包含，无公共模块依赖

#### 文档精简
- [README.md](./README.md)：466 → 327 行，删除人天估算、难度评级、版本记录、代办章节
- [todo.md](./todo.md)：精简列名、补全路径、合并重复项，新增 §8 测试设计说明

#### 单测迁移
- CRM / ERP 冒烟测试从 `ep-boot/src/test` 迁移到各自模块 `src/test`
- 每个模块新增独立的 `XxxTestApplication` 启动类（扫描本模块包）
- 每个模块新增独立的 `application.yml`（H2 内存库，`ddl-auto=create-drop`）
- 关键修复：
  - `webEnvironment = MOCK` — 解决 erupt-jpa `i18nTranslate` 需要 `HttpServletRequest` 的问题
  - 新增 `erupt-upms` test scope 依赖 — 解决 `EruptSecurityInterceptor` 依赖 `EruptUserService` 的 ClassNotFoundException
- ep-boot 保留 `EruptBusinessApplicationTests` + `SimpleEntityTest`（boot 层上下文加载测试）

#### 父 POM 变更
- `<modules>` 新增 4 个模块：`ep-module-mall` / `ep-module-wms` / `ep-module-mp` / `ep-module-iot`
- `<modules>` 移除 `ep-business-common`
- `<dependencyManagement>` 新增 4 个内部模块坐标 + `erupt-upms` 版本管理，移除 ep-business-common 坐标
- ep-boot/pom.xml 新增 4 个业务模块依赖

---

### 新增模块

#### 1. Mall 电商商城模块 (`ep-module-mall`)

**包名**：`xyz.herz.ep.mall`  
**测试**：4/4 ✅  
**文件数**：32

| 分类 | 文件 | 说明 |
|------|------|------|
| **枚举** | `MallDictEnums` | OrderStatus(5态) / AfterSaleStatus(7态) / AfterSaleType(3种) / PayStatus(3态) / EnableStatus / ListingStatus |
| **Core** | `MallEnumChoiceFetchHandler` | 字典下拉处理器 |
| | `MallStateDataProxy` | 状态机基类，锁定 status 字段 |
| **商品实体** | `MallProductCategory` | @Tree 树形分类 |
| | `MallProductBrand` | 品牌 |
| | `MallProductSpu` | SPU（含 @OneToMany SKU 子表，TAB_TABLE_ADD） |
| | `MallProductSku` | SKU（stock 只读） |
| **交易实体** | `MallTradeOrder` | 订单 + 4 个 @RowOperation（付款/发货/确认收货/取消） |
| | `MallTradeOrderItem` | 订单明细 |
| **售后实体** | `MallTradeAfterSale` | 售后单 + 5 个 @RowOperation（同意/拒绝/买家发货/卖家收货/完成） |
| | `MallTradeAfterSaleLog` | 售后日志 |
| **支付实体** | `MallPayOrder` | 支付流水 |
| **Handler** | `MallOrderPayHandler` | 0→10 待发货，生成已支付 PayOrder，调 Facade 锁库存 |
| | `MallOrderShipHandler` | 10→20 待收货，调 Facade 扣库存 |
| | `MallOrderConfirmHandler` | 20→30 已完成 |
| | `MallOrderCancelHandler` | 0/10→40 已取消，调 Facade 解锁库存 |
| | `MallAfterSaleAgreeHandler` | 10→20 同意 |
| | `MallAfterSaleRejectHandler` | 10→70 拒绝 |
| | `MallAfterSaleBuyerShipHandler` | 20→30 买家发货 |
| | `MallAfterSaleSellerReceiveHandler` | 30→40 卖家收货 |
| | `MallAfterSaleCompleteHandler` | 40/50→60 完成，调 Facade 退库存 |

**状态机**：
```
订单: 0待付款 →(付款)→ 10待发货 →(发货)→ 20待收货 →(确认)→ 30已完成
                                    →(取消)→ 40已取消
售后: 10申请 →(同意)→ 20 →(买家发货)→ 30 →(卖家收货)→ 40 →(完成)→ 60完成
              →(拒绝)→ 70拒绝
支付: 0待支付 → 10已支付 → 20已退款
```

---

#### 2. WMS 仓储管理模块 (`ep-module-wms`)

**包名**：`xyz.herz.ep.wms`  
**测试**：7/7 ✅  
**文件数**：42

| 分类 | 文件 | 说明 |
|------|------|------|
| **枚举** | `WmsDictEnums` | AsnStatus / ReceiptStatus / PutawayStatus / ShipmentNoticeStatus / PickStatus / LocationStatus / EnableStatus / StockMoveOrderStatus / StockCheckStatus |
| **Core** | `WmsEnumChoiceFetchHandler` / `WmsStateDataProxy` | |
| **基础实体** | `WmsWarehouse` | 仓库 |
| | `WmsZone` | 库区（ManyToOne→Warehouse） |
| | `WmsLocation` | 库位（ManyToOne→Zone，四态：空闲/有货/锁定/盘点中） |
| **四态库存** | `WmsStock` | available / locked / inTransit / frozen 四态余额 |
| | `WmsStockMove` | 库存流水（append-only） |
| **入库三单** | `WmsAsn` + `WmsAsnItem` | ASN 入库通知（新建→部分收货→已收货→关闭） |
| | `WmsReceipt` + `WmsReceiptItem` | 收货单（新建→收货中→已完成） |
| | `WmsPutaway` + `WmsPutawayItem` | 上架单（新建→上架中→已完成） |
| **出库两单** | `WmsShipmentNotice` + `WmsShipmentItem` | 出库通知（新建→部分拣货→已拣货→关闭） |
| | `WmsPick` + `WmsPickItem` | 拣货单（新建→拣货中→已完成） |
| **移库/盘点** | `WmsStockMoveOrder` + `WmsStockMoveOrderItem` | 移库作业（新建→移库中→已完成） |
| | `WmsStockCheck` + `WmsStockCheckItem` | 盘点单（新建→盘点中→完成/有差异） |
| **Handler** | `WmsReceiptCompleteHandler` | 收货完成，回写 ASN receivedQty |
| | `WmsPutawayCompleteHandler` | 上架完成，WmsStock.availableQty += |
| | `WmsPickCompleteHandler` | 拣货完成，availableQty -= / lockedQty -=，回写 Notice |
| | `WmsAsnCloseHandler` / `WmsShipmentNoticeCloseHandler` | 关闭单据 |
| | `WmsStockMoveCompleteHandler` | 移库完成，写 MOVE 流水 |
| | `WmsStockCheckStartHandler` | 开始盘点，回写 bookQty |
| | `WmsStockCheckFinishHandler` | 完成盘点，计算 diffQty，有差异→status=30 |

---

#### 3. MP 微信公众号模块 (`ep-module-mp`)

**包名**：`xyz.herz.ep.mp`  
**测试**：5/5 ✅  
**文件数**：24

| 分类 | 文件 | 说明 |
|------|------|------|
| **枚举** | `MpDictEnums` | SubscribeStatus / MenuStatus / MaterialType / ReplyType / ReplyContentType / EnableStatus / MessageType / MessageDirection |
| **实体** | `MpAccount` | 公众号账号（appId/appSecret/token） |
| | `MpUser` | 粉丝（openId/nickname/关注状态机） |
| | `MpUserTag` | 粉丝标签 |
| | `MpMessage` | 消息记录（方向/类型/内容） |
| | `MpAutoReply` | 自动回复（关注/关键字/默认 + 启用/禁用） |
| | `MpMenu` | 自定义菜单（树形 + 草稿/发布状态机） |
| | `MpMaterial` | 素材（图片/语音/视频/缩略图） |
| **Handler** | `MpMenuPublishHandler` | 菜单发布 0→1 |
| | `MpMenuRevokeHandler` | 菜单撤回 1→0 |
| | `MpReplyToggleHandler` | 自动回复启用/禁用切换 |

---

#### 4. IoT 物联网模块 (`ep-module-iot`)

**包名**：`xyz.herz.ep.iot`  
**测试**：5/5 ✅  
**文件数**：30

| 分类 | 文件 | 说明 |
|------|------|------|
| **枚举** | `IotDictEnums` | DeviceStatus / ThingModelType / DataType / AccessMode / NodeType / NetType / AlarmLevel / AlarmStatus / EnableStatus |
| **实体** | `IotProductCategory` | 产品分类（@Tree） |
| | `IotProduct` | 产品（节点类型/网络类型 + 启停） |
| | `IotThingModel` | 物模型（属性/服务/事件 + 数据类型 + 读写模式） |
| | `IotDevice` | 设备（状态机：未激活→离线↔在线→禁用） |
| | `IotDeviceMessage` | 设备消息（上行/下行） |
| | `IotAlarmRule` | 告警规则（阈值/状态） |
| | `IotAlarm` | 告警（四态：待处理→处理中→已解决/已忽略） |
| | `IotAlarmLog` | 告警日志 |
| **Handler** | `IotDeviceActivateHandler` | 激活 0→2，生成 deviceSecret |
| | `IotDeviceEnableHandler` | 启用 3→2 |
| | `IotDeviceDisableHandler` | 禁用 1/2→3 |
| | `IotAlarmProcessHandler` | 处理 0→10，写日志 |
| | `IotAlarmResolveHandler` | 解决 10→20，写日志 |
| | `IotAlarmIgnoreHandler` | 忽略 0/10→30，写日志 |
| | `IotProductToggleHandler` | 产品启停 |

---

### CRM 模块 P1 扩展

#### 数据权限 (todo 1.8)
| 文件 | 说明 |
|------|------|
| `CrmTeamMember` | 团队成员实体（bizType + bizId + userId + role + level） |
| `CrmTeamMemberRepository` | `findByBizTypeAndBizId` 查询 |
| `CrmTeamMemberAddHandler` | 添加成员行按钮 |
| `CrmDictEnums` 新增 | `TeamRole`(1负责人/2跟进人/3只读) |

#### 合同 + 回款 (todo 1.9)
| 文件 | 说明 |
|------|------|
| `CrmContract` | 合同实体（草稿→生效→作废 + @RowOperation EFFECT/VOID） |
| `CrmReceivablePlan` | 回款计划（待回款→部分回款→已回款） |
| `CrmReceivableRecord` | 回款记录 |
| `CrmContractEffectHandler` | 草稿(0)→生效(1) |
| `CrmContractVoidHandler` | 草稿/生效(0/1)→作废(2) |
| `CrmReceivableConfirmHandler` | 确认回款，累加 receivedAmount，刷新 plan.status |
| `CrmDictEnums` 新增 | `ContractStatus` / `ReceivableStatus` |

**测试新增**：`team_member_crud` + `contract_and_receivable`（9 步校验：合同创建→生效→回款计划→部分回款→已回款→超额拒绝→作废→重复作废拒绝）

---

### ERP 模块 P1 扩展

#### 其他入库/出库 (todo 2.6)
| 文件 | 说明 |
|------|------|
| `ErpStockIn` + `ErpStockInItem` | 其他入库单（盘盈/调拨入/其他 + 审核/反审/关闭） |
| `ErpStockOut` + `ErpStockOutItem` | 其他出库单（盘亏/调拨出/其他） |
| `ErpStockInAuditHandler` | 审核 0→1，调 InventoryChangeFacade 入库+；反审 1→0 冲销 |
| `ErpStockOutAuditHandler` | 审核 0→1，调 InventoryChangeFacade 出库-；反审 1→0 回补 |
| `ErpStockDocCloseHandler` | 关闭 0/1→2 |
| `ErpDictEnums` 新增 | `StockInBizType` / `StockOutBizType` / `StockDocStatus` |

**设计要点**：反审使用反向 StockBizType + 数量取正/取负通过 ErpStockService 方向校验；反审 bizId 加 10_000_000_000 偏移避免幂等键冲突。

#### 收付款单 + 核销 (todo 2.7)
| 文件 | 说明 |
|------|------|
| `ErpFinancePayment` | 付款单（supplier + account + totalAmount + paidAmount + 草稿→已确认） |
| `ErpFinanceReceipt` | 收款单（customer 对称结构） |
| `ErpFinanceSettlement` | 核销记录（bizType + docId + targetBizType + targetId + amount 快照） |
| `ErpPaymentConfirmHandler` | 付款确认 0→1 |
| `ErpReceiptConfirmHandler` | 收款确认 0→1 |
| `ErpDictEnums` 新增 | `FinanceDocStatus` |

**测试新增**：`stock_in_out_audit`（入库审核→库存+50→反审归零→出库审核→反审回补）+ `finance_payment_receipt`（付款/收款确认 + 2 条核销 + 双向查询校验）

---

### 跨模块 Facade 接口

各模块自包含，Facade 接口内聚到消费方模块：

| 接口 | 所在模块 | 方法 | 说明 |
|------|----------|------|------|
| `MallStockFacade` | ep-module-mall (`mall.facade`) | `lockStock` / `unlockStock` / `deductStock` / `returnStock` | Mall → ERP 库存调用，Handler `@Autowired(required=false)` 可选注入 |
| `InventoryChangeFacade` | ep-module-erp (`erp.inventory`) | `changeStock` | ERP 内部库存变更门面，采购/销售/其他出入库 Handler 调用 |

**Mall 模块接入**：4 个订单/售后 Handler 通过 `@Autowired(required = false)` 可选注入 `MallStockFacade`，Facade 不可用时 log.debug 跳过，不影响模块独立测试。

---

### 测试体系

#### 架构

```
ep-module-xxx/
  src/test/java/xyz/herz/ep/xxx/
    XxxTestApplication.java     ← 模块独立测试启动类
    XxxSmokeTests.java          ← 冒烟测试
  src/test/resources/
    application.yml             ← H2 内存库
```

#### 设计要点

| 要点 | 说明 |
|------|------|
| 独立启动类 | 每个模块 `@SpringBootApplication` 只扫描本模块包 |
| Web 环境 | `webEnvironment = MOCK` — erupt-jpa i18nTranslate 需要 HttpServletRequest |
| erupt-upms | test scope 引入，解决 erupt-security 的 EruptSecurityInterceptor 依赖 |
| 数据隔离 | H2 内存库 `ddl-auto=create-drop`，`@Transactional + @Rollback` 自动回滚 |
| JDK | 全工程 JDK 21，`<release>21</release>` |
| 验证模式 | 直接注入 Repository + Handler，调用 exec() 模拟行按钮，断言状态变更 + 副作用 |

#### 测试结果

| 模块 | 测试数 | 状态 |
|------|--------|------|
| CRM | 8 | ✅ |
| ERP | 6 | ✅ |
| Mall | 4 | ✅ |
| WMS | 7 | ✅ |
| MP | 5 | ✅ |
| IoT | 5 | ✅ |
| Boot | 20 | ✅ |
| **合计** | **55** | **ALL PASS** |

---

### 待办项状态总览

| 状态 | 数量 | 说明 |
|------|------|------|
| ✅ 已完成 | 38 项 | P0 全部 + P1 大部分 |
| 🔵 接口预留 | 2 项 | 5.8 微信 API、6.7 MQTT — Facade 接口已定义，待具体实现 |
| ⚪ 后续迭代 | 2 项 | 1.10 CRM 仪表盘（需 erupt-tpl）、1.11 导入导出去重（需 erupt-excel） |

---

### Git 提交记录

| Commit | 说明 |
|--------|------|
| `9de6591` | feat: P0 CRM + ERP MVP full rollout（首次提交） |
| `77465a1` | Create README.md |
| `73112b6` | feat: complete all P1 modules + cross-module Facade + JDK 21 + Lombok + test migration |

---

### 升级 & 迁移指南

#### JDK 21 环境配置
```bash
# 安装 JDK 21
brew install openjdk@21

# 设置 JAVA_HOME（终端会话）
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# 验证
java -version
# openjdk version "21.x.x"
```

#### 全量编译 & 测试
```bash
# 编译（跳过测试）
mvn clean install -DskipTests

# 全量测试
mvn test

# 单模块测试
mvn test -pl ep-module-crm
mvn test -pl ep-module-erp
mvn test -pl ep-module-mall
mvn test -pl ep-module-wms
mvn test -pl ep-module-mp
mvn test -pl ep-module-iot
```

#### 打包
```bash
mvn clean package -DskipTests
# 产物: ep-boot/target/ep-business.jar
```

---

## [1.0.0] - 2026-08-11

### 初始发布

- Maven 多模块架构：ep-business-parent + ep-module-crm + ep-module-erp + ep-boot（后移除 ep-business-common）
- CRM P0 MVP：线索/客户/商机/跟进状态机 + 公海回收定时任务（6 个测试）
- ERP P0 MVP：主数据 + 产品档案 + 库存 Facade + 采购/销售单据全链路状态机（4 个测试）
- 包名：`xyz.herz.ep`
- 最终打包产物：`ep-boot/target/ep-business.jar`
