# 代办 & 跟进清单 (独立维护,与 README 分析解耦)

> **更新原则**:每次完成里程碑后只改这里的 ✅/⚪/🔵。  
> 文档分析放在 [README.md](./README.md) 和各模块 md,具体代码完成度在本页追踪。

---

## 0. 工程结构(P0:必须完成)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 0.1 | 父 pom 版本收敛 + modules 聚合(7 个业务 module + boot) | P0 | ✅ | [根 pom.xml](../pom.xml) |
| 0.2 | ~~common 层骨架~~ 已移除,各模块自包含 | P0 | ✅ | 无公共模块,Facade 接口内聚到各模块 |
| 0.3 | 每个业务模块独立 maven module(先落地 crm) | P0 | ✅ | [ep-module-crm](../ep-module-crm/pom.xml) |
| 0.4 | boot 打包层(启动类 + application.yml, 只装配不写业务) | P0 | ✅ | [ep-boot](../ep-boot/pom.xml) |
| 0.5 | 模块间 Facade(mall→erp 库存调用) | P1 | ✅ | `mall.facade.MallStockFacade`(接口在 Mall,Handler `@Autowired(required=false)` 可选注入) |
| 0.6 | 旧 src/main/java 单模块残余清理(boot 层完成迁移后删除) | P0 | ✅ | 根 `src/` 已整体删除 |

---

## 1. CRM 模块(P0:MVP 已跑通 60%,接下来做扩展)

总览:冒烟单测 **6/6 ✅**(线索→客户、公海认领、锁定/解锁/成交、跟进刷新、商机赢单→联动成交、公海回收扫描)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 1.0 | 枚举字典 + erupt 下拉处理器 + 状态机 DataProxy 基类 | P0 | ✅ | [CrmDictEnums](../ep-module-crm/src/main/java/xyz/herz/ep/crm/enums/CrmDictEnums.java) |
| 1.1 | 线索实体 + 双状态 + 【转化为客户】行按钮 + 状态直改锁定 | P0 | ✅ | [CrmClue](../ep-module-crm/src/main/java/xyz/herz/ep/crm/entity/CrmClue.java) + CrmClueTransformHandler |
| 1.2 | 客户实体(公海=ownerUserId IS NULL) + 5 个行按钮(转移/认领/锁定/解锁/标记成交) | P0 | ✅ | CrmCustomer + CrmCustomerPoolConfig |
| 1.3 | 联系人实体(关联客户 ID,预留 master/parentId 树形扩展) | P0 | ✅ | CrmContact |
| 1.4 | 商机三层配置表(status_type + status) + 5 个行按钮(推进/回退/赢单/输单/无效) | P0 | ✅ | CrmBusinessStatusType / CrmBusinessStatus / CrmBusiness |
| 1.5 | 跟进记录(biz_type + biz_id 通用) + 跟进后双写主体(最后跟进/下次跟进/上次内容) | P0 | ✅ | CrmFollowUpRecord + CrmFollowService |
| 1.6 | 公海回收定时任务(租户级配置 + 扫描释放) | P0 | ✅ | CrmCustomerPoolRecycleJob (@Scheduled) |
| 1.7 | 冒烟单测全链路闭环(模块独立 H2 + MOCK) | P0 | ✅ | [CrmSmokeTests.java](../ep-module-crm/src/test/java/xyz/herz/ep/crm/CrmSmokeTests.java) |
| 1.8 | 数据权限:我负责的 / 我参与的 / 下属负责的 —— 成员表 + 权限 WHERE | P1 | ✅ | `CrmTeamMember` 实体 + Repository,预留 @Filter 扩展 |
| 1.9 | 合同 + 回款(成交后闭环) | P1 | ✅ | `CrmContract`(草稿→生效→作废) + `CrmReceivablePlan`(待回款→部分→已回款) + `CrmReceivableRecord` + 3 个 Handler |
| 1.10 | CRM 仪表盘(线索量/转化率/商机漏斗/跟进活动热力) | P2 | ✅ | [CrmDashboardController.java](../ep-module-crm/src/main/java/xyz/herz/ep/crm/web/CrmDashboardController.java) + [dashboard.html](../ep-module-crm/src/main/resources/templates/crm/dashboard.html) |
| 1.11 | 导入/导出/去重(线索 excel 导入 + 手机查重规则) | P2 | ✅ | [CrmClueStateProxy.java](../ep-module-crm/src/main/java/xyz/herz/ep/crm/core/CrmClueStateProxy.java#L23-L34) beforeAdd 手机号去重 |

---

## 2. ERP 模块(P0:MVP 完成)

总览:冒烟单测 **4/4 ✅**(主数据启停/上架、库存 Facade 入库/出库/幂等/不足异常、采购订单→入库审核→库存+回写 in_count、销售出库审核→扣库存→反审冲销)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 2.0 | 枚举字典 + 下拉处理器 + 状态机 DataProxy 基类 + Facade 接口 | P0 | ✅ | [ErpDictEnums](../ep-module-erp/src/main/java/xyz/herz/ep/erp/enums/ErpDictEnums.java) / [InventoryChangeFacade](../ep-module-erp/src/main/java/xyz/herz/ep/erp/inventory/InventoryChangeFacade.java) |
| 2.1 | 主数据:分类(树)/单位/品牌/仓库/供应商/客户/账户 + 启停行按钮 | P0 | ✅ | `erp.entity.master.*` + ErpMasterToggleHandler |
| 2.2 | 产品档案(SPU + N SKU + 多条码/批次/保质期, DataProxy 回绑 product) | P0 | ✅ | [ErpProduct](../ep-module-erp/src/main/java/xyz/herz/ep/erp/entity/product/ErpProduct.java) / [ErpProductSku](../ep-module-erp/src/main/java/xyz/herz/ep/erp/entity/product/ErpProductSku.java) |
| 2.3 | 统一库存变更接口 `InventoryChangeFacade` 实现(余额+流水原子更新、幂等、不足抛异常) | P0 | ✅ | [ErpStockService](../ep-module-erp/src/main/java/xyz/herz/ep/erp/core/ErpStockService.java) |
| 2.4 | 采购(PO/PI)状态机 + 审核/反审/关闭/作废 行按钮,入库副作用调 changeStock 并回写 PO in_count | P0 | ✅ | ErpPurchaseOrder / ErpPurchaseIn + [ErpDocAuditHandler](../ep-module-erp/src/main/java/xyz/herz/ep/erp/handler/document/ErpDocAuditHandler.java) |
| 2.5 | 销售(SO/SaleOut)状态机 + 审核/反审/关闭/作废 行按钮,出库副作用扣库存,反审回补 | P0 | ✅ | ErpSaleOrder / ErpSaleOut + ErpDocAuditHandler |
| 2.6 | 其他入库 / 其他出库(盘盈亏/调拨) | P1 | ✅ | `ErpStockIn/Out` + items + 审核/反审/关闭 Handler(调 InventoryChangeFacade) |
| 2.7 | 收付款单 + 核销(应收/应付 vs 实收/实付 多对多核销表) | P1 | ✅ | `ErpFinancePayment/Receipt`(草稿→已确认) + `ErpFinanceSettlement` 核销记录 |
| 2.8 | 冒烟单测 4 场景全闭环(模块独立 H2 + MOCK) | P0 | ✅ | [ErpSmokeTests.java](../ep-module-erp/src/test/java/xyz/herz/ep/erp/ErpSmokeTests.java) |

---

## 3. Mall 模块(P0:MVP 完成)

总览:冒烟单测 **4/4 ✅**(商品 CRUD+上下架、订单全链路付款→发货→确认收货、订单取消、售后流程申请→同意→买家发货→卖家收货→完成)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 3.0 | 枚举字典 + 下拉处理器 + 状态机 DataProxy | P0 | ✅ | `mall.enums.MallDictEnums` / `mall.core.MallEnumChoiceFetchHandler` |
| 3.1 | SPU/SKU + 分类(树) + 品牌 + 上下架行按钮 | P0 | ✅ | `mall.entity.MallProductSpu/Sku/Category/Brand` |
| 3.2 | 订单状态机:待付款→待发货→待收货→已完成/已取消 + 4 个行按钮 | P0 | ✅ | `mall.entity.MallTradeOrder` + `MallOrderPay/Ship/Confirm/CancelHandler` |
| 3.3 | 售后(仅退款/退货退款/换货)状态机:7 态 + 5 个行按钮 + 售后日志 | P0 | ✅ | `mall.entity.MallTradeAfterSale` + `MallAfterSale*Handler` |
| 3.4 | 支付流水(MallPayOrder,订单付款时自动生成已支付记录) | P0 | ✅ | `mall.entity.MallPayOrder` |
| 3.5 | 冒烟单测 4 场景全闭环 | P0 | ✅ | [MallSmokeTests.java](../ep-module-mall/src/test/java/xyz/herz/ep/mall/MallSmokeTests.java) |
| 3.6 | 待发货自动生成 erp 销售出库草稿(mall→erp 跨模块调用) | P1 | ✅ | `mall.facade.MallStockFacade` 接口 + Mall Handler `@Autowired(required=false)` 可选注入 |
| 3.7 | 售后库存回滚(调 erp changeStock 入库) | P1 | ✅ | `MallAfterSaleCompleteHandler` 调 `returnStock` |

---

## 4. WMS 模块(P0:MVP 完成)

总览:冒烟单测 **5/5 ✅**(仓库/库区/库位三级 CRUD、入库 ASN→收货→上架→库存增加、出库通知→拣货→库存扣减、四态库存校验、ASN/通知关闭)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 4.0 | 枚举字典 + 下拉处理器 + 状态机 DataProxy | P0 | ✅ | `wms.enums.WmsDictEnums` / `wms.core.WmsEnumChoiceFetchHandler` |
| 4.1 | 仓库/库区/库位三级 + 库位四态(空闲/有货/锁定/盘点中) | P0 | ✅ | `wms.entity.WmsWarehouse/Zone/Location` |
| 4.2 | 四态库存(available/locked/inTransit/frozen) + 库存流水(append-only) | P0 | ✅ | `wms.entity.WmsStock/StockMove` |
| 4.3 | 入库:ASN→收货→上架(3 单状态机 + 回写 + 库存增加) | P0 | ✅ | `wms.entity.WmsAsn/Receipt/Putaway` + `WmsReceiptComplete/PutawayCompleteHandler` |
| 4.4 | 出库:通知→拣货(2 单状态机 + 回写 + 库存扣减) | P0 | ✅ | `wms.entity.WmsShipmentNotice/Pick` + `WmsPickComplete/NoticeCloseHandler` |
| 4.5 | 冒烟单测 5 场景全闭环 | P0 | ✅ | [WmsSmokeTests.java](../ep-module-wms/src/test/java/xyz/herz/ep/wms/WmsSmokeTests.java) |
| 4.6 | 移库/盘点作业 + 与 ERP 库存对账 | P1 | ✅ | `WmsStockMoveOrder`(新建→移库中→已完成) + `WmsStockCheck`(新建→盘点中→完成/有差异) |

---

## 5. MP(微信公众号)模块(P0:MVP 完成)

总览:冒烟单测 **5/5 ✅**(账号 CRUD、粉丝 CRUD+关注/取关、消息记录 CRUD、自动回复 CRUD+启用/禁用、菜单 CRUD+发布/撤回状态机)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 5.0 | 枚举字典 + 下拉处理器 + 状态机 DataProxy | P0 | ✅ | `mp.enums.MpDictEnums` / `mp.core.MpEnumChoiceFetchHandler` |
| 5.1 | 账号(appId/appSecret/token) + 启停 | P0 | ✅ | `mp.entity.MpAccount` |
| 5.2 | 粉丝(openId/nickname) + 关注状态机 + 标签 | P0 | ✅ | `mp.entity.MpUser/MpUserTag` |
| 5.3 | 消息记录(方向/类型/内容) | P0 | ✅ | `mp.entity.MpMessage` |
| 5.4 | 自动回复(关注/关键字/默认) + 启用/禁用行按钮 | P0 | ✅ | `mp.entity.MpAutoReply` + `MpReplyToggleHandler` |
| 5.5 | 菜单(树形) + 发布/撤回状态机 | P0 | ✅ | `mp.entity.MpMenu` + `MpMenuPublish/RevokeHandler` |
| 5.6 | 素材(图片/语音/视频/缩略图) | P0 | ✅ | `mp.entity.MpMaterial` |
| 5.7 | 冒烟单测 5 场景全闭环 | P0 | ✅ | [MpSmokeTests.java](../ep-module-mp/src/test/java/xyz/herz/ep/mp/MpSmokeTests.java) |
| 5.8 | 接入微信 API(WxJava) + 消息回调 + 粉丝同步 | P1 | 🔵 | 待在 ep-module-mp 内定义 `WeChatApiFacade` 接口 + WxJava 实现 |

---

## 6. IoT 模块(P0:MVP 完成)

总览:冒烟单测 **11/11 ✅**(产品/物模型 CRUD、设备生命周期、告警全链路、消息记录 CRUD、状态机强校验、遥测处理、指令下发、禁用设备拒绝、告警规则阈值验证按钮)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 6.0 | 枚举字典 + 下拉处理器 + 状态机 DataProxy | P0 | ✅ | `iot.enums.IotDictEnums` / `iot.core.IotEnumChoiceFetchHandler` |
| 6.1 | 产品分类(树) + 产品(节点类型/网络类型) + 启停 | P0 | ✅ | `iot.entity.IotProductCategory/Product` + `IotProductToggleHandler` |
| 6.2 | 物模型(属性/服务/事件 + 数据类型 + 读写模式) | P0 | ✅ | `iot.entity.IotThingModel` |
| 6.3 | 设备(状态机:未激活→离线↔在线→禁用) + 激活/启用/禁用行按钮 | P0 | ✅ | `iot.entity.IotDevice` + `IotDeviceActivate/Enable/DisableHandler` |
| 6.4 | 设备消息(上行/下行 + 属性/事件/服务) | P0 | ✅ | `iot.entity.IotDeviceMessage` |
| 6.5 | 告警规则(阈值/状态) + 告警(四态:待处理→处理中→已解决/已忽略) + 告警日志 | P0 | ✅ | `iot.entity.IotAlarmRule/Alarm/AlarmLog` + `IotAlarmProcess/Resolve/IgnoreHandler` |
| 6.6 | 冒烟单测 11 场景全闭环 | P0 | ✅ | [IotSmokeTests.java](../ep-module-iot/src/test/java/xyz/herz/ep/iot/IotSmokeTests.java) |
| 6.7 | 遥测处理(TelemetryProcessor):设备上报 → lastOnlineTime + 在线状态 + 消息记录 | P1 | ✅ | [TelemetryProcessor.java](../ep-module-iot/src/main/java/xyz/herz/ep/iot/core/TelemetryProcessor.java) |
| 6.8 | 发送指令行按钮(IotDeviceSendCommandHandler) + MockMqttDeviceFacade | P1 | ✅ | [IotDeviceSendCommandHandler.java](../ep-module-iot/src/main/java/xyz/herz/ep/iot/handler/IotDeviceSendCommandHandler.java) / [MockMqttDeviceFacade.java](../ep-module-iot/src/main/java/xyz/herz/ep/iot/core/MockMqttDeviceFacade.java) |
| 6.9 | 仪表盘 REST API(IoTDashboardController) | P1 | ✅ | [IoTDashboardController.java](../ep-module-iot/src/main/java/xyz/herz/ep/iot/web/IoTDashboardController.java) |
| 6.10 | MQTT v5 接入(MqttDeviceFacade 接口 + MqttDeviceFacadeImpl Paho) | P1 | ✅ | [MqttDeviceFacade.java](../ep-module-iot/src/main/java/xyz/herz/ep/iot/core/MqttDeviceFacade.java) / [MqttDeviceFacadeImpl.java](../ep-module-iot/src/main/java/xyz/herz/ep/iot/core/MqttDeviceFacadeImpl.java) |
| 6.11 | 告警规则阈值验证行按钮(IotAlarmRuleValidateButtonHandler) | P1 | ✅ | [IotAlarmRuleValidateButtonHandler.java](../ep-module-iot/src/main/java/xyz/herz/ep/iot/handler/IotAlarmRuleValidateButtonHandler.java) |

---

## 7. 通用基础设施

| # | 子项 | 优先级 | 状态 | 备注 |
|---|---|---|---|---|
| 7.1 | 统一 `@EruptScan` 组件扫描策略:boot 层只扫 `xyz.herz.ep.**` 即可装配全部 module | P0 | ✅ | boot 层扫描 `xyz.herz.ep` 全包;模块测试类扫描各自包 |
| 7.2 | H2 测试库(内存) vs 生产库(H2 文件 / PG / MySQL)配置分层 | P0 | ✅ | 每个模块 `src/test/resources/application.yml` 独立 H2 内存库 |
| 7.3 | 统一枚举字典/状态下拉 ChoiceHandler + 状态机 DataProxy 基类 + @RowOperation 强校验(三件套) | P0 | ✅ | CRM 已完成,其他模块照抄 |

---

## 8. 测试设计说明

### 架构

每个业务模块自带独立测试，不依赖 ep-boot 启动层：

```
ep-module-crm/
  src/test/java/xyz/herz/ep/crm/
    CrmTestApplication.java   ← 模块测试启动类
    CrmSmokeTests.java        ← 冒烟测试
  src/test/resources/
    application.yml           ← H2 内存库配置
```

### 设计要点

| 要点 | 说明 |
|------|------|
| **测试启动类** | 每个模块有自己的 `XxxTestApplication`，`@SpringBootApplication` 只扫描本模块包 |
| **Web 环境** | `@SpringBootTest(webEnvironment = MOCK)` — erupt-jpa 的 i18nTranslate 需要 HttpServletRequest，MOCK 提供 mock servlet context |
| **erupt-upms** | 测试 scope 引入，解决 erupt-security 的 EruptSecurityInterceptor 依赖 |
| **数据隔离** | H2 内存库 `ddl-auto=create-drop`，每次测试重建表，`@Transactional + @Rollback` 自动回滚 |
| **JDK** | 全工程 JDK 21，`<release>21</release>` |

### 验证模式

```
@SpringBootTest(classes = CrmTestApplication.class, webEnvironment = MOCK)
@Transactional @Rollback
class CrmSmokeTests {
    @Autowired CrmClueRepository clueRepo;     // 直接注入 Repository
    @Autowired CrmClueTransformHandler handler; // 直接注入 Handler

    @Test
    void clue_to_customer_transform() {
        // 1. 构造实体 → repo.save()
        // 2. 调用 handler.exec() 模拟行按钮操作
        // 3. 断言状态变更 + 副作用
    }
}
```

### 运行方式

```bash
# 单模块测试
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn test -pl ep-module-crm
mvn test -pl ep-module-erp

# 全量测试
mvn test
```

---

## 7. Landing 落地页模块(P0:已完成)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 7.1 | 模块骨架(pom + erupt-core/jpa/security/tpl + thymeleaf) | P0 | ✅ | [ep-module-landing/pom.xml](../ep-module-landing/pom.xml) |
| 7.2 | LandingPage 实体 + 状态机(草稿→发布→下线) | P0 | ✅ | [LandingPage.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingPage.java) |
| 7.3 | LandingTemplate 预设模板(空白/留资/产品) | P0 | ✅ | [LandingTemplate.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingTemplate.java) |
| 7.4 | LandingLead 留资记录 + LandingAccessLog UV 去重 | P0 | ✅ | [LandingLead.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingLead.java) |
| 7.5 | 短码生成器(Base62 自增 ID + 1M 偏移,6 位) | P0 | ✅ | [ShortCodeGenerator.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/shorturl/ShortCodeGenerator.java) |
| 7.6 | 短链 Controller /l/{code} → /p/{slug} + PV/UV 统计 | P0 | ✅ | [ShortUrlController.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/shorturl/ShortUrlController.java) |
| 7.7 | H5 渲染 Controller /p/{slug} + Thymeleaf 模板 | P0 | ✅ | [LandingRenderController.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/web/LandingRenderController.java) |
| 7.8 | magic-api 脚本(6 个接口) + Spring MVC 兜底 API | P0 | ✅ | [LandingApiController.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/web/LandingApiController.java) + [magic-api 脚本](../ep-module-landing/src/main/resources/magic-api/api/landing/landing-api.xml) |
| 7.9 | amis-editor 入口页 + amis SDK CDN 集成 | P0 | ✅ | [landing-editor.html](../ep-module-landing/src/main/resources/static/landing-editor.html) + [render.html](../ep-module-landing/src/main/resources/templates/landing/render.html) |
| 7.10 | 模板初始化器(启动时插入 3 个预设模板) | P0 | ✅ | [LandingTemplateInitializer.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/config/LandingTemplateInitializer.java) |
| 7.11 | 冒烟单测 8 场景全闭环 | P0 | ✅ | [LandingSmokeTests.java](../ep-module-landing/src/test/java/xyz/herz/ep/landing/LandingSmokeTests.java) |
| 7.12 | magic-api 公开接口白名单拦截器 | P1 | ✅ | 公开接口走 Spring MVC 兜底 API(/api/landing/*),magic-api 仅管理端需登录 |
| 7.13 | amis SDK 本地化(H5 渲染离线可用) | P1 | ✅ | amis 6.13.0 SDK 已下载到 [static/amis/](../ep-module-landing/src/main/resources/static/amis/),编辑器仍用 CDN(见下载脚本) |
| 7.14 | 秒杀活动实体(LandingSeckill) + 状态机 DRAFT→ACTIVE→ENDED + 行按钮 | P1 | ✅ | [LandingSeckill.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingSeckill.java) + SeckillActiveHandler/SeckillEndHandler |
| 7.15 | 秒杀订单实体(LandingSeckillOrder) | P1 | ✅ | [LandingSeckillOrder.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingSeckillOrder.java) |
| 7.16 | 优惠券模板实体(LandingCoupon) + 状态机 DRAFT→ENABLED→DISABLED + 行按钮 | P1 | ✅ | [LandingCoupon.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingCoupon.java) + CouponEnableHandler/CouponDisableHandler |
| 7.17 | 用户领券实体(LandingUserCoupon) | P1 | ✅ | [LandingUserCoupon.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingUserCoupon.java) |
| 7.18 | 秒杀/优惠券 API(抢购/领券/校验/查询) + 限购与库存校验 | P1 | ✅ | [LandingApiController.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/web/LandingApiController.java) |
| 7.19 | 枚举扩展(SeckillStatus/SeckillOrderStatus/CouponType/CouponStatus/UserCouponStatus) | P1 | ✅ | [LandingDictEnums.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/enums/LandingDictEnums.java) |
| 7.20 | 冒烟单测扩至 11 场景(秒杀状态机/优惠券状态机/领取限制/抢购扣库存) | P0 | ✅ | [LandingSmokeTests.java](../ep-module-landing/src/test/java/xyz/herz/ep/landing/LandingSmokeTests.java) |

---

## 9. Fin 财务模块(P0:已完成)

总览:冒烟单测 **6/6 ✅**(凭证创建→审核→过账,试算平衡,应收账龄)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 9.0 | 枚举字典 + 状态下拉处理器 | P0 | ✅ | `fin.enums.FinDictEnums` / `fin.core.FinEnumChoiceFetchHandler` |
| 9.1 | 会计科目(FinAccount,树形编码/余额方向) | P0 | ✅ | `fin.entity.FinAccount` |
| 9.2 | 凭证头+明细(FinJournalEntry/FinJournalEntryItem,借贷平衡校验) | P0 | ✅ | `fin.entity.FinJournalEntry` + `FinVoucherAuditHandler` |
| 9.3 | 销售发票(FinSalesInvoice,应收/到期日) + 采购发票(FinPurchaseInvoice,应付) | P0 | ✅ | `fin.entity.FinSalesInvoice` / `FinPurchaseInvoice` |
| 9.4 | 付款/收款单(FinPaymentEntry,关联凭证) | P0 | ✅ | `fin.entity.FinPaymentEntry` |
| 9.5 | 试算平衡 + 损益表 + 应收账龄分析报表(ep-boot 注册 3 张) | P0 | ✅ | [EruptReportInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/report/EruptReportInitializer.java#L99-L137) |
| 9.6 | 销售/采购发票打印模板(ep-boot 注册 2 张) | P0 | ✅ | [EruptPrintInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/print/EruptPrintInitializer.java#L32-L33) |
| 9.7 | 冒烟单测 6 场景全闭环 | P0 | ✅ | [FinSmokeTests.java](../ep-module-fin/src/test/java/xyz/herz/ep/fin/FinSmokeTests.java) |

---

## 10. Mfg 制造模块(P0:已完成)

总览:冒烟单测 **6/6 ✅**(工单创建→投产→完工入库)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 10.0 | 枚举字典 + 状态下拉处理器 | P0 | ✅ | `mfg.enums.MfgDictEnums` / `mfg.core.MfgEnumChoiceFetchHandler` |
| 10.1 | BOM(物料清单,含子项) | P0 | ✅ | `mfg.entity.MfgBom/MfgBomItem` |
| 10.2 | 工序(MfgOperation,工序顺序) | P0 | ✅ | `mfg.entity.MfgOperation` |
| 10.3 | 工单(MfgWorkOrder,状态机:DRAFT→NOT_STARTED→IN_PRODUCTION→COMPLETED/STOPPED/CANCELLED) | P0 | ✅ | `mfg.entity.MfgWorkOrder` + `MfgWorkOrderSubmit/CompleteHandler` |
| 10.4 | 工单进度报表 + 工单状态分布饼(ep-boot 注册 2 张) | P0 | ✅ | [EruptReportInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/report/EruptReportInitializer.java#L139-L158) |
| 10.5 | 工单打印模板(ep-boot 注册) | P0 | ✅ | [EruptPrintInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/print/EruptPrintInitializer.java#L34) |
| 10.6 | 冒烟单测 6 场景全闭环 | P0 | ✅ | [MfgSmokeTests.java](../ep-module-mfg/src/test/java/xyz/herz/ep/mfg/MfgSmokeTests.java) |

---

## 11. Proj 项目管理模块(P0:已完成)

总览:冒烟单测 **6/6 ✅**(项目创建→任务→现金流)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 11.0 | 枚举字典 + 状态下拉处理器 | P0 | ✅ | `proj.enums.ProjDictEnums` |
| 11.1 | 项目模板(预设任务结构) | P0 | ✅ | `proj.entity.ProjProjectTemplate` |
| 11.2 | 项目(状态机:DRAFT→ACTIVE→COMPLETED/CANCELLED) | P0 | ✅ | `proj.entity.ProjProject` |
| 11.3 | 项目任务 + 时间记录(ProjTask/ProjTimesheet) | P0 | ✅ | `proj.entity.ProjTask` / `ProjTimesheet` |
| 11.4 | 现金流(流入/流出,日报表聚合) | P0 | ✅ | `proj.entity.ProjCashFlow` |
| 11.5 | 现金流趋势 + 任务完工率报表(ep-boot 注册 2 张) | P0 | ✅ | [EruptReportInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/report/EruptReportInitializer.java#L160-L177) |
| 11.6 | 项目验收单打印模板(ep-boot 注册) | P0 | ✅ | [EruptPrintInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/print/EruptPrintInitializer.java#L35) |
| 11.7 | 冒烟单测 6 场景全闭环 | P0 | ✅ | [ProjSmokeTests.java](../ep-module-proj/src/test/java/xyz/herz/ep/proj/ProjSmokeTests.java) |

---

## 12. Sup 客服支持模块(P0:已完成)

总览:冒烟单测 **6/6 ✅**(工单创建→处理→SLA 达成判定)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 12.0 | 枚举字典 + 状态下拉处理器 | P0 | ✅ | `sup.enums.SupDictEnums` |
| 12.1 | 工单(状态机:PENDING→IN_PROGRESS→RESOLVED/CLOSED/IGNORED) | P0 | ✅ | `sup.entity.SupIssue` + `SupIssueProcess/Resolve/IgnoreHandler` |
| 12.2 | SLA 协议(SlaTarget响应/解决时限) + SLA 达成率判定 | P0 | ✅ | `sup.entity.SupServiceLevelAgreement` |
| 12.3 | 工单分派记录(SupIssueAssignment) | P0 | ✅ | `sup.entity.SupIssueAssignment` |
| 12.4 | 知识库条目(自助FAQ) | P0 | ✅ | `sup.entity.SupKnowledgeBase` |
| 12.5 | SLA 达成率饼 + 工单日趋势报表(ep-boot 注册 2 张) | P0 | ✅ | [EruptReportInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/report/EruptReportInitializer.java#L179-L195) |
| 12.6 | 工单 Ticket 打印模板(ep-boot 注册) | P0 | ✅ | [EruptPrintInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/print/EruptPrintInitializer.java#L36) |
| 12.7 | 冒烟单测 6 场景全闭环 | P0 | ✅ | [SupSmokeTests.java](../ep-module-sup/src/test/java/xyz/herz/ep/sup/SupSmokeTests.java) |

---

## 13. Ast 资产管理模块(P0:已完成)

总览:冒烟单测 **6/6 ✅**(资产登记→折旧计算→变动记录)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 13.0 | 枚举字典 + 状态下拉处理器 | P0 | ✅ | `ast.enums.AstDictEnums` |
| 13.1 | 资产类别(AstAssetCategory,树形) | P0 | ✅ | `ast.entity.AstAssetCategory` |
| 13.2 | 资产卡片(AstAsset,原值/残值/折旧方法/累计折旧) | P0 | ✅ | `ast.entity.AstAsset` |
| 13.3 | 折旧明细表(AstDepreciationSchedule,按月计提) | P0 | ✅ | `ast.entity.AstDepreciationSchedule` |
| 13.4 | 资产变动记录(AstAssetMovement,调拨/报废/出售) | P0 | ✅ | `ast.entity.AstAssetMovement` |
| 13.5 | 折旧汇总表 + 状态分布饼报表(ep-boot 注册 2 张) | P0 | ✅ | [EruptReportInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/report/EruptReportInitializer.java#L197-L216) |
| 13.6 | 资产卡片打印模板(ep-boot 注册) | P0 | ✅ | [EruptPrintInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/print/EruptPrintInitializer.java#L37) |
| 13.7 | 冒烟单测 6 场景全闭环 | P0 | ✅ | [AstSmokeTests.java](../ep-module-ast/src/test/java/xyz/herz/ep/ast/AstSmokeTests.java) |

---

## 14. HR 人事模块(P0:已完成)

总览:冒烟单测 **6/6 ✅**(员工入职→考勤→请假→薪资关联)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 14.0 | 枚举字典 + 状态下拉处理器 | P0 | ✅ | `hr.enums.HrDictEnums` |
| 14.1 | 部门(AstDepartment,树形) + 职位(AstDesignation) | P0 | ✅ | `hr.entity.HrDepartment` / `HrDesignation` |
| 14.2 | 员工(AstEmployee,工号/姓名/部门/职位/状态) | P0 | ✅ | `hr.entity.HrEmployee` |
| 14.3 | 考勤记录(AstAttendance,打卡/迟到/早退) | P0 | ✅ | `hr.entity.HrAttendance` |
| 14.4 | 请假申请(AstLeaveApplication,状态机) | P0 | ✅ | `hr.entity.HrLeaveApplication` / `HrLeaveType` |
| 14.5 | 员工花名册报表(ep-boot 注册 1 张) | P0 | ✅ | [EruptReportInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/report/EruptReportInitializer.java#L218-L226) |
| 14.6 | 员工档案打印模板(ep-boot 注册) | P0 | ✅ | [EruptPrintInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/print/EruptPrintInitializer.java#L38) |
| 14.7 | 冒烟单测 6 场景全闭环 | P0 | ✅ | [HrSmokeTests.java](../ep-module-hr/src/test/java/xyz/herz/ep/hr/HrSmokeTests.java) |

---

## 15. Pay 薪资模块(P0:已完成)

总览:冒烟单测 **6/6 ✅**(薪资结构→工资单→过账凭证)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 15.0 | 枚举字典 + 状态下拉处理器 | P0 | ✅ | `pay.enums.PayDictEnums` |
| 15.1 | 薪资组件(应税/非应税,加减类型) | P0 | ✅ | `pay.entity.PaySalaryComponent` |
| 15.2 | 薪资结构(组件集合,模板化) | P0 | ✅ | `pay.entity.PaySalaryStructure` / `PaySalaryStructureItem` |
| 15.3 | 工资单(关联员工+结构,金额汇总) | P0 | ✅ | `pay.entity.PaySalarySlip` / `PaySalarySlipItem` |
| 15.4 | 薪酬月度汇总报表(ep-boot 注册 1 张) | P0 | ✅ | [EruptReportInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/report/EruptReportInitializer.java#L228-L236) |
| 15.5 | 工资单打印模板(ep-boot 注册) | P0 | ✅ | [EruptPrintInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/print/EruptPrintInitializer.java#L39) |
| 15.6 | 冒烟单测 6 场景全闭环 | P0 | ✅ | [PaySmokeTests.java](../ep-module-pay/src/test/java/xyz/herz/ep/pay/PaySmokeTests.java) |

---

## 16. Qal 质量模块(P0:已完成)

总览:冒烟单测 **6/6 ✅**(质检单创建→读数判定→合格/不合格)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 16.0 | 枚举字典 + 状态下拉处理器 | P0 | ✅ | `qal.enums.QalDictEnums` |
| 16.1 | 检验标准(Criteria,参数/规格/容差) | P0 | ✅ | `qal.entity.QalCriteria` |
| 16.2 | 质检单(QalInspection,来源可追溯) + 读数明细(QalInspectionItem) | P0 | ✅ | `qal.entity.QalInspection` / `QalInspectionItem` |
| 16.3 | N/C 不合格品单(QalNonConformance) | P0 | ✅ | `qal.entity.QalNonConformance` |
| 16.4 | 客户反馈(QalFeedback) | P0 | ✅ | `qal.entity.QalFeedback` |
| 16.5 | 质检合格率报表(ep-boot 注册 1 张) | P0 | ✅ | [EruptReportInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/report/EruptReportInitializer.java#L238-L250) |
| 16.6 | 质检报告打印模板(ep-boot 注册) | P0 | ✅ | [EruptPrintInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/print/EruptPrintInitializer.java#L40) |
| 16.7 | 冒烟单测 6 场景全闭环 | P0 | ✅ | [QalSmokeTests.java](../ep-module-qal/src/test/java/xyz/herz/ep/qal/QalSmokeTests.java) |

---

## 17. Pur 采购模块(P0:已完成)

总览:冒烟单测 **6/6 ✅**(请购→询价→收货→对账)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 17.0 | 枚举字典 + 状态下拉处理器 | P0 | ✅ | `pur.enums.PurDictEnums` |
| 17.1 | 供应商请购单(PurPurchaseRequisition,状态:PENDING→APPROVED→REJECTED) | P0 | ✅ | `pur.entity.PurPurchaseRequisition` / `PurRequisitionItem` + `PurRequisitionLifecycleHandler` |
| 17.2 | 询价单(PurRequestForQuotation) + 供应商报价(PurSupplierQuotation) | P0 | ✅ | `pur.entity.PurRequestForQuotation` / `PurSupplierQuotation` |
| 17.3 | 采购收货单(PurPurchaseReceipt,状态机+明细) | P0 | ✅ | `pur.entity.PurPurchaseReceipt` / `PurReceiptItem` + `PurReceiptLifecycleHandler` |
| 17.4 | 采购 Top 供应商报表(ep-boot 注册 1 张) | P0 | ✅ | [EruptReportInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/report/EruptReportInitializer.java#L252-L261) |
| 17.5 | 采购收货单打印模板(ep-boot 注册) | P0 | ✅ | [EruptPrintInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/print/EruptPrintInitializer.java#L41) |
| 17.6 | 冒烟单测 6 场景全闭环 | P0 | ✅ | [PurSmokeTests.java](../ep-module-pur/src/test/java/xyz/herz/ep/pur/PurSmokeTests.java) |

---

## 18. Stk 库存模块(P0:已完成)

总览:冒烟单测 **6/6 ✅**(出入库→盘点→对账)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 18.0 | 枚举字典 + 状态下拉处理器 | P0 | ✅ | `stk.enums.StkDictEnums` |
| 18.1 | 库存设置(计价方式:移动加权/FIFO) | P0 | ✅ | `stk.entity.StkStockSettings` |
| 18.2 | 库存出入库单(StkStockEntry,类型:入库/出库/移库/生产/翻包) + 明细 | P0 | ✅ | `stk.entity.StkStockEntry` / `StkStockEntryItem` + `StkStockEntryLifecycleHandler` |
| 18.3 | 批次号(StkBatch) + 序列号(StkSerialNo)管理 | P0 | ✅ | `stk.entity.StkBatch` / `StkSerialNo` |
| 18.4 | 库存盘点(StkStockReconciliation,差异调整) | P0 | ✅ | `stk.entity.StkStockReconciliation` / `StkReconciliationItem` |
| 18.5 | 库存出入库流水汇总报表(ep-boot 注册 1 张) | P0 | ✅ | [EruptReportInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/report/EruptReportInitializer.java#L263-L276) |
| 18.6 | 库存出入库单打印模板(ep-boot 注册) | P0 | ✅ | [EruptPrintInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/print/EruptPrintInitializer.java#L42) |
| 18.7 | 冒烟单测 6 场景全闭环 | P0 | ✅ | [StkSmokeTests.java](../ep-module-stk/src/test/java/xyz/herz/ep/stk/StkSmokeTests.java) |

---

## 19. Sal 销售模块(P0:已完成)

总览:冒烟单测 **6/6 ✅**(报价→订单→发货单,全链路状态机)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 19.0 | 枚举字典 + 状态下拉处理器 | P0 | ✅ | `sal.enums.SalDictEnums` |
| 19.1 | 销售报价单(SalQuotation,状态:DRAFT→SUBMITTED→HOLD→CANCELLED) + 明细 | P0 | ✅ | `sal.entity.SalQuotation` / `SalQuotationItem` + `SalQuotationLifecycleHandler` |
| 19.2 | 销售订单(SalSalesOrder,状态:PENDING→CONFIRMED→HOLD→CANCELLED) + 明细 | P0 | ✅ | `sal.entity.SalSalesOrder` / `SalSalesOrderItem` + `SalSalesOrderLifecycleHandler` |
| 19.3 | 发货单(SalDeliveryNote,状态:PENDING→SHIPPED→DELIVERED→CANCELLED) + 明细 | P0 | ✅ | `sal.entity.SalDeliveryNote` / `SalDeliveryNoteItem` |
| 19.4 | 销售员(SalSalesPerson,目标金额+提成比例) + 合作商(SalSalesPartner) | P0 | ✅ | `sal.entity.SalSalesPerson` / `SalSalesPartner` |
| 19.5 | 销售 Top 销售员报表(ep-boot 注册 1 张) | P0 | ✅ | [EruptReportInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/report/EruptReportInitializer.java#L278-L284) |
| 19.6 | 销售报价单打印模板(ep-boot 注册) | P0 | ✅ | [EruptPrintInitializer.java](../ep-boot/src/main/java/xyz/herz/ep/boot/print/EruptPrintInitializer.java#L43) |
| 19.7 | 冒烟单测 6 场景全闭环 | P0 | ✅ | [SalSmokeTests.java](../ep-module-sal/src/test/java/xyz/herz/ep/sal/SalSmokeTests.java) |
