# erupt-ai-project 项目记忆索引

## 项目基本信息
- **路径**: /Users/herz/Documents/hrz/erupt01
- **类型**: Spring Boot + Erupt 后台管理系统脚手架
- **构建工具**: Maven
- **Java 版本**: 17（系统安装: `/opt/homebrew/Cellar/openjdk@17/17.0.19`）
- **Maven 路径**: `/opt/homebrew/bin/mvn`

## 环境配置（关键！）
编译或运行测试前必须设置：
```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH"
```
⚠️ 系统默认 Java 26 不兼容，必须用 Java 17。

## 项目结构
```
erupt01/
├── pom.xml                          # 16个 erupt 模块依赖
└── src/
    ├── main/java/com/example/eruptaiproject/
    │   ├── EruptAiProjectApplication.java   # 启动类
    │   └── entity/SimpleEntity.java         # 唯一业务实体
    └── test/java/com/example/eruptaiproject/
        ├── EruptAiProjectApplicationTests.java
        └── entity/SimpleEntityTest.java
```

## 技术栈速查
| 组件 | 版本 |
|------|------|
| Spring Boot | 3.5.15 |
| Erupt | 2.0.3 |
| H2 | 嵌入式数据库 |
| JPA/Hibernate | update 模式 |

## erupt 模块清单
erupt-admin, erupt-web, erupt-ai, erupt-ai-claw, erupt-terminal, erupt-tpl, erupt-job, erupt-generator, erupt-monitor, erupt-magic-api, erupt-excel, erupt-security, erupt-designer, erupt-websocket, erupt-notice, erupt-print

## 业务实体：SimpleEntity（表名 demo_simple）
| 字段 | 类型 | 约束 |
|------|------|------|
| input | String | notNull, searchable |
| number | Float | sortable, searchable |
| bool | Boolean | - |
| date | LocalDate | searchable |
| slide | Integer | slider 0-100, marks at 30/60 |

## 已知限制与注意事项
1. **测试启动**: Erupt 的 i18n 模块依赖 HttpServletRequest，单元测试中必须使用 `WebApplicationType.SERVLET` 模式，不能用 NONE
2. **H2 Console**: 启用于 `/h2-console`
3. **数据持久化**: H2 文件路径 `./data/eruptdb`
4. **主题**: Ant Design

## 测试状态
- 20 个测试全部通过，BUILD SUCCESS
- 覆盖：默认值、getter/setter、边界值、注解验证、上下文启动

## 最近变更
- 2026-07-24: 升级 Erupt 从 2.0.1 到 2.0.3，20个单元测试全部通过，确认 Spring Boot 3.5.15 + Erupt 2.0.3 兼容
