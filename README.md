# 航空售票管理系统 · plane_keshe

基于 Spring Boot 2.7.18、MyBatis、MySQL、Vue 2 和 Element UI 的航空售票管理系统，包含用户门户与管理后台。

当前已实现：14 家航司官方资料与本地品牌图片、62 个城市、65 个机场、182 组往返航线、364 个每日班期、未来 30 天滚动航班、费用分项与锁价、具体座位选择、限时占位、订单确认、规则化退款、后台权限和订单审计，以及 DeepSeek 行程规划。数据库结构当前版本为 **V11**，启动时通过 Flyway 自动初始化和迁移。

系统维护本地航班目录、库存、报价与订单。外部航空出票和支付网关尚未接入，运行配置与对接边界在部署时独立管理。

本轮航线覆盖、查询规则和参考数据来源见[国内航线扩充说明](docs/国内航线扩充说明.md)。

第一批体验改造已增加：搜索人数与整单预算等筛选、AI 候选直接衔接预订、常用成人乘机人、三步预订、完整订单详情、打印行程卡与 ICS 日历导出。实现范围与验证记录见[第一批体验优化交付说明](docs/第一批体验优化交付说明.md)。

GitHub 提交范围、密钥保护和后续增量同步方式见 [GitHub 提交与同步说明](docs/GitHub提交与同步说明.md)。本机 `.env`、依赖、构建包和日志不进入仓库。

## 新成员：环境配置与启动

配置方式参考乡育智航后端：提交 `.env.example`，每位成员在自己的电脑上创建 `.env`，启动脚本自动加载。当前后端使用 Java，因此运行环境由 JDK/Maven 管理，前端依赖由 npm 管理，无需创建 Python venv。

在项目根目录使用 **PowerShell 7**：

```powershell
# 首次执行：复制示例配置并生成随机 JWT 密钥，不会覆盖已有 .env
./scripts/setup-env.ps1
notepad .env
```

在 `.env` 中修改 **DB_USERNAME、DB_PASSWORD**；数据库地址、端口和库名也在同一区域。`JWT_SECRET` 已由初始化脚本生成，固定保存即可。按自己的安装位置填写 `JAVA_HOME`、`MAVEN_HOME`（已加入 PATH 时可以省略）。智能规划可选，通过 `AI_ENABLED` 和 `DEEPSEEK_API_KEY` 配置。

```powershell
# 配置当前终端的工具路径和环境变量，作用类似参考项目的环境激活步骤
. ./scripts/activate.ps1

# 首次安装依赖并构建
mvn.cmd -f air-ticket-server/pom.xml -DskipTests package
npm.cmd ci --prefix air-ticket-web/portal
npm.cmd run build --prefix air-ticket-web/portal
npm.cmd ci --prefix air-ticket-web/admin
npm.cmd run build --prefix air-ticket-web/admin

# 终端 A：启动后端，自动读取 .env
./start_backend.ps1
```

另开一个 PowerShell 7 终端，在项目根目录执行 `./start_frontend.ps1`，然后访问门户 `http://127.0.0.1:18081` 和管理后台 `http://127.0.0.1:18082`。默认后端端口为 `.env` 中的 18080。MySQL 服务需提前安装并启动；后端自动创建缺失库并执行 V1～V11 迁移，首次初始化账号需具备建库、建表、视图、存储过程及触发器等权限，无需手动导入 SQL。

公开提交 `.env.example`、启动脚本和依赖清单；`.env`、`.local`、构建产物和依赖目录均已被 Git 忽略。已有 `.local/application-local.yml` 的成员可执行 `./scripts/setup-env.ps1 -FromLocalConfig` 迁移到 `.env`，原文件保留；已有 `.env` 时初始化脚本不会覆盖。

## 1. 本机日常启动：两个终端

本机私有配置和构建产物已准备好，日常运行从本节开始即可。其他成员先安装并启动 MySQL、填写自己的配置并构建程序，再执行同样的后端启动命令；**数据库不存在时会自动创建、建表并导入基础资料，无需先导入 SQL。**

推荐使用 **PowerShell 7**。以下命令在项目根目录执行。先确认 MySQL 服务已运行；已有服务正在运行时，先访问下面的地址，不要重复启动相同端口。

### 终端 A：启动后端

```powershell
Set-Location 'D:\keshe\plane_keshe'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.5'
./scripts/run-server.ps1 -Port 18080 -Database air_ticket
```

该命令持续占用终端 A，**保持窗口运行**。脚本自动加载项目根目录 `.env`，使用 JAVA_HOME 对应的 Java，并兼容原有 `.local/application-local.yml` 配置。启动顺序为：连接 MySQL → 创建缺失数据库 → Flyway 建表/升级至 V11 → 启动 HTTP → 补齐未来 30 天航班。看到 `Project database ... is ready` 和 `Tomcat started on port(s): 18080` 后可访问；首次生成航班比普通重启更慢。初始化失败时后端停止启动，不会开放半套数据库的业务服务。

### 终端 B：启动门户和后台

```powershell
Set-Location 'D:\keshe\plane_keshe'
./start_frontend.ps1
```

脚本自动加载 `.env`，一条命令同时提供两个前端，**保持终端 B 运行**。看到 portal/admin 两条地址输出后，用浏览器打开：

| 入口 | 地址 | 说明 |
|---|---|---|
| 用户门户 | [http://127.0.0.1:18081](http://127.0.0.1:18081) | 查询航班、选座、购票、个人订单 |
| 管理后台 | [http://127.0.0.1:18082](http://127.0.0.1:18082) | 登录后管理航司、航班、退款及生成任务 |
| 智能规划 | [http://127.0.0.1:18081/#/planner](http://127.0.0.1:18081/#/planner) | 需要用户登录；使用服务端 DeepSeek 配置 |
| 后端 API | [http://127.0.0.1:18080/api/ai/capabilities](http://127.0.0.1:18080/api/ai/capabilities) | 可用于确认后端与 AI 能力状态 |

`serve-built.cjs` 读取 portal/admin 的 **dist 构建目录**，将 `/api/` 转发到 `http://127.0.0.1:18080`。它不自动编译源码，也不需要再执行 `npm run serve`。

本机此前发现 8080 被其他程序占用、8081 无法监听，因此推荐上述 18080/18081/18082 组合。`run-server.ps1` 优先使用明确的 `-Port`，其次读取 `.env` 的 SERVER_PORT；两者均未设置时回退至 8080。

### 启动后检查与停止

1. 门户能打开，航司图片正常显示。
2. 进入“订机票”，选择售卖窗口内日期查询；若首次生成未完成，稍后重新查询。
3. 管理后台 → 航班管理 →“每日航班、班期与票价规则”，查看生成记录是否为 `SUCCESS`。必要时使用“补齐缺失航班”。
4. 每日 00:05 按 Asia/Shanghai 时区补齐航班，需要后端持续运行；停机后再次启动会补漏，已有订单及库存不会被刷新重置。

停止时，分别在终端 B、终端 A 按 **Ctrl+C**。重新启动重复本节两条启动流程即可。停止前端后后端仍会继续运行定时任务；停止后端后前端无法查询或交易。

## 2. 环境与私有配置

### 环境要求

| 工具 | 项目使用情况 |
|---|---|
| PowerShell 7 | 数据库脚本使用 .NET 的 ProcessStartInfo.ArgumentList；不要用 Windows PowerShell 5.1 执行这些脚本 |
| JDK 17 | 本机已验证；后端编译目标 Java 11，建议使用已验证的 JDK 17 启动 |
| Maven 3.9.x | 本轮使用 Maven 3.9.6 完成构建与测试 |
| MySQL 8.0.16+ | 使用 CHECK 约束、JSON、视图、存储过程；本机已验证 8.0.42 |
| Node.js / npm | 用于前端依赖、构建和本地静态服务器；按现有 package-lock.json 安装 |
| MySQL 命令行工具（可选） | 正常启动和安装/升级无需 mysql.exe；备份脚本仍需 mysqldump.exe，手动 SQL 核对可使用数据库客户端 |

在每个需要构建的终端中可设置本机工具路径：

```powershell
Set-Location 'D:\keshe\plane_keshe'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.5'
$env:Path = "$env:JAVA_HOME\bin;C:\Users\tc190\apache-maven-3.9.6\bin;$env:Path"

java -version
mvn.cmd -version
node --version
npm.cmd --version
mysql.exe --version
$PSVersionTable.PSVersion
```

这些路径是本机已存在的安装位置；换电脑时替换为实际路径。环境变量只作用于当前终端及其子进程。另一个终端需要构建时，也应配置其工具路径。

### 配置文件

推荐使用根目录 `.env`，由 `.env.example` 生成。后端、数据库安装/升级、备份、测试和前端 PowerShell 启动脚本都会加载它。通过任意工作目录调用这些脚本时，仍定位到项目根目录配置。

| 文件/脚本 | 用途 |
|---|---|
| `.env.example` | 可提交的分组配置模板，标明数据库、工具路径、端口、JWT 和 AI 的修改位置 |
| `.env` | 每位成员自己的真实连接参数和密钥，不提交 |
| `scripts/setup-env.ps1` | 创建配置、生成 JWT 密钥；`-FromLocalConfig` 可迁移旧 YAML，不覆盖现有文件 |
| `scripts/activate.ps1` | 用 `. ./scripts/activate.ps1` 配置当前终端；为构建命令加载 JDK/Maven 路径 |
| `start_backend.ps1` / `start_frontend.ps1` | 自动读取 `.env` 并启动已构建的后端/前端 |
| `.local/application-local.yml` | 兼容原有私有配置，`.env` 未提供的本地字段仍可回退读取 |

配置优先级为：**明确指定的启动参数 > 已有进程环境变量 > `.env` > 本地 YAML > 程序默认值**。脚本加载的变量只作用于当前进程及子进程，不修改 Windows 的全局环境变量。若在当前终端激活过环境、随后修改 `.env`，请在新终端启动，避免旧变量继续优先；在 IDE 中直接运行 Java 或直接执行 `java -jar`，应通过 IDE 设置环境变量，或先执行环境激活脚本。

`.env` 支持 `KEY=value`、单/双引号、整行和行尾注释，以及 `export KEY=value`。不会执行命令或展开 `$变量`；不支持跨行值。密码含空格或 `#` 时请用单引号包裹，内部单引号写成 `\'`、反斜杠写成 `\\`。模板中的工具路径可直接按实际 Windows 路径填写。

| 环境变量 | 默认/用途 |
|---|---|
| JAVA_HOME / MAVEN_HOME | 可选，指向本机 JDK/Maven 安装目录，激活/后端脚本加入各自 bin |
| SERVER_PORT | `.env.example` 默认 18080；`-Port` 可覆盖 |
| DB_HOST / DB_PORT | 默认 `127.0.0.1:3306` |
| DB_NAME | 默认 `air_ticket`；后端 `-Database` 可覆盖 |
| DB_USERNAME / DB_PASSWORD | MySQL 账号/密码，优先于本地 `local.db-username` / `local.db-password` |
| DB_INITIALIZE / DB_CREATE_IF_MISSING | 默认 true，控制启动迁移和自动创建缺失库 |
| JWT_SECRET | 由初始化脚本生成，优先于本地 `local.jwt-secret`，固定保存 |
| ALLOWED_ORIGINS | 允许的前端来源，逗号分隔；支持 `-AllowedOrigins` 覆盖 |
| BACKEND_URL / PORTAL_PORT / ADMIN_PORT | 构建模式前端的 API 代理和监听端口 |
| FLIGHT_GENERATION_ENABLED | 默认 true，控制启动补漏和每日生成 |
| AI_ENABLED / DEEPSEEK_API_KEY | 可选的 AI 开关/密钥，优先于本地配置 |
| DEEPSEEK_MODEL | 默认 `deepseek-flash` |

正常启动和安装/升级 CLI 使用同一后端连接配置；备份脚本也读取 DB_HOST/DB_PORT/DB_USERNAME/DB_PASSWORD。首次建库需要相应权限，备份仍需安装 mysqldump.exe。修改 SERVER_PORT 时，应同步调整 BACKEND_URL；开发热更新代理另见第 4 节。

## 3. 首次安装、升级与重新构建

本机已有 V11 数据库与构建产物可跳过本节。换电脑、拿到新源码或修改程序后，按对应步骤处理。

### 3.1 数据库：默认随后端自动建立

新成员只需在 `.env` 中配置自己的 MySQL 和 JWT；无需预先创建 air_ticket 或手动执行 SQL。先按下两节构建，再按第 1 节启动后端。默认执行：

1. 库不存在：仅在 MySQL 明确返回“Unknown database”时创建配置指定的库。
2. 库为空：执行 jar 内 V1 至 V11，初始化管理员、菜单、14 家航司、机场、布局、费用规则及班期；不导入历史乘机人和固定日期旧航班。
3. 已有项目库：核对结构和版本，通过后接入 Flyway；原 V8 库记录 V8 BASELINE，不重导 V1，不重置账号、订单或库存。
4. 后续启动：校验已执行脚本，只执行新增版本；无变更时跳过。
5. 迁移完成后才启动业务组件与定时任务，补齐实际查询日期的航班实例。

正式 SQL **唯一目录**为 `air-ticket-server/src/main/resources/db/migration`，会随 Maven 打包进 jar。`sql/legacy` 仅为旧脚本归档，包含清表等历史语句，不会被程序扫描，不能导入在用数据库。文件分工见 [数据库文件说明](air-ticket-server/sql/README.md)。

已有库升级前建议停止旧项目后端并备份。可选维护命令也调用同一个 jar/Flyway 入口，执行后退出，不启动 HTTP 或购票定时任务：

```powershell
Set-Location 'D:\keshe\plane_keshe'
# 已有数据库：先备份（需要mysqldump.exe）
./scripts/backup-db.ps1 -Database air_ticket
# 仅迁移已有数据库
./scripts/upgrade-db.ps1 -Database air_ticket
```

```powershell
# 可选：仅初始化一个缺失/空数据库，不启动网站
Set-Location 'D:\keshe\plane_keshe'
./scripts/install-db.ps1 -Database air_ticket
```

两个维护命令需要先构建后端 jar；正常运行项目直接启动后端即可。install 发现非空库会拒绝；upgrade 发现库不存在会拒绝。旧 -DemoData 参数不再导入过期航班，-RestoreTest 私有恢复入口已退役。

可在数据库客户端核对：

```sql
USE air_ticket;
SELECT installed_rank, version, description, type, success
FROM flyway_schema_history ORDER BY installed_rank;
SELECT * FROM v_inventory_health WHERE difference <> 0;
SELECT * FROM v_seat_health WHERE difference <> 0 OR capacity_difference <> 0;
```

空库初始化应有 V1～V11 SQL 记录；原有 V8 库接管后应有 V8 BASELINE 和 V9、V10 升级记录，这也是正常状态。后两项正常应为空集。`schema_version` 保留供旧库兼容，新迁移以 `flyway_schema_history` 为准。

非项目非空库、版本不连续、脚本校验和不符或权限不足时会明确失败，不会自动清库或忽略错误。MySQL 的 DDL 不能整体回滚；失败迁移请先核对和备份后修复，不要直接删除版本记录或自动执行 Flyway clean/repair。

### 3.2 构建后端

先停止正在运行的同一个 jar，避免 Windows 文件占用。已按第 2 节设置 JAVA_HOME 和 Maven 路径后执行：

```powershell
Set-Location 'D:\keshe\plane_keshe\air-ticket-server'
mvn.cmd -DskipTests package
```

生成 `air-ticket-server/target/air-ticket-server-1.0.0.jar`。此命令用于构建，不代表测试通过；完整测试见第 5 节。

### 3.3 安装前端依赖并构建

首次安装或 lockfile 变化时执行 npm ci；日常启动现有 dist 不需要重复安装。

```powershell
Set-Location 'D:\keshe\plane_keshe\air-ticket-web\portal'
npm.cmd ci
npm.cmd run build

Set-Location 'D:\keshe\plane_keshe\air-ticket-web\admin'
npm.cmd ci
npm.cmd run build
```

分别生成 portal/dist 和 admin/dist。只修改前端源码且依赖未变化时，重新执行相应目录的 `npm.cmd run build` 即可。构建完成后按第 1 节启动；浏览器刷新后加载新资源。

## 4. 开发调试与自定义端口

### 开发服务器模式

两份 `vue.config.js` 当前配置为：portal 8081、admin 8082，API 代理目标均为 `http://localhost:8080`。如果电脑可以使用这组端口，可以在三个终端分别运行：

```powershell
# 终端 A：后端；已按第 2 节设置 JAVA_HOME
Set-Location 'D:\keshe\plane_keshe'
./scripts/run-server.ps1 -Port 8080 -Database air_ticket
```

```powershell
# 终端 B：门户热更新
Set-Location 'D:\keshe\plane_keshe\air-ticket-web\portal'
npm.cmd run serve
```

```powershell
# 终端 C：后台热更新
Set-Location 'D:\keshe\plane_keshe\air-ticket-web\admin'
npm.cmd run serve
```

本机推荐使用第 1 节的构建模式。如果要在本机用 18080/18081/18082 开发热更新，需要先将**两份** vue.config.js 的代理 `target` 改为 `http://127.0.0.1:18080`，再运行前端 `npm.cmd run serve -- --port 18081` / `--port 18082`，并通过后端脚本的 `-AllowedOrigins` 参数允许这两个来源。仅改变前端监听端口不会改变其 API 代理目标；BACKEND_URL 只对 serve-built.cjs 生效。

### 构建模式自定义端口

后端显式指定端口；另一个终端为静态服务器设置匹配的地址：

```powershell
# 终端 A
Set-Location 'D:\keshe\plane_keshe'
./scripts/run-server.ps1 -Port 19080 -Database air_ticket
```

```powershell
# 终端 B
Set-Location 'D:\keshe\plane_keshe'
$env:BACKEND_URL = 'http://127.0.0.1:19080'
$env:PORTAL_PORT = '19081'
$env:ADMIN_PORT = '19082'
node scripts/serve-built.cjs
```

静态服务器只监听 127.0.0.1，适合本机演示。对外部署使用正式静态服务器提供两个 dist，并配置 `/api/` 反向代理及明确的来源白名单。

## 5. 功能测试

### 环境配置检查

```powershell
./scripts/test-env.ps1
```

此检查不连接数据库、不启动服务，验证配置初始化不会覆盖现有文件、JWT 生成、旧 YAML 迁移、密码特殊字符，以及 `.env`、终端环境变量和启动参数的优先级。

### 数据库集成测试

测试使用独立 **air_ticket_test**。现在测试上下文也会自动创建缺失库和迁移结构，无需先运行 SQL 安装脚本。测试账号需要创建临时测试库的权限；初始化测试只创建并清理随机前缀 `air_ticket_boot_it_` 的自有数据库，不会删除正式库。

按第 2 节设置工具路径后，在根目录运行：

```powershell
Set-Location 'D:\keshe\plane_keshe'
./scripts/test.ps1
```

该脚本自动加载 `.env`（兼容本地 YAML），进入后端目录执行 `mvn verify`。测试显式连接 air_ticket_test，并检查库名；自动生成与真实模型调用在测试上下文关闭。打包前停止正在运行的同一 jar；测试期间不要让浏览器或另一个测试应用同时修改测试库。

上轮业务验收：BookingIntegrationTest 18 项 + FlightFeaturesIntegrationTest 19 项，共 37 项通过。本次新增 DatabaseInitializationIntegrationTest，覆盖缺失库、已有空库、V4/V8 接管、并发、重复启动、校验和及误连保护，最新结果见自动初始化验收记录。报告在 `air-ticket-server/target/surefire-reports`。这包括计价锁价、座位并发、组单回滚、退款释放、班期幂等、星期/停飞例外、旧库存缓存兼容和模型协议测试。

### 浏览器闭环测试

需要已构建的两个 dist、Playwright 和已安装的 Chrome。Playwright 应在运行 Node 的环境中可加载；不属于前端生产依赖。

先停止第 1 节的静态服务器，释放 18081/18082。测试后端单独启动到 18085，**必须指定测试库**：

```powershell
# 测试后端终端
Set-Location 'D:\keshe\plane_keshe'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.5'
$env:AI_ENABLED = 'false'
./scripts/run-server.ps1 -Port 18085 -Database air_ticket_test -AllowedOrigins 'http://127.0.0.1:18081,http://127.0.0.1:18082'
```

等待测试库航班生成成功，再在另一终端执行：

```powershell
Set-Location 'D:\keshe\plane_keshe'
$env:E2E_CONFIRM_TEST_DB = 'air_ticket_test'
$env:E2E_BACKEND = 'http://127.0.0.1:18085'
node scripts/browser-smoke.cjs
```

脚本临时提供 18081/18082 的测试页面，完成登录、查询、14 家航司筛选、选座、报价、支付、退款审核及手机布局检查，结束后自动关闭前端测试服务。它会创建测试用户与交易，请勿指向正式库。测试后端仍需在其终端按 Ctrl+C 停止；之后恢复第 1 节的日常启动。

最近一次浏览器验收为 **26 个页面/宽度组合**通过，pageErrors=[]。模型协议测试使用模拟提供商；实际 DeepSeek 调用另外验证成功，返回 4 个数据库候选，详见验收文件。

## 6. DeepSeek、账号与业务规则

### DeepSeek

本机已经在私有配置中接入 DeepSeek。新环境推荐在 `.env` 中填写 AI_ENABLED、DEEPSEEK_API_KEY；也可在**后端终端**设置：

```powershell
$env:AI_ENABLED = 'true'
$env:DEEPSEEK_API_KEY = '填写自己的密钥'
$env:DEEPSEEK_MODEL = 'deepseek-flash'
```

设置后重新启动后端。访问 `/api/ai/capabilities` 查看 enabled/provider；enabled 表示已启用并配置密钥，不等于已验证远端余额或服务可用性，完整链路通过登录后的“智能规划”验证。

当前支持国内成人直飞单程需求，提供出发地、目的地、日期、1～5 位成人和机票总预算。模型仅接收必要需求与公开航班候选，不持有数据库凭据、不执行 SQL，也不自动占位、支付、退款或创建新航班。未启用或远端失败时，可继续使用普通查询。

调用协议参见 [DeepSeek 官方文档](https://api-docs.deepseek.com/)；密钥不能进入前端或源码交付材料。

### 账号与交易

- 初始化超级管理员为 `admin / admin123`；已有库如果改过密码，以当前数据库为准。普通用户可通过门户注册。
- 后端角色值 0 为超级管理员、1 为普通管理员；普通管理员需要分配菜单权限。
- 每次 1～5 张，每位旅客对应具体座位；每用户最多 3 个未完成占位。
- 完整报价有效 5 分钟，成功占位后默认锁定 15 分钟，过期自动释放；确认使用服务端保存的金额快照。
- 当前演示产品：起飞前 24 小时以上，基础运价退订费为 10%；之后至起飞前 2 小时为 20%；演示税费、附加费和选座费可退。以购买时保存的产品版本为准。
- 退款批准后才恢复库存与座位；拒绝后恢复已购状态，可再次申请。航班取消可按非自愿规则处理，旧单保持旧规则。
- 有交易历史的航班/舱位禁止硬删除。班期生成不重置库存、不覆盖已有日期实例；已绑定布局的容量不能直接修改。
- Web 退款统一经 BookingService 事务处理；V8 同步升级了数据库退款过程，不在一次退款中叠加调用两个入口。

## 7. 常见启动问题

| 现象 | 检查与处理 |
|---|---|
| 找不到 mvn.cmd / JAVA_HOME 错误 | 按第 2 节在当前终端配置实际 JDK 和 Maven 路径，确认 `mvn.cmd -version` 使用 JDK 17 |
| PowerShell 的 ArgumentList 等属性不可用 | 使用 PowerShell 7；检查 `$PSVersionTable.PSVersion` |
| 无法执行 ps1 脚本 | 先检查当前 PowerShell 执行策略；在允许的本机环境可使用 `pwsh -NoProfile -ExecutionPolicy Bypass -File ./scripts/run-server.ps1 -Port 18080 -Database air_ticket`，不修改全局策略 |
| 找不到 jar / Build first | 先按第 3.2 节打包；启动脚本只运行已有 jar，不会自动编译 |
| Build portal/admin first | 缺少对应 dist，按第 3.3 节构建两个前端 |
| Port already in use / EADDRINUSE | 可能已有实例运行，先访问地址确认；停止自己启动的对应终端，或按第 4 节成套更换端口 |
| listen EACCES，但未发现进程 | Windows 可能限制了端口监听；本机使用 18080/18081/18082，不强行占用 8081 |
| 页面能打开，API 返回 502 | 检查后端是否运行、BACKEND_URL 是否匹配、数据库是否可连接 |
| npm run serve 后接口请求错误 | 检查两份 vue.config.js 的代理 target；它们默认指向 8080，与构建模式的 18080 不同 |
| 登录请求 403 / 跨域错误 | 开发服务器直连代理场景检查后端 `-AllowedOrigins` 与实际前端来源是否一致 |
| MySQL connection refused / access denied | 确认 MySQL 服务运行、3306 可访问、私有密码配置正确；不要在问题截图中展示密钥 |
| 自动建库/迁移失败 | 确认 MySQL 已启动，账号有创建库/表/视图/过程/触发器权限；不要在异常后导入旧全量脚本 |
| Table/column does not exist | 检查 DB_NAME、DB_INITIALIZE 是否启用及 Flyway 迁移记录 |
| 非空目标库被拒绝 | 核对是否连错库或旧库不完整；程序不会覆盖不认识的数据 |
| Flyway checksum mismatch / failed migration | 保留现场和备份，核对已执行的 SQL 是否被修改，由维护者处理，不自动跳过错误 |
| 今天查不到可售航班 | 检查生成任务、查询日期、出发/到达地以及售卖截止时间；可切换未来日期，不要修改旧航班日期凑数据 |
| AI enabled=false | 检查后端进程的 AI_ENABLED、私有开关与密钥，配置变更后重启后端 |
| AI enabled=true 但规划失败 | 查看界面返回的远端状态，核对密钥、账户额度和网络，普通查询仍可用 |
| 修改源码后页面/后端没变化 | 构建模式需重新打包相应部分；重启后端、刷新浏览器。dist 和 jar 不会随源码自动更新 |

后端日志默认在 `air-ticket-server/logs/air-ticket`。数据库备份和本机私有验证日志在 `.local`；对外提交时不包含该目录。

## 8. 项目结构与验收资料

```text
air-ticket-server/           Spring Boot 后端
  src/main/                 业务、接口与配置
    resources/db/migration/  V1～V11 正式SQL，随jar交付
  src/test/                 MySQL 集成测试
  sql/legacy/               旧SQL归档，不用于自动初始化
air-ticket-web/
  portal/                   用户门户
  admin/                    管理后台
  shared/                   两端共享主题
scripts/                    统一CLI安装/升级、备份、启动及浏览器测试脚本
docs/                       方案、实施记录、素材清单和验收证据
.local/                     本机密钥、备份、私有日志（不交付）
```

- [数据库自动初始化说明与验收](docs/数据库自动初始化与成员启动.md)：成员首次运行、旧库接管及初始化测试。
- [航班、计价、座位与 DeepSeek 实施验收记录](docs/航班与选座扩展_实施验收记录.md)：本轮功能、数据库变化、测试与范围。
- [航班扩展实施方案](docs/航班扩充与每日刷新_票价座位_AI接口实施方案.md)：设计依据和阶段安排。
- [航空公司素材清单](docs/assets/airline-assets.json)：14 份官方品牌图片的原始地址、下载日期、尺寸、版权说明和校验和；图片保存在双前端 public/images/airlines，版权保留，不宣称开放许可。
- [数据库验收摘要](docs/evidence/flight-feature-verification.json)：V8、航司/班期/座位数量、一致性和历史数据保护结果；数量及日期是验收时快照，随窗口滚动。
- [浏览器验收](docs/evidence/browser-smoke.json)及[DeepSeek 实际调用记录](docs/evidence/deepseek-live-smoke.json)。
- [前端 UI 重构记录](docs/前端UI重构实施记录.md)与[早期业务验收记录](docs/实施与验收记录.md)。
- [课程设计总体方案](课程设计优化完善方案.md)；audit 目录中的自查报告为历史问题记录，不代表当前功能状态。

课程提交按课程要求整理源码和个人报告，排除 `.local`、node_modules、运行日志、账号备份和未脱敏信息。保留 package-lock.json、数据库迁移、来源清单及必要验收文档。前端现有 vendor 包体积提示仍存在，最近一次两端生产构建均已通过。
