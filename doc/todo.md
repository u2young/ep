# 代办 & 跟进清单 (独立维护,与 README 分析解耦)

> **更新原则**:每次完成里程碑后只改这里的 ✅/⚪/🔵。  
> 文档分析放在 [README.md](./README.md) 和各模块 md,具体代码完成度在本页追踪。

---

## 0. 工程结构(P0:必须完成)

| # | 子项 | 优先级 | 状态 | 代码位置 / 备注 |
|---|---|---|---|---|
| 0.1 | 父 pom 版本收敛 + modules 聚合(common / crm module / boot) | P0 | ✅ | [根 pom.xml](../pom.xml) `packaging=pom`,3 个 module |
| 0.2 | common 层骨架(后续放通用 Facade/BaseEntity) | P0 | ✅ | [ep-business-common](../ep-business-common/pom.xml) |
| 0.3 | 每个业务模块独立 maven module(先落地 crm) | P0 | ✅ | [ep-module-crm](../ep-module-crm/pom.xml) 自包含 |
| 0.4 | boot 打包层(启动类 + application.yml, 只装配不写业务) | P0 | ✅ | [ep-boot](../ep-boot/pom.xml) 含 spring-boot-maven-plugin repackage,`ep-business.jar` 打包验证通过(4 reactor SUCCESS) |
| 0.5 | 模块间 Facade(erupt-cloud 拆分预留):mall→erp 库存、erp→wms 出入库 | P1 | ⚪ | 先在 common 层定义接口,各模块提供实现 |
| 0.6 | 旧 src/main/java 单模块残余清理(boot 层完成迁移后删除) | P0 | ✅ | 根 `src/` 已整体删除,避免 maven 默认编译单模块代码冲突 |

---

## 1. CRM 模块(P0:MVP 已跑通 60%,接下来做扩展)

总览:冒烟单测 **6/6 ✅**(线索→客户、公海认领、锁定/解锁/成交、跟进刷新、商机赢单→联动成交、公海回收扫描)

| # | 子项 | 优先级 | 状态 | 代码位置 / 备注 |
|---|---|---|---|---|
| 1.0 | 枚举字典 + erupt 下拉处理器 + 状态机 DataProxy 基类 | P0 | ✅ | `crm.enums.CrmDictEnums` 11 种;[core/](../ep-module-crm/src/main/java/...) |
| 1.1 | 线索实体 + 双状态 + 【转化为客户】行按钮 + 状态直改锁定 | P0 | ✅ | [CrmClue.java](../ep-module-crm/src/main/java/.../entity/CrmClue.java) + CrmClueTransformHandler |
| 1.2 | 客户实体(公海=ownerUserId IS NULL) + 5 个行按钮(转移/认领/锁定/解锁/标记成交) | P0 | ✅ | CrmCustomer + CrmCustomerPoolConfig |
| 1.3 | 联系人实体(关联客户 ID,预留 master/parentId 树形扩展) | P0 | ✅ | CrmContact |
| 1.4 | 商机三层配置表(status_type + status) + 5 个行按钮(推进/回退/赢单/输单/无效) | P0 | ✅ | CrmBusinessStatusType / CrmBusinessStatus / CrmBusiness |
| 1.5 | 跟进记录(biz_type + biz_id 通用) + 跟进后双写主体(最后跟进/下次跟进/上次内容) | P0 | ✅ | CrmFollowUpRecord + CrmFollowService |
| 1.6 | 公海回收定时任务(租户级配置 + 扫描释放) | P0 | ✅ | CrmCustomerPoolRecycleJob (@Scheduled,非 erupt-job 注解) |
| 1.7 | 冒烟单测全链路闭环(H2 内存库 + RANDOM_PORT) | P0 | ✅ | [CrmSmokeTests.java](../ep-boot/src/test/java/.../crm/CrmSmokeTests.java) 6/6 |
| 1.8 | 数据权限:我负责的 / 我参与的 / 下属负责的 —— 成员表 + 权限 WHERE | P1 | ⚪ | 新增 `crm_team_member`(biz_type + biz_id + user_id + level) + 自定义 Filter/Security |
| 1.9 | 合同 + 回款(成交后闭环) | P1 | ⚪ | 状态:草稿→生效→作废 / 计划回款→已回款(部分/全部) + 对账 |
| 1.10 | CRM 仪表盘(线索量/转化率/商机漏斗/跟进活动热力) | P2 | ⚪ | erupt-tpl 聚合,后续 BI 模块 |
| 1.11 | 导入/导出/去重(线索 excel 导入 + 手机查重规则) | P2 | ⚪ | erupt-excel + Service 规则校验 |

---

## 2. ERP 模块(P0:MVP 完成)

总览:冒烟单测 **4/4 ✅**(主数据启停/上架、库存 Facade 入库/出库/幂等/不足异常、采购订单→入库审核→库存+回写 in_count、销售出库审核→扣库存→反审冲销)

| # | 子项 | 优先级 | 状态 | 文档参考 / 代码位置 |
|---|---|---|---|---|
| 2.0 | 枚举字典 + 下拉处理器 + 状态机 DataProxy 基类 + Facade 接口 | P0 | ✅ | [ErpDictEnums](../ep-module-erp/src/main/java/xyz/herz/ep/erp/enums/ErpDictEnums.java) / [InventoryChangeFacade](../ep-business-common/src/main/java/xyz/herz/ep/common/inventory/InventoryChangeFacade.java) |
| 2.1 | 主数据:分类(树)/单位/品牌/仓库/供应商/客户/账户 + 启停行按钮 | P0 | ✅ | `erp.entity.master.*` + `ErpMasterToggleHandler`(启用/停用/上下架, 停用连带下架) |
| 2.2 | 产品档案(SPU + N SKU + 多条码/批次/保质期, DataProxy 回绑 product) | P0 | ✅ | [ErpProduct.java](../ep-module-erp/src/main/java/xyz/herz/ep/erp/entity/product/ErpProduct.java), [ErpProductSku.java](../ep-module-erp/src/main/java/xyz/herz/ep/erp/entity/product/ErpProductSku.java) |
| 2.3 | 统一库存变更接口 `InventoryChangeFacade` 实现(余额+流水原子更新、幂等、不足抛异常) | P0 | ✅ | [ErpStockService.java](../ep-module-erp/src/main/java/xyz/herz/ep/erp/core/ErpStockService.java) |
| 2.4 | 采购(PO/PI)状态机 + 审核/反审/关闭/作废 行按钮,入库副作用调 changeStock 并回写 PO in_count | P0 | ✅ | ErpPurchaseOrder / ErpPurchaseIn + [ErpDocAuditHandler](../ep-module-erp/src/main/java/xyz/herz/ep/erp/handler/document/ErpDocAuditHandler.java) |
| 2.5 | 销售(SO/SaleOut)状态机 + 审核/反审/关闭/作废 行按钮,出库副作用扣库存,反审回补 | P0 | ✅ | ErpSaleOrder / ErpSaleOut + ErpDocAuditHandler 复用 |
| 2.6 | 其他入库 / 其他出库(盘盈亏/调拨) | P1 | ⚪ | |
| 2.7 | 收付款单 + 核销(应收/应付 vs 实收/实付 多对多核销表) | P1 | ⚪ | |
| 2.8 | 冒烟单测 4 场景全闭环(H2 + RANDOM_PORT),全工程 30/30 PASS | P0 | ✅ | [ErpSmokeTests.java](../ep-boot/src/test/java/xyz/herz/ep/boot/erp/ErpSmokeTests.java) 4/4 |

---

## 3. Mall 模块(P1:依赖 erp changeStock)

| # | 子项 | 优先级 | 状态 | 备注 |
|---|---|---|---|---|
| 3.1 | SPU/SKU + 分类 + 商品(上下架/推荐/新品) | P1 | ⚪ | SKU 库存 = erp 库存的视图(只读) |
| 3.2 | 订单状态机:待付款→待发货→待收货→已完成,已取消(5 种场景) | P1 | ⚪ | 待发货 = 自动生成 erp 销售出库草稿(通过 Facade) |
| 3.3 | 售后(退款/退货退款/换货)状态机 + 库存回滚 | P1 | ⚪ | |
| 3.4 | 支付流水(模拟先,后接真实三方) | P2 | ⚪ | |

---

## 4. WMS 模块(P1:复用 erp 产品档案)

| # | 子项 | 优先级 | 状态 | 备注 |
|---|---|---|---|---|
| 4.1 | 仓库/库区/库位(四态:空/有货/锁定/盘点中) | P1 | ⚪ | |
| 4.2 | 三入三出 6 种单据:采购入/退供入/调拨入,销退出/领料出/调拨出 | P1 | ⚪ | 复用 erp 产品档案,只加库位维度 |
| 4.3 | 库内操作:上架/拣货/移库/盘点 | P1 | ⚪ | 状态机:新建→作业中→完成/差异 |
| 4.4 | 与 ERP 对账:实时库位库存 + ERP 仓库库存一致性校验 | P2 | ⚪ | |

---

## 5. MP(微信公众号)模块(P2)

| # | 子项 | 优先级 | 状态 | 备注 |
|---|---|---|---|---|
| 5.1 | 账号 + 接入 token / AppSecret 管理 | P2 | ⚪ | 需要微信测试号 |
| 5.2 | 粉丝拉取 + 标签/分组 | P2 | ⚪ | WxJava |
| 5.3 | 自定义菜单、素材、群发(图文/Template) | P2 | ⚪ | |
| 5.4 | 接收消息/事件 + 自动回复(关键词/默认/fallback) | P2 | ⚪ | |

---

## 6. IoT 模块(P2)

| # | 子项 | 优先级 | 状态 | 备注 |
|---|---|---|---|---|
| 6.1 | 产品 + 物模型(属性/事件/服务)定义 | P2 | ⚪ | 需要 EMQX |
| 6.2 | 设备注册/激活/上下线 + 影子状态 | P2 | ⚪ | |
| 6.3 | 物模型数据流(T 表时序/属性快照/事件流水) | P2 | ⚪ | H2/PG 都要跑通 |
| 6.4 | 告警规则 + 告警事件 + 处理(确认/派发/关闭) | P2 | ⚪ | 状态机四态流转 |

---

## 7. 通用基础设施

| # | 子项 | 优先级 | 状态 | 备注 |
|---|---|---|---|---|
| 7.1 | 统一 `@EruptScan` 组件扫描策略:boot 层只扫 `xyz.herz.ep.**` 即可装配全部 module | P0 | ⚪ | 迁移后需要检查 crm 包扫描是否自动命中 |
| 7.2 | H2 测试库(内存) vs 生产库(H2 文件 / PG / MySQL)配置分层 | P0 | ✅ | `src/test/resources/application.yml` 已独立 |
| 7.3 | 统一枚举字典/状态下拉 ChoiceHandler 模板 | P0 | ✅ | CRM 完成 → 其他模块照抄 |
| 7.4 | 状态机 DataProxy 基类锁定表单直改(只能通过行按钮) | P0 | ✅ | CRM 完成 → 其他模块照抄 |
| 7.5 | @RowOperation 显示表达式 vs Service 强校验双保险 | P0 | ✅ | CRM 完成 → 其他模块照抄 |
