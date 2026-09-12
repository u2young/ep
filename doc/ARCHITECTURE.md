# erupt-plus 架构与业务模块设计参考

> **版本**: v2.1
> **更新时间**: 2026-09-12
> **技术栈**: Java 21 + Spring Boot 3.5.15 + Erupt 2.1.0 + JPA/Hibernate + H2

## 概述

本文档为 erupt-plus 项目的业务模块设计参考，记录各模块的状态机设计、Erupt 适配模式和关键决策。项目已从最初 7 个设计文档演进为 **18 个完整实现的业务模块**，所有模块均已通过独立 H2 内存库冒烟测试。

详细模块设计见各子文档：[crm.md](./crm.md) · [mall.md](./mall.md) · [erp.md](./erp.md) · [wms.md](./wms.md) · [iot.md](./iot.md) · [mp.md](./mp.md) · [landing.md](./landing.md)

当前代码实现状态追踪见 [todo.md](./todo.md)。

---

## 模块索引

| 模块 | 核心链路 | 状态机 | 单测 | 设计文档 |
|---|---|---|---|---|
| **CRM** | 线索 → 客户(公海) → 商机 → 合同 → 回款 | 5 | 26 ✅ | [crm.md](./crm.md) |
| **ERP** | 产品/仓库主数据 → 采购订单 → 入库 → 销售订单 → 出库 → 收付款 | 8 | 4 ✅ | [erp.md](./erp.md) |
| **Mall** | SPU/SKU 商品 → 订单 → 支付 → 售后 | 6 | 4 ✅ | [mall.md](./mall.md) |
| **WMS** | ASN → 收货 → 上架 → 拣货 → 发货 + 四态库存 | 11 | 5 ✅ | [wms.md](./wms.md) |
| **IoT** | 产品 → 物模型 → 设备 → 消息 → 告警 | 5 | 5 ✅ | [iot.md](./iot.md) |
| **MP** | 公众号账号 → 粉丝 → 消息 → 自动回复 → 菜单 → 素材 | 6 | 5 ✅ | [mp.md](./mp.md) |
| **Landing** | H5 落地页(amis 拖拽)→短链→留资→秒杀→优惠券 | 5 | 11 ✅ | [landing.md](./landing.md) |
| **Fin** | 科目树 → 凭证 → 审核过账 → 试算平衡 → 发票 | 4 | 6 ✅ | — |
| **Mfg** | BOM → 工序 → 工单(草稿→投产→完工) → 领料/报工 | 3 | 6 ✅ | — |
| **Proj** | 项目模板 → 项目 → 任务 → 时间记录 → 现金流 | 3 | 6 ✅ | — |
| **Sup** | 工单(PENDING→IN_PROGRESS→RESOLVED) + SLA 达成率 | 3 | 6 ✅ | — |
| **Ast** | 资产类别树 → 资产卡片 → 折旧明细 → 变动记录 | 3 | 6 ✅ | — |
| **HR** | 部门(树) → 职位 → 员工 → 考勤 → 请假 | 4 | 6 ✅ | — |
| **Pay** | 薪资组件 → 结构模板 → 工资单 → GL 过账凭证 | 2 | 6 ✅ | — |
| **Qal** | 检验标准 → 质检单 → N/C 不合格品 → 客户反馈 | 3 | 6 ✅ | — |
| **Pur** | 请购单 → 询价单 → 供应商报价 → 收货单 | 4 | 6 ✅ | — |
| **Stk** | 出入库单(类型:入库/出库/移库/生产/翻包) → 批次/序列号 → 盘点 | 4 | 6 ✅ | — |
| **Sal** | 报价单 → 销售订单 → 发货单 + 销售员/合作商 | 4 | 6 ✅ | — |
| **合计** | — | **44+** | **106+** ✅ | — |

---

## 通用设计模式

### 状态机实现(Erupt)

所有模块统一采用以下模式，status 字段锁定禁止表单直接编辑，状态变更仅允许通过行按钮触发：

```java
@Erupt(name = "订单")
@Table(name = "mall_trade_order")
@Entity
public class TradeOrder extends BaseModel {

    // status 字段通过 ChoiceType 下拉展示，表单编辑被 DataProxy 锁定
    @EruptField(
        views = @View(title = "订单状态"),
        edit = @Edit(title = "订单状态", type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = ChoiceFetchHandler.class,
                fetchHandlerParams = "status")))
    private Integer status; // 0=待付款 10=待发货 20=待收货 30=已完成 40=已取消
}
```

状态变更通过 `@RowOperation` + Handler 实现：

```java
@RowOperation(code = "SHIP", title = "发货", icon = "fa fa-truck",
    operationHandler = ShipOrderHandler.class)
```

### 跨模块解耦

各模块完全自包含，跨模块调用通过 Facade 接口 + `@Autowired(required=false)` 可选注入实现：

```java
// mall 模块定义接口
public interface MallStockFacade { void returnStock(Long orderId); }

// erp 模块注入(可为 null，mall 未启用时不影响 erp 启动)
@Autowired(required = false)
private MallStockFacade mallStockFacade;
```

### 主子表模式

订单-明细、ASN-明细等统一使用 `@OneToMany` + `EditType.TAB_TABLE`：

```java
@EruptField(edit = @Edit(title = "订单明细", type = EditType.TAB_TABLE))
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
private List<TradeOrderItem> items;
```

### 树形结构模式

仓库-库区-库位、产品分类等使用 `@Tree` + `TreeModel` 基类：

```java
@Erupt(name = "产品分类")
@Tree
public class ProductCategory extends TreeModel {
    // 自动继承 parent / sort 字段
}
```

### 库存核算模式

涉及库存的模块(erp / wms / mall / stk)统一采用：
1. **余额表** — 当前库存快照，`@Version` 乐观锁保护
2. **流水表** — append-only，记录每笔出入库明细
3. **统一接口** — `InventoryChangeFacade.changeStock(bizType, bizId, sku, qty)` 事务方法
4. **DataProxy 副作用** — 单据状态变更时通过 DataProxy 触发库存联动

---

## 关键技术决策

| 决策 | 说明 |
|---|---|
| 状态机完整性 | MVP 一律用完整版(跳跃枚举 0/10/20/30/40)，预留中间扩展位，不做极简两状态 |
| 库存扣减时机 | mall 下单锁定+支付真扣；erp/wms/stk 审批时真扣；统一 `changeStock()` 事务入口 |
| 公海判断 | CRM 核心字段 `ownerUserId IS NULL` 即公海，不需独立表 |
| 多租户 | 直接复用 erupt-tenant，各模块不重复实现 |
| 工作流 | MVP 不接 BPM，审批降级为 `audit_status` 单字段 + 下一级审批人字段 |
| 外部依赖 | IoT MQTT / MP 微信 API 独立于 Erupt 注解层写 Controller/Listener，P2 阶段接入 |
| 测试隔离 | 每个模块独立 H2 内存库 + `@Transactional @Rollback`，不依赖 ep-boot 启动层 |
| 报表与打印 | ep-boot 层通过 `EruptReportInitializer` / `EruptPrintInitializer` 集中注册 |

---

## 参考来源

### 官方文档
- [Erupt 框架](https://www.erupt.xyz/)
- [yudao 文档](https://doc.iocoder.cn/)
- [yudao 微服务版](https://cloud.iocoder.cn/)

### 同类开源项目
- [RuoYi-Vue-Pro](https://github.com/YunaiV/ruoyi-vue-pro) — CRM/ERP 业务模型参考
- [mall4j](https://github.com/GUWENVOVO/mall4j) — 电商参考
- [litemall](https://gitee.com/linlinjava/litemall) — 轻量商城参考
- [JetLinks](https://www.jetlinks.com/) — IoT 设备管理参考
- [ThingsBoard](https://thingsboard.io/) — IoT 平台参考
- [WxJava](https://github.com/Wechat-Group/WxJava) — 微信公众号 SDK
- [RuoYi-WMS](https://gitee.com/y_project/RuoYi-WMS) — 仓储管理参考

### 行业标准
- BPMN 2.0 (工作流)
- GS1 (条码/RFID)
- ISO/IEC 30141 (IoT 参考架构)

各模块详细参考来源见对应子文档末尾。
