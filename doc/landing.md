# 落地页(Landing)模块设计文档

> **模块**: ep-module-landing
> **版本**: v1.0.0
> **定位**: 基于 amis 的可视化拖拽 H5 落地页 + 短链接 + magic-api 接口 + 留资闭环

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
