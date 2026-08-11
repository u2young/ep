---
name: erupt-project-context
description: '当用户打开 erupt01 项目或提到 erupt-ai-project、Erupt 后台管理项目时使用。快速加载项目上下文，避免重复扫描文件。'
metadata:
  argument-hint: '[可选: 指定分析深度]'
---

你正在分析 erupt-ai-project 项目。以下是项目的完整上下文，直接基于此进行分析，无需重新扫描文件：

## 项目基本信息
- **路径**: /Users/herz/Documents/hrz/erupt01
- **技术栈**: Spring Boot 3.5.15 + Erupt 2.0.1 + H2 + JPA/Hibernate
- **Java 版本**: 17（安装路径: /opt/homebrew/Cellar/openjdk@17/17.0.19）
- **Maven 路径**: /opt/homebrew/bin/mvn
- **构建命令**: 每次执行 mvn 前需设置 `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home && export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH"`

## 项目结构
```
erupt01/
├── pom.xml                          # Maven 配置，引入 16 个 erupt 模块
└── src/main/java/com/example/eruptaiproject/
    ├── EruptAiProjectApplication.java   # 启动类 (@SpringBootApplication + @EruptScan)
    └── entity/
        └── SimpleEntity.java             # 唯一业务实体 "简单示例"
```

## SimpleEntity 字段
| 字段 | 类型 | 说明 |
|------|------|------|
| input | String | 文本（必填，可搜索） |
| number | Float | 数值（可排序，可搜索） |
| bool | Boolean | 布尔值 |
| date | LocalDate | 日期（可搜索） |
| slide | Integer | 滑动条（0-100，标记点30/60） |

## application.yml 关键配置
- 端口: 8080
- 数据库: H2 文件模式 `./data/eruptdb`
- H2 Console: 启用，路径 `/h2-console`
- JPA ddl-auto: update
- 主题: Ant Design
- 日志: xyz.erupt DEBUG, SQL TRACE

## 测试覆盖
- `EruptAiProjectApplicationTests.contextLoads()` — WebApplicationType.SERVLET 模式启动
- `SimpleEntityTest` — 19 个测试：默认值、getter/setter、边界值、注解验证
- 全部 20 个测试通过，BUILD SUCCESS

## erupt 模块列表
erupt-admin, erupt-web, erupt-ai, erupt-ai-claw, erupt-terminal, erupt-tpl, erupt-job, erupt-generator, erupt-monitor, erupt-magic-api, erupt-excel, erupt-security, erupt-designer, erupt-websocket, erupt-notice, erupt-print

## 注意事项
- 编译测试时必须使用 Java 17，不能用系统默认的 Java 26
- Erupt 的 i18n 模块依赖 HttpServletRequest，测试中必须用 SERVLET 模式
- 数据目录 `./data/eruptdb` 在测试时会产生临时文件，正常
