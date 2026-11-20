# GitHub 提交与同步说明

仓库：[tianchen426wl-prog/plane_keshe](https://github.com/tianchen426wl-prog/plane_keshe)。可见性沿用用户在 GitHub 页面中的设置，同步脚本不会修改可见性或协作者权限。

## 提交范围

保留后端/前端源码、Flyway 正式迁移、测试和启动脚本、配置示例、两个前端的 `package-lock.json`、项目说明和运行必需的 `public` 图片/字体。

忽略 `.env`、`.local`、真实本地配置、依赖目录、`target`/`dist`、日志、临时目录、备份、私钥和打包文件。根目录 `img` 是旧素材副本，运行所需素材已保留在前端 `public`；`libs` 的连接器由 Maven 获取；旧 `sql/legacy`、本机审计记录、论文模板 PDF 和无用途的 `query` 文件不上传。忽略不会删除本机文件。

本机验收材料默认不上传，仅保留明确使用测试数据生成的体验截图与摘要。首次提交前执行 `scripts/check-repository.ps1`，检查实际暂存快照与本机密钥是否重合；该脚本不打印密钥值。

## 后续更新

在项目根目录使用 PowerShell 7：

```powershell
./scripts/sync-github.ps1 -Message '说明本次修改'
```

脚本会暂存符合忽略规则的变更、执行提交检查、有变更时创建提交、拉取远端并重放本地提交，最后推送 `main`。若远端修改导致冲突，会中止此次重放并保留本地提交，不强制覆盖远端。

在 Windows 上会沿用当前系统已启用的 HTTP/HTTPS 代理，并在系统关闭代理后清除仓库的旧代理配置；不会修改 Windows 网络设置。认证凭据保存在 Windows 凭据管理器中，不依赖项目临时目录内的 GitHub CLI。

不要重复新建同名仓库，不使用网页反复上传整个项目，不提交 `node_modules` 或 `dist`，不要强制推送。

克隆到另一台电脑后，先执行：

```powershell
git config core.hooksPath .githooks
./scripts/setup-env.ps1
```

然后按 README 安装依赖并构建。`.env.example` 中仅有占位符，真实配置在每台电脑独立填写。

`.gitattributes` 统一文本换行；两个前端的锁文件应随依赖修改一并提交。合并中出现冲突时先解决冲突，再重新提交和同步。
