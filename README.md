# erupt-plus

基于 [Erupt](https://github.com/erupt-project/erupt) 注解驱动的企业级 ERP 业务系统，Java 21 + Spring Boot 3.5 构建，零前端代码实现完整的业务状态流转。

移植自 ERPNext 16 核心业务模块，以原生化建模 + 完整状态机为设计目标，覆盖客户关系、企业资源、商城、仓储、财务、制造、采购、库存、销售等 18 个业务域。

## 特性

- **注解驱动** — 通过 `@Erupt` 注解定义实体与字段，自动生成后台管理界面，无需编写前端代码
- **状态机驱动** — 所有核心实体采用统一的状态机模式，status 字段锁定，状态变更仅允许通过行按钮触发，杜绝非法流转
- **跨模块解耦** — 各业务模块完全自包含，跨模块通信通过 Facade 接口 + 可选注入实现，任一模块可独立编译和测试
- **报表 & 打印** — 内置 erupt-report / erupt-print 扩展，支持跨模块报表种子初始化与业务单据模板化打印
- **全量测试** — 每个模块均配备独立 H2 内存库冒烟测试，覆盖 CRUD + 状态机全分支 + 跨模块集成

## 技术栈

| 组件 | 版本 |
|------|------|
| Java | 21 (LTS) |
| Spring Boot | 3.5.15 |
| Erupt | 2.1.0 |
| JPA / Hibernate | Spring Boot 托管 |
| H2 | 嵌入式（测试 + 开发） |
| Lombok | 全局启用 |

## 业务模块

| 模块 | 功能描述 |
|------|----------|
| **CRM** | 线索 → 客户（公海回收） → 商机 → 合同 → 回款计划全链路管理 |
| **ERP** | 产品主数据、采购订单、销售订单、收付款、财务发票、库存基础变更 |
| **Mall** | SPU/SKU 商品管理、订单交易、售后处理、支付流水 |
| **WMS** | 仓库/库区/库位三级结构、ASN/收货/上架/拣货/发货、四态库存（可用/锁定/在途/冻结） |
| **Fin** | 科目树、凭证管理（借贷平衡校验）、销售/采购发票、收付款、预算、成本中心 |
| **Mfg** | BOM 展开、生产工单（草稿→下达→生产→完工）、领料/报工 |
| **Proj** | 项目进度、任务管理、现金流统计、里程碑跟踪 |
| **Sup** | 服务工单生命周期、SLA 引擎（多级优先级）、知识库、工单关联 CRM 客户 |
| **Ast** | 资产台账、折旧引擎（直线法/双倍余额递减/末期补齐）、资产转移与维修 |
| **HR** | 员工档案、部门/职务体系、考勤打卡、请假申请工作流 |
| **Pay** | 工资结构、工资单生成与过账（GL）、薪资组件管理 |
| **Qal** | 质检标准、来料/过程/出货检验、上下限自动判定、不合格品 N/C 跟踪 |
| **Pur** | 采购请购 → 订单 → 收货单 → 供应商管理，收货自动触发库存入库 |
| **Stk** | 库存出入库单、盘点单、盘点差异自动生成 GL 凭证 |
| **Sal** | 销售报价单 → 销售订单 → 发货单全链路，发货自动触发库存出库 |
| **MP** | 微信公众号账号、粉丝管理、消息记录、自动回复、自定义菜单 |
| **IoT** | 产品分类/物模型、设备生命周期（未激活→在线↔离线→禁用）、告警规则与处置 |
| **Landing** | amis 拖拽 H5 落地页、短链接生成与统计、留资收集闭环 |

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+

### 构建 & 启动

```bash
git clone https://github.com/u2young/ep.git
cd ep

mvn clean package -DskipTests
java -jar ep-boot/target/ep.jar
```

访问 http://localhost:8080/erupt 进入管理后台。

### 运行测试

```bash
mvn test
# 174 个测试用例全部通过
```

### 单模块测试

```bash
mvn test -pl ep-module-crm -am
mvn test -pl ep-module-sal -am
mvn test -pl ep-boot -Dtest=EruptReportSmokeTest,EruptPrintSmokeTest
```

## 架构概览

```
ep-parent                       父 pom（版本收敛 + dependencyManagement）
├── ep-module-crm               客户关系管理
├── ep-module-erp               企业资源计划
├── ep-module-fin               财务会计
├── ep-module-mfg               生产制造
├── ep-module-proj              项目管理
├── ep-module-sup               服务支持
├── ep-module-ast               资产管理
├── ep-module-hr                人力资源
├── ep-module-pay               薪酬管理
├── ep-module-qal               质量管理
├── ep-module-pur               采购管理
├── ep-module-stk               库存管理
├── ep-module-sal               销售管理
├── ep-module-mall              电商商城
├── ep-module-wms               仓储管理
├── ep-module-mp                微信公众号
├── ep-module-iot               物联网
├── ep-module-landing           落地页
└── ep-boot                     打包层（装配所有模块，生成可执行 jar）
```

## 参考项目

- [Erupt](https://github.com/erupt-project/erupt) — 注解驱动后台框架
- [RuoYi-Vue-Pro](https://github.com/YunaiV/ruoyi-vue-pro) — CRM/ERP 业务模型参考
- [JetLinks](https://github.com/jetlinks) — IoT 设备管理参考
- [WxJava](https://github.com/Wechat-Group/WxJava) — 微信公众号 SDK 参考

## License

MIT
