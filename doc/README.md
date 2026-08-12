# 业务模块功能设计文档

> **版本**: v1.2
> **更新时间**: 2026-08-12
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

| 维度 | MVP 包含 | MVP 不含(后续扩展) |
|---|---|---|
| 实体 | crm_clue(线索)、crm_customer(客户)、crm_contact(联系人)、crm_business(商机)、crm_follow_up_record(跟进记录)、crm_backlog(待办视图) | crm_contract(合同)、crm_receivable(回款)、crm_product(产品)、crm_permission(权限成员表) |
| 状态机 | 线索(跟进+转化双轴)、客户(含公海自动回收三条件 `owner_time + lock_status=0 + deal_status=0`)、商机三层(status_type_id + status_id + end_status 赢单/输单/无效) | 合同(audit_status + flow_status)、回款(审批+部分/全部) |

### ERP(企业资源)MVP

| 维度 | MVP 包含 | MVP 不含(后续扩展) |
|---|---|---|
| 实体 | erp_product / erp_product_category / erp_unit / erp_warehouse / erp_supplier / erp_customer(基础档案) + erp_stock / erp_stock_move(库存) + erp_purchase_order + erp_purchase_in(采购) + erp_sale_order + erp_sale_out(销售) + erp_finance_payment + erp_finance_receipt(财务) | 库存调拨/盘点、采购退货/销售退货、批次/序列号、库存核算成本表、应收应付台账 |
| 状态机 | 采购订单(7 状态完整:0草稿→10待审批→20已审批→30部分入库→40已入库→50关闭/60作废)、采购入库(审批+入库)、销售订单(同采购)、销售出库(审批+出库)、付款/收款单(审批) | 调拨(在途两步法)、盘点(差异→审批→调整)、退货 |

### MALL(商城)MVP

| 维度 | MVP 包含 | MVP 不含(后续扩展) |
|---|---|---|
| 实体 | product_spu / product_sku / product_category / product_brand(商品) + trade_order / trade_order_item(订单,快照设计) + trade_after_sale / trade_after_sale_log(售后) + pay_order(支付单) | 优惠券/拼团/秒杀/砍价/分销/满减/限时折扣、购物车、门店自提、快递对接 |
| 状态机 | 订单 5 状态完整(0待付款→10待发货→20待收货→30已完成/40已取消,超时/退款异常分支)+ 售后 9 状态完整 + 支付单(待支付→已支付→已退款) | 拼团/秒杀/砍价活动状态机 |

### WMS(仓储)MVP

> 说明:WMS 与 ERP 库存的区别在于**四态库存 + 库位精细化 + 多单据拆分**。MVP 做三单据拆分(不做六单据超重),后续补复核/打包/波次不重构表。

| 维度 | MVP 包含 | MVP 不含(后续扩展) |
|---|---|---|
| 实体 | wms_warehouse / wms_zone / wms_location(三级树形) + wms_sku(产品) + wms_stock / wms_stock_move(四态库存) + wms_asn(收货通知) + wms_receipt(收货单) + wms_putaway(上架单) + wms_shipment(出库通知) + wms_pick(拣货单) | 质检单、波次、复核、打包、托盘、批次序列号、调拨单、盘点单、加工单、计费 |
| 状态机 | ASN→收货→上架(入三单据)、出库通知→拣货→发货(出三单据)、库存四态(可用/锁定/在途/冻结) | 波次、质检、调拨在途、盘点差异、加工 |

### IoT(物联网)MVP

| 维度 | MVP 包含 | MVP 不含(后续扩展) |
|---|---|---|
| 实体 | iot_product / iot_product_category(产品) + iot_thing_model(物模型:属性/服务/事件) + iot_device(设备) + iot_device_message(上行消息日志) + iot_alarm_rule(告警规则) + iot_alarm / iot_alarm_log(告警) | 规则引擎完整 TCA 模型、场景联动、OTA 升级、下行命令完整状态机、设备分组、固件管理 |
| 状态机 | 设备生命周期(未激活→在线↔离线→禁用→删除)、物模型三态(属性/服务/事件)、消息(发送→送达→确认→失败)、告警(待处理→处理中→已解决/已忽略) | 规则引擎状态机、OTA 状态机 |

### MP(微信公众号)MVP

| 维度 | MVP 包含 | MVP 不含(后续扩展) |
|---|---|---|
| 实体 | mp_account(账号) + mp_user + mp_user_tag(粉丝+标签) + mp_message(收发消息统一表) + mp_auto_reply(自动回复 3 类:关注/关键字/默认) + mp_menu(菜单) + mp_material(素材) | 群发消息、消息统计、图文分析、个性化菜单 matchrule、第三方平台授权 |
| 状态机 | 粉丝(未关注↔已关注,由微信订阅事件驱动)、消息(接收→自动回复/转人工/48h 超时)、菜单(草稿→已发布/撤回)、素材(临时 3 天 vs 永久)、自动回复(启用/禁用 + 匹配优先级) | 群发状态机(草稿→审批→发送→回执) |

---

## 实施优先级(重排:学习曲线友好 + 业务闭环独立)

> **核心思路**: 先用最轻的模块验证 erupt 注解模式,再逐步叠加复杂度。**两个 P0 互不依赖可并行**,后续扩展依赖表字段已预留。

| 阶段 | 模块(MVP) | 依赖 | 为什么放这 |
|---|---|---|---|
| **P0(并行起步)** | **crm(MVP)** — 线索+客户+商机+跟进 | 无 | 纯数据流转,没有库存一致性/外部系统/事务一致性压力,业务人人懂。**用它验证三件事**:1) `@ChoiceType` + `@RowOperation` + `DataProxy.beforeUpdate` 状态机模式 2) `biz_type+biz_id` 通用跟进表 3) "我负责的/下属负责的"数据权限过滤 |
| **P0(并行起步)** | **erp(MVP)** — 产品+仓库+进销存+收付款 | 无 | 有简单库存(单态 count,预留四态)但没有库位/波次/策略。**用它验证三件事**:1) `@OneToMany`+TAB_TABLE 主子表 2) `StockService.changeStock()` 统一事务 + `@Version` 乐观锁 3) 审批→单据状态变更→DataProxy 副作用库存联动 |
| **P1(顺序)** | mall(MVP) — 商品+订单+售后 | 依赖 erp 库存服务(复用 changeStock 接口) | 订单状态机最完整,能验证"状态机 + 库存锁定 + 超时/退款异常分支"。放在 P1 因为它比 CRM/ERP 重一点但比 WMS 轻 |
| **P1(顺序)** | wms(MVP) — 三单据入 + 三单据出 + 四态库位库存 | 依赖 erp 基础档案产品表(不要重复建) | 多单据拆分 + 四态库存是最复杂的业务逻辑,留到 erupt 注解和状态机模式验证熟了再上,否则 80% 时间写 Service 事务,看不到 erupt 驱动效果 |
| **P2(按需)** | mp(MVP) — 账号+粉丝+消息+菜单+素材 | 无(独立模块) | 有外部微信 API 依赖和 48h/access_token 等坑,等主业务稳定了再调 |
| **P2(按需)** | iot(MVP) — 产品+物模型+设备+MQTT+告警 | 无(独立模块) | 有 Broker 部署和协议接入,和业务完全解耦,最后上 |
| **P3+(扩展)** | 各模块完整/高级版(合同回款/营销/调拨盘点/规则引擎群发…) | 对应 MVP 表字段已全部预留,**不用改表结构**直接加表 | — |

---

## 关键技术决策

1. **状态机复杂度**: MVP 一律用完整版(跳跃式枚举 0/10/20/30/40…,预留中间扩展位),不做 yudao 的极简两状态
2. **库存扣减时机**: mall 下单锁定 + 支付真扣;erp/wms 审批时真扣;统一 `changeStock(bizType…)` 事务入口
3. **多租户**: 直接 erupt-tenant,不用重复写
4. **数据权限**: crm "我负责的/下属负责的" 在 Service 层手写 WHERE 叠加 erupt-security
5. **工作流**: MVP 不接 BPM,合同/回款审批降级为 `audit_status` 单字段 + 下一级审批人字段;后续对接 erupt-designer 或 Flowable
6. **协议接入**: iot MQTT / mp 微信 API 独立于 Erupt 注解层写 Controller/Listener
7. **微信/IoT 延迟**: 由于有外部依赖,放在 P2,先把 P0/P1 的核心业务模式验证顺

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
