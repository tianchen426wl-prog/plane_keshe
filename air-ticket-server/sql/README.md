# 数据库文件入口

正式结构与数据迁移的**唯一来源**为 `src/main/resources/db/migration`，V1～V10 随后端一起打入 jar。

- V1：干净的基础表、默认管理员、菜单、索引、视图、过程与审计触发器。不导入历史用户、订单或固定日期航班。
- V2～V4：原项目安全、占位、库存及历史保护迁移。
- V5～V8：机场、班期、费用、座位与 AI 表，官方航司资料、演示布局/规则/班期及退款兼容。
- V9：国内参考航线网络扩充，保留已有航班、订单和库存。
- V10：按账户隔离的常用成人乘机人档案。
- 后续新增变更使用新的 `V11__说明.sql` 等版本文件；已经被 Flyway 执行的文件不能原地修改。

正常启动后端即可创建缺失的数据库并迁移，无需手动运行 SQL。只需预先安装并启动 MySQL，配置自己的连接账号、密码及 JWT 密钥。首次建库需要 CREATE DATABASE 权限；建表、视图、过程、触发器等需要对应权限。代码不会安装 MySQL 服务。

已有 V1～V10 项目库通过结构与旧版本记录校验后接入 Flyway；不会重导 V1 或重置管理员、订单及库存。非项目非空库、旧版本记录不连续、校验和不匹配等情况停止启动，不会自动 clean 或猜测修复。

`flyway_schema_history` 为新迁移的权威记录；`schema_version` 保留供旧版本兼容和课程展示。空库首次正常迁移有 V1～V10 SQL 记录，已存在 V8 库接管时记录 V8 BASELINE，后续执行 V9、V10。

可选 CLI 与正常启动使用相同 Flyway 引擎：

```powershell
# 在项目根目录，先构建后端
./scripts/install-db.ps1 -Database air_ticket       # 仅空库/缺失库
./scripts/upgrade-db.ps1 -Database air_ticket       # 仅已有库
```

这两个命令只初始化/迁移，执行完退出，不启动 HTTP 和购票定时任务。不再依赖 mysql.exe；备份脚本仍需 mysqldump.exe。

`legacy/` 是归档。这里的旧初始化文件包含清表、历史示例数据与旧对象定义，只供追溯，**不能导入正在使用的数据库**，不会被启动程序扫描。旧文档引用的 `sql/migrations` 已迁入 resources 下的标准目录。

生成航司/班期 SQL 的维护脚本为 `scripts/build_flight_catalog.py`。V6 已发布并被校验后，不应运行生成器覆盖 V6；新的资料变更请新增迁移。详细成员启动流程见项目根 README。
