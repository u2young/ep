# 变更日志 (CHANGELOG)

> 本文件记录 erupt-cloud 项目的迭代变更。  
> 版本号遵循 [语义化版本](https://semver.org/)。

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
