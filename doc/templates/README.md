# 落地页模板合集

存放用于测试和快速导入的落地页 amis schema JSON 文件。

## 使用方法

### 方式一：直接在 Erupt 后台导入

1. 登录 Erupt 管理后台 → 落地页模板
2. 点击「新建」或「导入」
3. 将对应 `.json` 文件的完整内容粘贴到「amis Schema」字段
4. 填写模板名称、分类后保存

### 方式二：通过 magic-api 接口导入

```sql
-- 以 saas-product.json 为例
INSERT INTO landing_template(name, category, schema, enabled)
VALUES (
  'SaaS产品介绍页',
  3,  -- PRODUCT
  '{"type":"page",...}',  -- 粘贴完整 JSON
  1   -- ENABLED
);
```

---

## 模板列表

| 文件名 | 模板名称 | 分类 | 说明 |
|--------|---------|------|------|
| [saas-product.json](./saas-product.json) | SaaS产品介绍页 | 产品介绍(3) | Hero区+功能卡片+定价表+客户案例+表单 |
| [webinar-registration.json](./webinar-registration.json) | 线上研讨会报名 | 活动报名(5) | 双栏布局+报名表单+嘉宾卡片+议程 |
| [course-promo.json](./course-promo.json) | 课程推广页 | 留资表单(2) | 课程大纲+学员数据+限时特惠 |
| [consulting.json](./consulting.json) | 企业咨询预约 | 留资表单(2) | 双栏+服务卡片+行业选择+需求描述 |
| [app-download.json](./app-download.json) | App下载落地页 | 海报单页(4) | 下载按钮+功能亮点+用户评价 |
| [recruitment.json](./recruitment.json) | 企业招聘页 | 活动报名(5) | 职位表格+福利展示+意向投递表单 |

### 分类编码说明

| 编码 | 分类名 |
|------|--------|
| 1 | 空白页 |
| 2 | 留资表单 |
| 3 | 产品介绍 |
| 4 | 海报单页 |
| 5 | 活动报名 |

---

## 表单对接说明

所有模板的表单均使用统一的 lead 提交接口：

```
POST /api/landing/lead
Content-Type: application/json

{
  "pageId": "<落地页ID>",
  "slug": "<页面slug>",
  "phone": "<手机号>",
  "extra": "{\"name\":\"张三\",\"company\":\"XXX\"}",  -- 其他表单字段合入此 JSON
  "source": 1,  -- 1短链 2直链 3二维码
  "clientIp": "127.0.0.1",
  "userAgent": "Mozilla/5.0..."
}
```

> **注意**：各模板中 `<input>` 的 `name` 字段会自动通过 `extra` JSON 存入 `landing_lead` 表，便于后续查询分析。
