# 业务模块功能设计文档

> **版本**: v1.1
> **更新时间**: 2026-08-11
> **目标**: 为 erupt01 项目扩展业务模块提供完整设计参考,重点关注数据状态流转(状态机),而非简单 CRUD
> **MVP 原则**: 基本功能闭环完整(状态机跑通)+ 字段/结构预留扩展口,后续迭代无需重构表结构

## 文档说明

本目录包含 6 个业务模块的完整功能设计文档,所有文档均基于以下原则:

1. **状态流转完整** — 每个核心实体都给出 Mermaid stateDiagram 状态机图,而非简单 CRUD
2. **Erupt 适配** — 注解驱动(`@Erupt` / `@EruptField` / `@RowOperation` / `@ChoiceType` / `@Tree` / `DataProxy`),适配 Spring Boot 3.5.15 + Erupt 2.0.3 + JPA + H2 技术栈
3. **不依赖 yudao 的 infra / system** — 使用 Erupt 默认的 UPMS(erupt-upms)与基础设施
4. **参考来源明确** — 每篇文档末尾列出官方文档、源码仓库、同类开源项目参考链接

## 模块清单

| 模块 | 文档 | 行数 | 核心业务链 | 状态机数 |
|---|---|---|---|---|
| 商城 | [mall.md](./mall.md) | 1225 | 商品→订单→售后→营销 | 6 |
| 客户关系 | [crm.md](./crm.md) | 868 | 线索→客户→商机→合同→回款 | 5 |
| 企业资源 | [erp.md](./erp.md) | 1235 | 采购→入库→库存→销售→财务 | 8 |
| 物联网 | [iot.md](./iot.md) | 1320 | 产品→物模型→设备→消息→规则→告警 | 5 |
| 微信公众号 | [mp.md](./mp.md) | 1035 | 账号→粉丝→消息→素材→群发 | 6 |
| 仓储管理 | [wms.md](./wms.md) | 1991 | ASN→收货→上架→库存→拣货→发货 | 11 |
| **合计** | — | **7674** | — | **41** |

## 各模块概览

### 1. 商城(mall)

**核心定位**: 完整电商解决方案,基于 yudao mall + mall4j + litemall 设计

**核心子模块**: 商品中心(SPU/SKU)、交易中心(订单/售后/发货)、营销中心(优惠券/拼团/秒杀/砍价)、统计中心

**关键状态机**:
- 订单状态机(5 状态): 待付款 → 待发货 → 待收货 → 已完成 / 已取消
- 售后状态机(9 状态): 申请 → 同意 → 买家发货 → 卖家收货 → 待退款 → 完成
- 拼团/秒杀/优惠券/砍价活动状态机

**关键设计**:
- 库存扣减采用"下单锁定 lock_stock + 支付真扣"模式
- 订单明细采用快照设计,冗余商品信息
- 售后颗粒度到订单项,支持多次售后
- 金额统一用 int 分,避免 decimal 精度问题

### 2. 客户关系(crm)

**核心定位**: 销售漏斗全流程管理,基于 yudao crm + 简道云 + 销帮帮设计

**核心业务链**: 线索 → 客户(公海) → 商机 → 合同 → 回款

**关键状态机**:
- 线索状态机: 跟进状态 + 转化状态双轴
- 客户状态机: 含公海自动回收规则
- **商机状态机(三层模型)**: 状态组(status_type_id) + 阶段(status_id) + 终局(end_status: 赢单/输单/无效)
- 合同状态机: audit_status + flow_status 双轴
- 回款状态机: 审批 + 部分/全部回款

**关键设计**:
- 公海不是独立表: `owner_user_id IS NULL` 即公海
- 跟进记录通用设计: biz_type + biz_id 一张表服务 5 类对象
- 数据权限双层: 系统级 dept + CRM 级 owner_user_id + crm_permission
- 合同审批对接 BPM(可降级为单字段 MVP)

### 3. 企业资源(erp)

**核心定位**: 采购/销售/库存/财务一体化,基于 yudao erp + RuoYi-ERP + 金蝶云星空设计

**核心业务链**: 采购订单 → 采购入库 → 库存 → 销售出库 → 销售订单 → 财务收付款

**关键状态机**(8 张):
- 通用审批状态机、采购订单、采购入库、销售出库
- 库存调拨(在途两步法)、库存盘点、付款单、收款单

**关键发现**:
- yudao 实际状态机极简(未审批/已审批 + 反审批),无草稿/完成/关闭等多状态
- "部分入库/已完成入库"等执行状态是通过 count 字段比较派生的(参考金蝶"执行状态"概念)
- 报告同时给出 yudao 极简版 + 推荐的完整版(7 状态)两套方案

**库存核算模型**: 移动加权平均(默认推荐)、FIFO、全月一次加权平均

### 4. 物联网(iot)

**核心定位**: 设备接入与管理的完整 IoT 平台,基于 yudao iot + JetLinks + ThingsBoard 设计

**核心业务链**: 产品 → 物模型 → 设备 → 消息 → 规则引擎 → 告警

**关键状态机**:
- 设备生命周期: 未激活 → 在线 ↔ 离线 → 禁用 → 删除
- 物模型: 属性(读/写/读写)、服务、事件
- 消息状态机: 已发送 → 已送达 → 已确认 → 失败 → 重试
- 规则引擎状态机: 含 duration/repeating 持久化
- 告警状态机: 待处理 → 处理中 → 已解决 / 已忽略(含 ESCALATE 升级)

**关键设计**:
- MQTT Topic 采用 JetLinks 风格: `/{productId}/{deviceId}/...`
- 统一消息模型(8 类): ReportPropertyMessage 等
- 告警规则状态持久化(duration/repeating 计时器),避免重启丢失
- 协议接入独立于 Erupt(双通道策略),Erupt 仅做配置与管理

### 5. 微信公众号(mp)

**核心定位**: 微信公众号本地管理后台,基于 yudao mp + WxJava + RuoYi-MP 设计

**核心业务链**: 账号 → 粉丝 → 消息 → 素材 → 群发

**关键状态机**:
- 粉丝状态: 未关注 ↔ 已关注(由微信事件驱动)
- 消息状态: 接收 → 自动回复/转人工/超时(48 小时窗口)
- 菜单状态: 草稿 ↔ 已发布 / 个性化菜单启用禁用
- 素材状态: 临时(3 天) vs 永久
- 自动回复状态: 关注回复/关键字回复/默认回复 + 匹配优先级
- 群发状态: 草稿 → 待发送 → 发送中 → 已发送/失败

**关键发现**:
- yudao 群发与统计未做本地持久化,需补 `mp_mass_message` 表
- yudao 的 mp_menu 未落个性化菜单 matchrule 字段
- 粉丝状态机完全由微信事件推送驱动,不能本地手动改状态
- 客服消息 48 小时窗口限制(`45015` 错误码)

### 6. 仓储管理(wms)

**核心定位**: 完整仓储管理系统(芋道无此模块),基于 RuoYi-WMS + wms-ruoyi + 金蝶/SAP EWM 设计

**核心业务链**: ASN → 收货 → 质检 → 上架 → 库存 → 拣货 → 复核 → 打包 → 发货

**关键状态机**(11 张):
- ASN、收货、质检、上架
- 出库通知、波次、拣货
- 调拨、盘点、加工
- **库存四态**: 可用 / 锁定 / 在途 / 冻结

**WMS 与 ERP 库存的本质区别**:
| 维度 | yudao-erp stock | WMS |
|---|---|---|
| 库存状态 | 单态(count) | 四态(可用/锁定/在途/冻结) |
| 库位管理 | 无 | 三级(仓库-库区-库位) |
| 批次/序列号 | 无 | 完整追踪 |
| 入库单据 | 单张 | 四单据(ASN/收货/质检/上架) |
| 出库单据 | 单张 | 六单据(通知/波次/拣货/复核/打包/发货) |
| 分配策略 | 无 | FIFO/LIFO/FEFO/批次指定 |

**关键设计**:
- 入库四单据模式(ASN → 收货 → 质检 → 上架)是行业标配
- 库存余额表四态模型 + append-only 流水表
- 批次管理支持 FEFO(先到期先出)
- 波次引擎: 先波后分 / 先分后波
- ABC 分类管理(分类价值分级)

## 通用设计模式

### 状态机实现模式(Erupt)

所有模块的状态机统一采用以下模式:

```java
@Erupt(name = "订单")
@Table(name = "mall_trade_order")
@Entity
public class TradeOrder extends BaseModel {

    @EruptField(
        views = @View(title = "订单状态"),
        edit = @Edit(title = "订单状态",
            type = EditType.CHOICE,
            choiceType = @ChoiceType(
                fetchHandler = ChoiceFetchHandler.class,
                fetchParams = "{\"field\":\"status\"}"
            ))
    )
    private Integer status;  // 0=待付款 10=待发货 20=待收货 30=已完成 40=已取消

    // 状态机操作按钮(不直接编辑 status 字段)
    // 通过 DataProxy.beforeUpdate 禁止表单直接修改 status
}
```

状态变更通过 `@RowOperation` + `OperationHandler` 实现:

```java
@RowOperation(
    code = "SHIP", title = "发货",
    icon = "fa fa-truck",
    operationHandler = ShipOrderHandler.class
)
```

### 主子表模式

订单-明细、ASN-明细等主子表统一用 `@OneToMany` + `EditType.TAB_TABLE`:

```java
@EruptField(
    edit = @Edit(
        title = "订单明细",
        type = EditType.TAB_TABLE
    )
)
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
private List<TradeOrderItem> items;
```

### 树形结构模式

仓库-库区-库位、产品分类等树形结构用 `@Tree` + `@LinkTree`:

```java
@Erupt(name = "产品分类")
@Tree
@Table(name = "iot_product_category")
@Entity
public class ProductCategory extends TreeModel {
    // 自动继承 parent / sort 等字段
}
```

### 库存计算模式

涉及库存的模块(erp / wms / mall)统一采用:

1. **余额表** — 当前库存快照,通过 `@Version` 乐观锁或悲观锁保护
2. **流水表** — append-only,记录每笔出入库明细
3. **统一接口** — `StockService.changeStock(bizType, bizId, sku, qty, fromLocation, toLocation)` 事务方法
4. **DataProxy 副作用** — 单据状态变更时通过 DataProxy 触发库存联动

## 各模块 MVP 完整基础版边界(基本功能完整,后续可扩展)

> **定义**: MVP = 核心业务闭环完整(状态机全跑通,所有正常+异常分支覆盖) + 字段/子表预留扩展位,后续扩功能不需要改表结构。不是最简一表 CRUD,也不包含超重高级特性。

### CRM(客户关系)MVP

| 维度 | MVP 包含(6 实体 + 2 状态机 + 1 通用能力) | MVP 不含(后续扩展) |
|---|---|---|
| 实体 | crm_clue(线索)、crm_customer(客户)、crm_contact(联系人)、crm_business(商机)、crm_follow_up_record(跟进记录)、crm_backlog(待办视图) | crm_contract(合同)、crm_receivable(回款)、crm_product(产品)、crm_permission(权限成员表) |
| 状态机 | 线索(跟进+转化双轴)、客户(含公海自动回收三条件 `owner_time + lock_status=0 + deal_status=0`)、商机三层(status_type_id + status_id + end_status 赢单/输单/无效) | 合同(audit_status + flow_status)、回款(审批+部分/全部) |
| 通用能力 | 跟进记录通用 `biz_type + biz_id` 服务线索/客户/联系人/商机;公海视图(`owner_user_id IS NULL` 即公海);转移负责人 @RowOperation | crm_permission 团队 OWNER/WRITE/READ 三级协作权限;公海配置规则表 |
| 预留扩展 | 合同预留 `customer.contract_count/amount` 汇总字段;回款预留 `customer.receivable` 字段;商机预留 `crm_business_status` 表(部门级可配置) | — |
| 业务闭环 | 线索→分配→跟进→转化客户→商机推进→赢单/输单,完整走通 ✅ | — |

**预计工作量**: 4-6 天。

### ERP(企业资源)MVP

| 维度 | MVP 包含(10 实体 + 5 状态机) | MVP 不含(后续扩展) |
|---|---|---|
| 实体 | erp_product / erp_product_category / erp_unit / erp_warehouse / erp_supplier / erp_customer(基础档案) + erp_stock / erp_stock_move(库存) + erp_purchase_order + erp_purchase_in(采购) + erp_sale_order + erp_sale_out(销售) + erp_finance_payment + erp_finance_receipt(财务) | 库存调拨/盘点、采购退货/销售退货、批次/序列号、库存核算成本表、应收应付台账 |
| 状态机 | 采购订单(7 状态完整:0草稿→10待审批→20已审批→30部分入库→40已入库→50关闭/60作废)、采购入库(审批+入库)、销售订单(同采购)、销售出库(审批+出库)、付款/收款单(审批) | 调拨(在途两步法)、盘点(差异→审批→调整)、退货 |
| 库存模型 | 库存余额表单态 `count`(**预留 `locked / in_transit / frozen` 三字段**) + 库存流水 append-only + `@Version` 乐观锁 + 统一 `WmsStockService.changeStock()` 接口 | 批次 FEFO、序列号一物一码、分库位余额、ABC 分类 |
| 预留扩展 | 余额表预留四态字段;`stock_move.biz_type` 枚举预留调拨/盘点/退货 code;产品表预留 `batch_enabled/serial_enabled` 开关 | — |
| 业务闭环 | 采购→入库→库存↑→销售→出库→库存↓→收款/付款,完整进销存链路走通 ✅ | — |

**预计工作量**: 5-7 天。

### MALL(商城)MVP

| 维度 | MVP 包含(8 实体 + 3 状态机) | MVP 不含(后续扩展) |
|---|---|---|
| 实体 | product_spu / product_sku / product_category / product_brand(商品) + trade_order / trade_order_item(订单,快照设计) + trade_after_sale / trade_after_sale_log(售后) + pay_order(支付单) | 优惠券/拼团/秒杀/砍价/分销/满减/限时折扣、购物车、门店自提、快递对接 |
| 状态机 | 订单 5 状态完整(0待付款→10待发货→20待收货→30已完成/40已取消,超时/退款异常分支)+ 售后 9 状态完整 + 支付单(待支付→已支付→已退款) | 拼团/秒杀/砍价活动状态机 |
| 库存模式 | "下单锁定 lock_stock + 支付真扣",余额表同 ERP 复用(字段预留 lock_stock) | 分销返佣、库存分摊 |
| 快照设计 | `order_item` 冗余 sku_name / sku_price / sku_spec,订单详情不依赖商品表变更 | — |
| 预留扩展 | 营销预留 `order.promotion_id` / `order.discount_amount` 字段;购物车预留表结构空;分销预留 `item.commission` 字段 | — |
| 业务闭环 | 商品上架→下单→支付→发货→收货→完成,以及申请售后→退款,完整闭环 ✅ | — |

**预计工作量**: 5-7 天。

### WMS(仓储)MVP

> 说明:WMS 与 ERP 库存的区别在于**四态库存 + 库位精细化 + 多单据拆分**。MVP 做三单据拆分(不做六单据超重),后续补复核/打包/波次不重构表。

| 维度 | MVP 包含(10 实体 + 6 状态机) | MVP 不含(后续扩展) |
|---|---|---|
| 实体 | wms_warehouse / wms_zone / wms_location(三级树形) + wms_sku(产品) + wms_stock / wms_stock_move(四态库存) + wms_asn(收货通知) + wms_receipt(收货单) + wms_putaway(上架单) + wms_shipment(出库通知) + wms_pick(拣货单) | 质检单、波次、复核、打包、托盘、批次序列号、调拨单、盘点单、加工单、计费 |
| 状态机 | ASN→收货→上架(入三单据)、出库通知→拣货→发货(出三单据)、库存四态(可用/锁定/在途/冻结) | 波次、质检、调拨在途、盘点差异、加工 |
| 策略 | 分配策略 FIFO(按 created_at) + 上架策略同类就近(简单版) | FEFO/批次指定/路径优化/波次引擎 |
| 预留扩展 | `stock_move` 预留 `batch_id / serial_no`;`asn/receipt` 预留 `quality_id` 关联质检;出库预留 `wave_id / pack_id / check_id` | — |
| 业务闭环 | 商品进仓:ASN→收货→上架→库位库存↑;商品出仓:出库通知→拣货→库位库存↓→发货,完整闭环 ✅ | — |

**预计工作量**: 7-10 天。

### IoT(物联网)MVP

| 维度 | MVP 包含(8 实体 + 4 状态机) | MVP 不含(后续扩展) |
|---|---|---|
| 实体 | iot_product / iot_product_category(产品) + iot_thing_model(物模型:属性/服务/事件) + iot_device(设备) + iot_device_message(上行消息日志) + iot_alarm_rule(告警规则) + iot_alarm / iot_alarm_log(告警) | 规则引擎完整 TCA 模型、场景联动、OTA 升级、下行命令完整状态机、设备分组、固件管理 |
| 状态机 | 设备生命周期(未激活→在线↔离线→禁用→删除)、物模型三态(属性/服务/事件)、消息(发送→送达→确认→失败)、告警(待处理→处理中→已解决/已忽略) | 规则引擎状态机、OTA 状态机 |
| 通信架构 | **双通道**:Erupt 仅配置管理(实体+状态机)+ 独立 MQTT Listener 线程接 EMQX Broker + JetLinks 风格 Topic `/productId/deviceId/...` + 一机一密认证 | 一型一密、动态注册、集群 Broker |
| 预留扩展 | 设备表预留 `firmware_id / ota_status`;消息表预留 `direction`(上行/下行)与 `message_id` 去重;告警预留 `escalate_level` 升级等级 | — |
| 业务闭环 | 定义产品→物模型→注册设备→MQTT 上报属性→超阈值触发告警→告警处理/升级/忽略,完整闭环 ✅ | — |

**预计工作量**: 7-10 天(不含 MQTT Broker 部署,仅代码)。

### MP(微信公众号)MVP

| 维度 | MVP 包含(7 实体 + 5 状态机 + 1 外部接口) | MVP 不含(后续扩展) |
|---|---|---|
| 实体 | mp_account(账号) + mp_user + mp_user_tag(粉丝+标签) + mp_message(收发消息统一表) + mp_auto_reply(自动回复 3 类:关注/关键字/默认) + mp_menu(菜单) + mp_material(素材) | 群发消息、消息统计、图文分析、个性化菜单 matchrule、第三方平台授权 |
| 状态机 | 粉丝(未关注↔已关注,由微信订阅事件驱动)、消息(接收→自动回复/转人工/48h 超时)、菜单(草稿→已发布/撤回)、素材(临时 3 天 vs 永久)、自动回复(启用/禁用 + 匹配优先级) | 群发状态机(草稿→审批→发送→回执) |
| 外部接口 | 微信接入验证 endpoint + 事件/消息回调 endpoint + WxJava 封装 + Redis access_token 管理 + 粉丝同步 + 菜单同步微信 | 群发 API、客服消息(需要 ICP 备案) |
| 预留扩展 | 群发预留 `mp_mass_message` 空表结构;菜单表预留 `matchrule_json` 字段;消息预留 `kf_account` 客服号字段 | — |
| 业务闭环 | 接入公众号→粉丝关注→自动回复/关键字回复→自定义菜单草稿→发布→微信同步生效,完整闭环 ✅ | — |

**预计工作量**: 5-7 天(不含微信平台 ICP 备案/企业认证)。

---

## 实施优先级(重排:学习曲线友好 + 业务闭环独立)

> **核心思路**: 先用最轻的模块验证 erupt 注解模式,再逐步叠加复杂度。**两个 P0 互不依赖可并行**,后续扩展依赖表字段已预留。

| 阶段 | 模块(MVP) | 难度 | 依赖 | 为什么放这 |
|---|---|---|---|---|
| **P0(并行起步)** | **crm(MVP)** — 线索+客户+商机+跟进 | ⭐⭐ 最轻 | 无 | 纯数据流转,没有库存一致性/外部系统/事务一致性压力,业务人人懂。**用它验证三件事**:1) `@ChoiceType` + `@RowOperation` + `DataProxy.beforeUpdate` 状态机模式 2) `biz_type+biz_id` 通用跟进表 3) "我负责的/下属负责的"数据权限过滤 |
| **P0(并行起步)** | **erp(MVP)** — 产品+仓库+进销存+收付款 | ⭐⭐⭐ 中等 | 无 | 有简单库存(单态 count,预留四态)但没有库位/波次/策略。**用它验证三件事**:1) `@OneToMany`+TAB_TABLE 主子表 2) `StockService.changeStock()` 统一事务 + `@Version` 乐观锁 3) 审批→单据状态变更→DataProxy 副作用库存联动 |
| **P1(顺序)** | mall(MVP) — 商品+订单+售后 | ⭐⭐⭐ 中等 | 依赖 erp 库存服务(复用 changeStock 接口) | 订单状态机最完整,能验证"状态机 + 库存锁定 + 超时/退款异常分支"。放在 P1 因为它比 CRM/ERP 重一点但比 WMS 轻 |
| **P1(顺序)** | wms(MVP) — 三单据入 + 三单据出 + 四态库位库存 | ⭐⭐⭐⭐ 难 | 依赖 erp 基础档案产品表(不要重复建) | 多单据拆分 + 四态库存是最复杂的业务逻辑,留到 erupt 注解和状态机模式验证熟了再上,否则 80% 时间写 Service 事务,看不到 erupt 驱动效果 |
| **P2(按需)** | mp(MVP) — 账号+粉丝+消息+菜单+素材 | ⭐⭐⭐ 中等 | 无(独立模块) | 有外部微信 API 依赖和 48h/access_token 等坑,等主业务稳定了再调 |
| **P2(按需)** | iot(MVP) — 产品+物模型+设备+MQTT+告警 | ⭐⭐⭐⭐ 难 | 无(独立模块) | 有 Broker 部署和协议接入,和业务完全解耦,最后上 |
| **P3+(扩展)** | 各模块完整/高级版(合同回款/营销/调拨盘点/规则引擎群发…) | — | 对应 MVP 表字段已全部预留,**不用改表结构**直接加表 | — |

---

## 代办与进度跟踪(TODO)

> **更新方式**: 每次完成里程碑后修改对应单元格状态和备注。模块内实体按文档章节顺序编码。

### 总览表

| # | 模块(MVP) | 文档 | 实体编码 | 状态机 + @RowOperation | Service 副作用(库存/联动) | 冒烟测试(跑闭环单测) | 完成度 | 备注 |
|---|---|---|---|---|---|---|---|---|
| **1** | **crm — 线索/客户/商机/跟进** | ✅ [crm.md](./crm.md) | **✅ 已完成**(8 实体) | **✅ 已完成**(11 个状态机 Handler) | **✅ 已完成**(跟进刷新/赢单成交/公海回收) | **✅ 6/6**(H2 内存库) | **60%** | P0,并行起步 ✅ MVP 跑通,后续合同/回款/权限扩展 |
| 2 | erp — 产品/进销存/收付款 | ✅ [erp.md](./erp.md) | ⚪ 待开始 | ⚪ 待开始 | ⚪ 待开始(changeStock 接口) | ⚪ 待开始 | 0% | P0,并行起步,先做 changeStock 单测 |
| 3 | mall — 商品/订单/售后/支付 | ✅ [mall.md](./mall.md) | ⚪ 待开始 | ⚪ 待开始 | ⚪ 依赖 erp changeStock | ⚪ 待开始 | 0% | P1 |
| 4 | wms — 三入三出 + 四态库位库存 | ✅ [wms.md](./wms.md) | ⚪ 待开始 | ⚪ 待开始 | ⚪ 待开始(复用 erp 产品档案) | ⚪ 待开始 | 0% | P1 |
| 5 | mp — 账号/粉丝/消息/菜单/素材 | ✅ [mp.md](./mp.md) | ⚪ 待开始 | ⚪ 待开始 | ⚪ 待开始(WxJava 接入) | ⚪ 待开始 | 0% | P2,需要微信测试号 |
| 6 | iot — 产品/物模型/设备/告警 | ✅ [iot.md](./iot.md) | ⚪ 待开始 | ⚪ 待开始 | ⚪ 待开始(MQTT 接入) | ⚪ 待开始 | 0% | P2,需要 EMQX |

**状态**: ✅ 完成 / 🔵 进行中 / ⚪ 待开始 / ❌ 阻塞

---

### CRM(MVP)详细进度

| 子项 | 状态 | 代码位置 / 备注 |
|---|---|---|
| 实体 1 — crm_clue(线索) | ✅ | [CrmClue.java](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/entity/CrmClue.java) followUpStatus + transformStatus 双状态 + **@RowOperation 转化为客户** + [CrmClueStateProxy](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/core/CrmClueStateProxy.java) 锁定表单直改 + [CrmClueTransformHandler](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/handler/CrmClueTransformHandler.java) |
| 实体 2 — crm_customer(客户) + 公海配置 | ✅ | [CrmCustomer.java](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/entity/CrmCustomer.java) 公海=`ownerUserId IS NULL`;[CrmCustomerPoolConfig.java](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/entity/CrmCustomerPoolConfig.java) 租户级单条配置;**5 个行按钮**(转移/认领/锁定/解锁/标记成交)+ [RecycleJob](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/job/CrmCustomerPoolRecycleJob.java) 定时回收 |
| 实体 3 — crm_contact(联系人) | ✅ | [CrmContact.java](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/entity/CrmContact.java) 关联客户ID;parentId/master 字段已预留树形扩展 |
| 实体 4 — 商机三层 | ✅ | [BusinessStatusType](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/entity/CrmBusinessStatusType.java) + [BusinessStatus](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/entity/CrmBusinessStatus.java) 配置表 + [CrmBusiness](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/entity/CrmBusiness.java) 三层状态机;**5 个行按钮**(推进/回退/赢单/输单/无效)+ [AdvanceHandler](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/handler/CrmBusinessAdvanceHandler.java) sort 序自动算下一阶段 + [EndHandler](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/handler/CrmBusinessEndHandler.java) 赢单**副作用**客户成交标记置 1 |
| 通用 5 — crm_follow_up_record + CrmFollowService | ✅ | [CrmFollowUpRecord.java](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/entity/CrmFollowUpRecord.java) biz_type+biz_id 通用;[CrmFollowService](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/core/CrmFollowService.java) 写跟进 + 刷新跟进主体最后跟进/内容/下次时间**汇总双写** |
| 通用 0 — 枚举字典 + 下拉 + 状态机基类 | ✅ | [CrmDictEnums.java](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/enums/CrmDictEnums.java) 11 种枚举 + [CrmEnumChoiceFetchHandler](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/core/CrmEnumChoiceFetchHandler.java) erupt ChoiceFetch 处理器 + [CrmStateDataProxy](file:///Users/herz/Documents/hrz/erupt01/src/main/java/xyz/herz/epaiproject/crm/core/CrmStateDataProxy.java) 状态字段防直接编辑基类 |
| 数据权限 — "我负责的/下属负责的" | ⚪ | 预留 erupt-security 接口权限叠加,下一步扩展(合同/回款阶段和权限成员表 crm_permission 一起做) |
| **闭环冒烟单测 6/6 ✅** | ✅ | [CrmSmokeTests.java](file:///Users/herz/Documents/hrz/erupt01/src/test/java/xyz/herz/epaiproject/crm/CrmSmokeTests.java) 内存 H2,全链路:① 线索→客户转化 ② 公海认领 ③ 锁定/解锁/成交 ④ 跟进记录刷新汇总 ⑤ 商机阶段推进→赢单(客户自动成交) ⑥ 公海扫描释放客户 |
| Erupt 2.0.3 注解 API 踩坑记录 | ✅ | `@ChoiceType.fetchHandlerParams`(不是 fetchParams);`@Filter("JPQL")`(不是 condition);`NumberType` 只有 `min/max`(没有 step);`@RowOperation` 显示表达式下沉到 Service 强校验(不在注解写);定时器用 `@Scheduled`(erupt-job 只有 EruptJob 实体模型,没有注解) |

---

### ERP(MVP)详细进度

| 子项 | 状态 | 负责人/备注 |
|---|---|---|
| 基础档案 — 产品/分类/单位/仓库/供应商/客户 | ⚪ | 先做这 6 张,验证 @Tree 分类 + 普通 CRUD |
| 库存层 — erp_stock + erp_stock_move | ⚪ | **先做 changeStock 单测**,验证:加/减/超卖(乐观锁异常)/流水 append-only |
| 采购 — 订单(7 状态机) + 入库单 | ⚪ | 审批→DataProxy 入库副作用 + `in_count/total_count` 派生执行状态 |
| 销售 — 订单 + 出库单 | ⚪ | 同上,出库扣库存 |
| 财务 — 付款单 + 收款单 | ⚪ | 简单版,不做应收应付台账,关联订单号 |
| 闭环冒烟单测 | ⚪ | 建供应商→采购订单→审批→入库→库存↑→销售订单→审批→出库→库存↓→收款 |
| WMS/MALL 共享接口确认 | ⚪ | changeStock(bizType) 接口和 WMS/MALL 的字段一一对应 |

---

### MALL(MVP)详细进度

| 子项 | 状态 | 负责人/备注 |
|---|---|---|
| 商品 — SPU + SKU + 分类 + 品牌 | ⚪ | 主子表(SPU→SKU) + 快照字段确认(订单明细要拷贝的列) |
| 订单 — trade_order + trade_order_item | ⚪ | 5 状态机 + @RowOperation(付款/发货/收货/取消) + 超时取消定时 |
| 支付 — pay_order | ⚪ | 回调接口空实现 + 幂等校验 |
| 售后 — after_sale + after_sale_log | ⚪ | 9 状态机 + 日志(变更前后 status + 操作人 + 时间) |
| 库存联动 | ⚪ | 下单 lock_stock + 支付真扣,调 erp StockService |
| 闭环冒烟单测 | ⚪ | 商品→下单→(锁库存)→支付→(扣库存)→发货→收货→完成→售后申请→退款→库存返还 |

---

### WMS(MVP)详细进度

| 子项 | 状态 | 负责人/备注 |
|---|---|---|
| 基础 — warehouse / zone / location(三级 @Tree) | ⚪ | 产品不要重建,关联 erp_product |
| 库存 — wms_stock(四态) + wms_stock_move | ⚪ | 余额按 仓库+库区+库位+sku 四组主键 |
| 入库三单 — ASN + 收货单 + 上架单 | ⚪ | ASN 明细下推收货→收货下推上架,每下推一次都是 @RowOperation |
| 出库三单 — 通知 + 拣货单 + 发货单 | ⚪ | 分配库存(FIFO 策略)→拣货扣可用+加锁定→发货锁定真扣 |
| 策略 — 简单 FIFO + 就近上架 | ⚪ | 预留策略接口,后续 FEFO/批次指定不改调用方 |
| 闭环冒烟单测 | ⚪ | ASN→收货→上架→库位库↑→出库通知→分配→拣货→库位库↓→发货 |

---

### MP(MVP)详细进度

| 子项 | 状态 | 负责人/备注 |
|---|---|---|
| 账号 + 接入验证 endpoint | ⚪ | 微信 Token 验证 + 消息加解密模式(明文先用,后续切加密) |
| 粉丝 + 标签 + 同步接口 | ⚪ | 关注/取关事件推送更新状态,不要手动改 status |
| 消息表 + 自动回复 3 类(关注/关键字/默认) | ⚪ | 消息 send_from 字段区分方向;48h 窗口限制超时判断 |
| 自定义菜单 — 草稿 → 发布同步微信 | ⚪ | 发布按钮触发 WxJava menuService.menuCreate |
| 素材 — 永久素材 CRUD + 上传 | ⚪ | 临时素材不持久化到 DB,3 天过期 |
| access_token Redis 管理 | ⚪ | WxJava 自带,确认 Redis 配置 |
| 闭环冒烟验证 | ⚪ | 测试号扫二维码→关注→自动回复→菜单发布→微信端生效 |

---

### IoT(MVP)详细进度

| 子项 | 状态 | 负责人/备注 |
|---|---|---|
| 产品 + 分类(@Tree) + 物模型(属性/服务/事件) | ⚪ | 物模型字段 JSON schema 文本存储 + 前端 JSON 编辑器 |
| 设备(完整生命周期状态机) + 激活码 | ⚪ | @RowOperation(禁用/启用/重置激活) |
| MQTT 接入独立线程 | ⚪ | 双通道,不写 Erupt 控制器;连接 EMQX,JetLinks 风格 Topic |
| 上行消息日志 + 物模型解析入库 | ⚪ | ReportPropertyMessage 等 8 类统一消息模型 |
| 告警规则(阈值) + 告警(4 状态机:待处理/处理中/解决/忽略) | ⚪ | duration/repeating 持久化,重启不丢计时器 |
| 闭环冒烟验证 | ⚪ | 定义产品物模型(温度属性,告警阈值 50)→注册设备→激活→MQTT 发温度=60→告警触发→处理→已解决 |

---

### 关键技术决策

1. **状态机复杂度**: MVP 一律用完整版(跳跃式枚举 0/10/20/30/40…,预留中间扩展位),不做 yudao 的极简两状态
2. **库存扣减时机**: mall 下单锁定 + 支付真扣;erp/wms 审批时真扣;统一 `changeStock(bizType…)` 事务入口
3. **多租户**: 直接 erupt-tenant,不用重复写
4. **数据权限**: crm "我负责的/下属负责的" 在 Service 层手写 WHERE 叠加 erupt-security
5. **工作流**: MVP 不接 BPM,合同/回款审批降级为 `audit_status` 单字段 + 下一级审批人字段;后续对接 erupt-designer 或 Flowable
6. **协议接入**: iot MQTT / mp 微信 API 独立于 Erupt 注解层写 Controller/Listener
7. **微信/IoT 延迟**: 由于有外部依赖,放在 P2,先把 P0/P1 的核心业务模式验证顺

## 版本记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-08-11 | 初版,完成 mall/crm/erp/iot/mp/wms 六个模块文档 |
| v1.1 | 2026-08-11 | ① 新增「各模块 MVP 完整基础版边界」(含/不含清单 + 字段预留扩展) ② 重排实施优先级(P0 crm+erp 并行起步) ③ 新增「代办与进度跟踪 TODO」(总览 + 每个模块详细里程碑) |

---

## 参考来源汇总

### 官方文档
- yudao 文档: https://doc.iocoder.cn/
- yudao 微服务版: https://cloud.iocoder.cn/
- Erupt 框架: https://www.erupt.xyz/

### 同类开源项目
- mall4j: https://github.com/GUWENVOVO/mall4j
- litemall: https://gitee.com/linlinjava/litemall
- RuoYi-CRM / RuoYi-ERP / RuoYi-WMS / RuoYi-MP / RuoYi-IoT
- ThingsBoard: https://thingsboard.io/
- JetLinks: https://www.jetlinks.com/
- WxJava: https://github.com/Wechat-Group/WxJava

### 行业标准
- BPMN 2.0(工作流)
- GS1(条码/RFID 标准)
- ISO/IEC 30141(IoT 参考架构)

各模块详细的参考来源见对应文档末尾"参考来源"章节。
