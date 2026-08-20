# ep-business (erupt-cloud)

基于 [Erupt](https://www.erupt.xyz/) 框架的企业级业务系统，6 大业务模块 + 打包层，Maven 多模块架构。聚焦数据状态流转（状态机），而非简单 CRUD。

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 | LTS 版本 |
| Spring Boot | 3.5.15 | 基础框架 |
| Erupt | 2.0.3 | 注解驱动的后台框架，零前端代码 |
| H2 | 嵌入式 | 开发/测试用，可切换 MySQL/PostgreSQL |
| JPA/Hibernate | Spring Boot 管理 | ORM 层 |
| Lombok | 全局启用 | 实体类 `@Getter @Setter` 简化 |

## 架构设计

### 模块结构

```
ep-business-parent              ← 父 pom，版本收敛 + dependencyManagement
├── ep-module-crm               ← 客户关系：线索/客户/商机/合同/回款
├── ep-module-erp               ← 企业资源：产品/库存/采购/销售/财务
├── ep-module-mall              ← 商城：SPU/SKU/订单/售后/支付
├── ep-module-wms               ← 仓储：仓库/库区/库位/四态库存/ASN/拣货
├── ep-module-mp                ← 微信公众号：账号/粉丝/消息/菜单/素材
├── ep-module-iot               ← 物联网：产品/物模型/设备/告警
├── ep-module-landing           ← 落地页：amis 拖拽 H5 + 短链接 + 留资闭环
└── ep-boot                     ← 打包层，只装配不写业务
```

### 核心原则

- **无公共模块** — 各业务模块完全自包含，无 cross-module 直接依赖
- **跨模块通信** — 调用方模块内定义 Facade 接口 + `@Autowired(required=false)` 可选注入，Facade 不可用时 `log.debug` 跳过，模块可独立编译/测试
- **状态机驱动** — 每个核心实体都有状态流转，禁止直接编辑 status 字段，必须通过行按钮触发

### 状态机三件套

所有模块的状态机统一采用以下模式：

```java
// 1. 枚举定义
public class OrderDictEnums {
    public static final int STATUS_PENDING_PAY = 0;
    public static final int STATUS_PENDING_SHIP = 10;
    // ...
}

// 2. 下拉处理器 — 注册到 Erupt ChoiceType
@Component
public class OrderEnumChoiceFetchHandler implements ChoiceFetchHandler<String> {
    private static final Map<String, String> LOOKUP = new LinkedHashMap<>();
    static {
        LOOKUP.put("0", "待付款");
        LOOKUP.put("10", "待发货");
        // ...
    }
    @Override public List<Pair<String, String>> fetch(String[] params) { ... }
}

// 3. 状态锁 — DataProxy 禁止表单直改 status
public abstract class OrderStateDataProxy implements DataProxy<BaseModel> {
    @Override
    public void beforeUpdate(BeforeUpdateModel model) {
        // 不允许前端直接修改 status
    }
}

// 4. 行按钮 Handler — 状态变更的唯一入口
@RowOperation(code = "SHIP", title = "发货", operationHandler = ShipOrderHandler.class)
```

### 跨模块 Facade 示例

```java
// 接口定义在调用方模块 (ep-module-mall)
package xyz.herz.ep.mall.facade;
public interface MallStockFacade {
    void lockStock(Long skuId, int qty);
    void deductStock(Long skuId, int qty);
    void returnStock(Long skuId, int qty);
}

// Handler 中可选注入
@Autowired(required = false)
private MallStockFacade stockFacade;

public void ship(Order order) {
    if (stockFacade != null) {
        stockFacade.deductStock(order.getSkuId(), order.getQty());
    } else {
        log.debug("MallStockFacade not available, skip stock deduction");
    }
}
```

### 库存模型

| 模块 | 库存类型 | 核心类 | 说明 |
|------|----------|--------|------|
| ERP | 单态 | `erp.inventory.InventoryChangeFacade` | 余额表 + append-only 流水表，`@Version` 乐观锁 |
| WMS | 四态 | `wms.entity.WmsStock` | 可用/锁定/在途/冻结，三级库位（仓库-库区-库位） |

## 各模块概览

| 模块 | 核心实体 | 状态机数 | 测试 |
|------|----------|----------|------|
| **CRM** | 线索→客户(公海)→商机→合同→回款 | 5 | 8/8 ✅ |
| **ERP** | 采购→入库→库存→销售→收付款 | 8 | 6/6 ✅ |
| **Mall** | SPU/SKU→订单→售后→支付 | 6 | 4/4 ✅ |
| **WMS** | ASN→收货→上架→四态库存→拣货→发货 | 11 | 7/7 ✅ |
| **MP** | 账号→粉丝→消息→菜单→素材 | 6 | 5/5 ✅ |
| **IoT** | 产品→物模型→设备→消息→告警 | 5 | 5/5 ✅ |
| **Landing** | amis 拖拽 H5→短链→留资 | 3 | 8/8 ✅ |

## 快速开始

### 编译 & 打包

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn clean package -DskipTests
# 产物：ep-boot/target/ep-business.jar
java -jar ep-boot/target/ep-business.jar
```

### 运行测试

```bash
mvn test
# 55 个测试全部通过
```

### 测试架构

| 模块 | 测试数 | 启动类 | 测试类 |
|------|--------|--------|--------|
| CRM | 8 | `CrmTestApplication` | `CrmSmokeTests` |
| ERP | 6 | `ErpTestApplication` | `ErpSmokeTests` |
| Mall | 4 | `MallTestApplication` | `MallSmokeTests` |
| WMS | 7 | `WmsTestApplication` | `WmsSmokeTests` |
| MP | 5 | `MpTestApplication` | `MpSmokeTests` |
| IoT | 5 | `IotTestApplication` | `IotSmokeTests` |
| Landing | 8 | `LandingTestApplication` | `LandingSmokeTests` |
| Boot | 20 | `EruptBusinessApplication` | `SimpleEntityTest` |

- 每个模块独立 `XxxTestApplication`，`@SpringBootApplication` 只扫描本模块包
- `@SpringBootTest(webEnvironment = MOCK)` + H2 内存库
- 测试 scope 需加 `erupt-upms` 解决 `EruptSecurityInterceptor` 依赖
- 冒烟测试覆盖：CRUD + 状态机全分支（正常流转 + 异常拦截 + 审核/反审幂等）

## 参考项目

| 项目 | 模块参考 | 地址 |
|------|----------|------|
| [yudao](https://doc.iocoder.cn/) | CRM/ERP/Mall/MP/IoT 业务模型 | https://github.com/YunaiV/ruoyi-vue-pro |
| [Erupt](https://www.erupt.xyz/) | 注解驱动后台框架 | https://github.com/erupt-project/erupt |
| [mall4j](https://github.com/GUWENVOVO/mall4j) | 商城 SPU/SKU + 订单设计 | — |
| [litemall](https://gitee.com/linlinjava/litemall) | 商城售后 + 支付设计 | — |
| [RuoYi-ERP](https://gitee.com/jeecp/JeecgBoot) | ERP 采购/销售/库存 | — |
| [RuoYi-WMS](https://gitee.com/ruoyi-iot/ruoyi-wms) | WMS 四态库存 + 库位 | — |
| [JetLinks](https://www.jetlinks.com/) | IoT 物模型 + MQTT Topic | — |
| [ThingsBoard](https://thingsboard.io/) | IoT 设备生命周期 + 告警 | — |
| [WxJava](https://github.com/Wechat-Group/WxJava) | 微信公众号 API 封装 | — |
| [amis](https://aisuda.bce.baidu.com/amis/) | 落地页 H5 可视化渲染 | https://github.com/baidu/amis |
| [amis-editor](https://github.com/aisuda/amis-editor) | 落地页拖拽编辑器 | https://github.com/aisuda/amis-editor |

## 文档

- [业务模块设计文档](doc/README.md) — 6 模块完整状态机设计、MVP 边界、实施优先级
- [变更日志](doc/CHANGELOG.md) — 每次迭代详细变更
- [待办清单](doc/todo.md) — P0/P1/P2 任务状态

