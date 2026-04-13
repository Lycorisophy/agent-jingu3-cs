# 数据库脚本与示例数据

## 目录说明

| 路径 | 说明 |
|------|------|
| [migration/](migration/) | **Flyway 版本化 DDL**（`V1__*.sql` …），为单一事实来源；构建时复制到 `agent-jingu3-cs-server` 的 classpath `db/migration` 由 Flyway 执行。 |
| [test-data/](test-data/) | **可选测试/演示数据**（如 `sample_seed.sql`），**不**自动执行；按需手动导入或用于集成测试 `@Sql`。 |

## 与代码的关系

- 服务端使用 **MyBatis-Plus** 访问表结构与此处 DDL 一致；连接配置见 `agent-jingu3-cs-server` 的 `application.yml` / `application-prod.yml`。
- 本地默认 **H2（MySQL 兼容模式）**；生产示例为 **MySQL 8**（见 `application-prod.yml`）。

## MySQL 手动执行迁移

MySQL **不支持** JDBC/H2 里的 **`CLOB`** 关键字，本仓库迁移脚本已统一为 **`LONGTEXT`**（大文本），可在 MySQL 8 与 H2 `MODE=MySQL` 下共用。

建议步骤：

1. 建库（含中文/emoji 时用 utf8mb4）：  
   `CREATE DATABASE jingu3 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
2. 客户端指定字符集，避免中文种子乱码：  
   `mysql --default-character-set=utf8mb4 -u用户 -p jingu3`
3. 在库内**按版本号顺序**执行 `migration/V1__*.sql` … `V5__*.sql`（勿只执行其中一段导致外键/依赖缺失）。**V5** 为记忆向量映射表 `memory_embedding`（与 Milvus 索引对账）。
4. 外键依赖 InnoDB；MySQL 8 默认引擎为 InnoDB，一般无需改表选项。

若仍报错，请对照具体报错信息检查：`sql_mode`、是否重复执行已建表脚本、以及连接是否指向正确 schema。

## 修改迁移脚本后

1. 编辑本目录下对应 `Vn__*.sql`。
2. 执行 `mvn -pl agent-jingu3-cs-server clean compile`（或完整 `verify`）确认 Flyway 校验通过。
3. 若已有环境需重建库，按部署文档处理（开发环境可换库或 `flyway repair` / 手工对齐）。
