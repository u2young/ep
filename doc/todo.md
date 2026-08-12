# 代办 & 跟进清单 (独立维护,与 README 分析解耦)

> **更新原则**:每次完成里程碑后只改这里的 ✅/⚪/🔵。  
> 文档分析放在 [README.md](./README.md) 和各模块 md,具体代码完成度在本页追踪。

---

## 0. 工程结构(P0:必须完成)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 0.1 | 父 pom 版本收敛 + modules 聚合(common / crm module / boot) | P0 | ✅ | [根 pom.xml](../pom.xml) |
| 0.2 | common 层骨架(后续放通用 Facade/BaseEntity) | P0 | ✅ | [ep-business-common](../ep-business-common/pom.xml) |
| 0.3 | 每个业务模块独立 maven module(先落地 crm) | P0 | ✅ | [ep-module-crm](../ep-module-crm/pom.xml) |
| 0.4 | boot 打包层(启动类 + application.yml, 只装配不写业务) | P0 | ✅ | [ep-boot](../ep-boot/pom.xml) |
| 0.5 | 模块间 Facade(erupt-cloud 拆分预留):mall→erp 库存、erp→wms 出入库 | P1 | ✅ | `common.facade.MallStockFacade` / `WmsOperationFacade` / `WeChatApiFacade` / `MqttDeviceFacade` |
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
| 1.10 | CRM 仪表盘(线索量/转化率/商机漏斗/跟进活动热力) | P2 | ⚪ | 需 erupt-tpl 前端模板 |
| 1.11 | 导入/导出/去重(线索 excel 导入 + 手机查重规则) | P2 | ⚪ | 需 erupt-excel 配置 |

---

## 2. ERP 模块(P0:MVP 完成)

总览:冒烟单测 **4/4 ✅**(主数据启停/上架、库存 Facade 入库/出库/幂等/不足异常、采购订单→入库审核→库存+回写 in_count、销售出库审核→扣库存→反审冲销)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 2.0 | 枚举字典 + 下拉处理器 + 状态机 DataProxy 基类 + Facade 接口 | P0 | ✅ | [ErpDictEnums](../ep-module-erp/src/main/java/xyz/herz/ep/erp/enums/ErpDictEnums.java) / [InventoryChangeFacade](../ep-business-common/src/main/java/xyz/herz/ep/common/inventory/InventoryChangeFacade.java) |
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
| 3.6 | 待发货自动生成 erp 销售出库草稿(mall→erp 跨模块调用) | P1 | ✅ | `MallStockFacade` 接口 + Mall Handler `@Autowired(required=false)` 可选注入 |
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
| 5.8 | 接入微信 API(WxJava) + 消息回调 + 粉丝同步 | P1 | 🔵 | `WeChatApiFacade` 接口已定义,待 WxJava 实现 |

---

## 6. IoT 模块(P0:MVP 完成)

总览:冒烟单测 **5/5 ✅**(产品/物模型 CRUD、设备生命周期未激活→激活→禁用→启用、告警全链路处理→解决/忽略、消息记录 CRUD、状态机非法前置状态强校验)

| # | 子项 | 优先级 | 状态 | 代码位置 |
|---|---|---|---|---|
| 6.0 | 枚举字典 + 下拉处理器 + 状态机 DataProxy | P0 | ✅ | `iot.enums.IotDictEnums` / `iot.core.IotEnumChoiceFetchHandler` |
| 6.1 | 产品分类(树) + 产品(节点类型/网络类型) + 启停 | P0 | ✅ | `iot.entity.IotProductCategory/Product` + `IotProductToggleHandler` |
| 6.2 | 物模型(属性/服务/事件 + 数据类型 + 读写模式) | P0 | ✅ | `iot.entity.IotThingModel` |
| 6.3 | 设备(状态机:未激活→离线↔在线→禁用) + 激活/启用/禁用行按钮 | P0 | ✅ | `iot.entity.IotDevice` + `IotDeviceActivate/Enable/DisableHandler` |
| 6.4 | 设备消息(上行/下行 + 属性/事件/服务) | P0 | ✅ | `iot.entity.IotDeviceMessage` |
| 6.5 | 告警规则(阈值/状态) + 告警(四态:待处理→处理中→已解决/已忽略) + 告警日志 | P0 | ✅ | `iot.entity.IotAlarmRule/Alarm/AlarmLog` + `IotAlarmProcess/Resolve/IgnoreHandler` |
| 6.6 | 冒烟单测 5 场景全闭环 | P0 | ✅ | [IotSmokeTests.java](../ep-module-iot/src/test/java/xyz/herz/ep/iot/IotSmokeTests.java) |
| 6.7 | 接入 MQTT(EMQX) + 设备影子 + 时序数据 | P1 | 🔵 | `MqttDeviceFacade` 接口已定义,待 Paho/Spring Integration MQTT 实现 |

---

## 7. 通用基础设施

| # | 子项 | 优先级 | 状态 | 备注 |
|---|---|---|---|---|
| 7.1 | 统一 `@EruptScan` 组件扫描策略:boot 层只扫 `xyz.herz.ep.**` 即可装配全部 module | P0 | ✅ | boot 层扫描 `xyz.herz.ep` 全包;模块测试类扫描各自包+common |
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
| **测试启动类** | 每个模块有自己的 `XxxTestApplication`，`@SpringBootApplication` 只扫描本模块 + common 包 |
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
``` |
