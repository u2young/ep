# 落地页(Landing)模块设计文档

> **版本**: v1.2（设计文档 + 实现状态）  |  **更新**: 2026-09-12
> **实现状态**: ✅ **已完成** | 冒烟单测 **11/11 全部通过**
> **模块代码**: [ep-module-landing](../ep-module-landing/src/main/java/xyz/herz/ep/landing/)
> **测试代码**: [LandingSmokeTests.java](../ep-module-landing/src/test/java/xyz/herz/ep/landing/LandingSmokeTests.java)

## 实现状态

| 子项 | 状态 | 代码位置 |
|---|---|---|
| 模块骨架(pom + erupt-core/jpa/security/tpl + thymeleaf) | ✅ | [pom.xml](../ep-module-landing/pom.xml) |
| LandingPage 实体 + 状态机(草稿→发布→下线) | ✅ | [LandingPage.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingPage.java) |
| LandingTemplate 预设模板(空白/留资/产品) | ✅ | [LandingTemplate.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingTemplate.java) |
| LandingLead 留资记录 + LandingAccessLog UV 去重 | ✅ | [LandingLead.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingLead.java) |
| 短码生成器(Base62 自增 ID + 1M 偏移,6 位) | ✅ | [ShortCodeGenerator.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/shorturl/ShortCodeGenerator.java) |
| 短链 Controller /l/{code} → /p/{slug} + PV/UV 统计 | ✅ | [ShortUrlController.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/shorturl/ShortUrlController.java) |
| H5 渲染 Controller /p/{slug} + Thymeleaf 模板 | ✅ | [LandingRenderController.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/web/LandingRenderController.java) |
| magic-api 脚本(6 个接口) + Spring MVC 兜底 API | ✅ | [LandingApiController.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/web/LandingApiController.java) |
| amis-editor 入口页 + amis SDK CDN 集成 | ✅ | `static/landing-editor.html` + `templates/landing/render.html` |
| 模板初始化器(启动时插入 3 个预设模板) | ✅ | [LandingTemplateInitializer.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/config/LandingTemplateInitializer.java) |
| 冒烟单测 11 场景全闭环 | ✅ | [LandingSmokeTests.java](../ep-module-landing/src/test/java/xyz/herz/ep/landing/LandingSmokeTests.java) |
| 秒杀活动实体(LandingSeckill) + 状态机 DRAFT→ACTIVE→ENDED | ✅ | [LandingSeckill.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingSeckill.java) |
| 秒杀订单实体(LandingSeckillOrder) | ✅ | [LandingSeckillOrder.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingSeckillOrder.java) |
| 优惠券模板实体(LandingCoupon) + 状态机 DRAFT→ENABLED→DISABLED | ✅ | [LandingCoupon.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingCoupon.java) |
| 用户领券实体(LandingUserCoupon) | ✅ | [LandingUserCoupon.java](../ep-module-landing/src/main/java/xyz/herz/ep/landing/entity/LandingUserCoupon.java) |

详细实现追踪见 [todo.md](./todo.md#7-landing-落地页模块)。

---

## 核心业务链

amis 编辑器建页 → 发布(生成短码) → 短链/直链访问 → H5 渲染 → 表单留资 → Erupt 后台查看

## 状态机

```
        发布(生成短码,设publishTime)            下线(设offlineTime)
  ┌────┐ ─────────────────────────► ┌──────────┐ ──────────────────► ┌────────┐
  │ 草稿 │                            │ 已发布    │                     │ 已下线  │
  │  0   │ ◄──────────重新发布──────── │    10     │ ◄────重新发布(复用)─ │   20   │
  └────┘                              └──────────┘                     └────────┘
```

- 草稿(0) → 发布(10):生成 6 位短码(Base62),设 publishTime,清空 offlineTime
- 已发布(10) → 下线(20):设 offlineTime,短码保留但访问返回 410 Gone
- 已下线(20) → 发布(10):复用原短码(不重新生成),设 publishTime

## 实体设计

### LandingPage(落地页主表)

| 字段 | 类型 | 说明 |
|------|------|------|
| slug | VARCHAR(64) | 业务 slug,直链 /p/{slug},不可改 |
| name | VARCHAR(100) | 页面名称 |
| status | INT | 0 草稿 / 10 已发布 / 20 已下线 |
| content | TEXT | amis schema JSON |
| templateId | BIGINT | 模板 ID(可选) |
| shortCode | VARCHAR(16) | 6 位短码,发布时生成 |
| publishTime | DATETIME | 发布时间 |
| offlineTime | DATETIME | 下线时间 |
| pvCount | BIGINT | PV 访问计数 |
| uvCount | BIGINT | UV 去重计数 |

### LandingTemplate(预设模板)

| 字段 | 类型 | 说明 |
|------|------|------|
| name | VARCHAR(100) | 模板名称 |
| category | INT | 1 空白 / 2 留资 / 3 产品 / 4 海报 / 5 活动 |
| schema | TEXT | amis schema JSON |
| thumbUrl | VARCHAR(500) | 缩略图 URL |
| enabled | INT | 0 禁用 / 1 启用 |

### LandingLead(留资记录)

| 字段 | 类型 | 说明 |
|------|------|------|
| pageId | BIGINT | 落地页 ID |
| slug | VARCHAR(64) | 页面 slug |
| phone | VARCHAR(20) | 手机号 |
| extra | TEXT | 扩展字段 JSON |
| source | INT | 1 短链 / 2 直链 / 3 二维码 |
| clientIp | VARCHAR(64) | 提交 IP |
| userAgent | VARCHAR(500) | UA |
| submitTime | DATETIME | 提交时间 |

### LandingAccessLog(访问日志,UV 去重)

| 字段 | 类型 | 说明 |
|------|------|------|
| pageId | BIGINT | 落地页 ID |
| slug | VARCHAR(64) | 页面 slug |
| clientIp | VARCHAR(64) | 客户端 IP |
| clientFp | VARCHAR(64) | 指纹 MD5(IP+UA) |
| accessTime | DATETIME | 访问时间 |

唯一约束 `(page_id, client_ip, client_fp)` → UV 去重

## 短链接方案

### 短码生成:Base62 自增 ID

```
ShortCodeGenerator.generate(pageId):
  n = pageId + 1_000_000  (偏移防止低位太短)
  Base62 编码(0-9a-zA-Z),前缀补零到 6 位
```

- 无碰撞:自增 ID 本身唯一,零额外查询
- 可逆:decode 可还原 pageId
- 空间:6 位覆盖 568 亿,百万级落地页内必唯一

### 路由

- `/l/{code}` → 查 shortCode → 校验已发布 → 记访问日志 → `redirect:/p/{slug}`
- `/p/{slug}` → 渲染 H5(Thymeleaf + amis SDK)
- 普通 Spring MVC Controller,EruptSecurityInterceptor 不拦截(只拦 @EruptRouter),C 端免登

### PV/UV 统计

- PV:每次访问 `UPDATE landing_page SET pv_count = pv_count + 1`(原子自增)
- UV:`INSERT` 到 landing_access_log,唯一约束冲突即跳过,受影响行=1 则 `uv_count++`

## magic-api 接口

magic-api 是首选实现(脚本见 `src/main/resources/magic-api/api/landing/`),
Spring MVC 兜底 API(`/api/landing/*`)确保开箱即用。

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | /api/landing/page/slug/{slug} | 按 slug 取已发布页面 | 公开 |
| GET | /api/landing/template/list | 模板列表 | 公开 |
| POST | /api/landing/lead | 留资提交 | 公开 |
| GET | /api/landing/page/{id} | 按 ID 取页面 | 管理 |
| GET | /api/landing/seckill/{id} | 获取秒杀活动详情(含倒计时) | 公开 |
| POST | /api/landing/seckill/claim | 秒杀抢购 | 公开 |
| GET | /api/landing/coupon/validate?code=XXX | 校验优惠券码有效性 | 公开 |
| POST | /api/landing/coupon/claim | 领取优惠券 | 公开 |
| GET | /api/landing/coupon/list?phone=XXX | 查询用户领券列表 | 公开 |

magic-api web UI:http://localhost:8080/magic/web (erupt/erupt)

## amis SDK 集成

### 编辑器(amis-editor 6.x)

- 入口:`/static/landing-editor.html?id={pageId}`
- 从后端拉取页面 JSON → amis-editor 渲染拖拽画布 → Ctrl+S 保存
- SDK 来源:CDN `cdn.jsdelivr.net/npm/amis-editor@6.7.3`(可换本地)

### H5 渲染器(amis SDK 6.x)

- 入口:`/p/{slug}` → Thymeleaf `templates/landing/render.html`
- 后端注入 schema → `amisRequire('amis/embed').embed('#root', schema)`
- 拦截 amis form 的 fetcher → 改调 `/api/landing/lead`
- SDK 来源:本地 `static/amis/`(可 CDN 兜底)

## 测试

| 用例 | 说明 |
|------|------|
| template_crud | 模板 CRUD + 启停 |
| page_draft_save_and_load | 落地页草稿保存加载 |
| page_state_machine | 草稿→发布→下线→重新发布(复用短码) |
| short_code_deterministic_unique_reversible | 短码确定性+唯一性+可逆 |
| lead_submit_persisted | 留资闭环 |
| uv_dedup | UV 去重(唯一约束) |
| state_proxy_blocks_direct_status_edit | DataProxy 状态机守护 |
| publish_rejected_when_not_draft_or_offline | 非草稿态发布被拒 |

## 参考来源

- amis 文档: https://aisuda.bce.baidu.com/amis/
- amis-editor: https://github.com/aisuda/amis-editor
- amis-editor-demo: https://github.com/aisuda/amis-editor-demo
- magic-api: https://www.ssssssss.org/
