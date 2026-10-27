# GPT 自动驾驶任务（本轮）

你是 Litra 的主程。请直接开始，不要停在计划或空壳。

## 读取顺序

1. `AGENTS.md`
2. `docs/product-goal.md`
3. `mockup.html`（视觉参考）
4. `docs/apple-books-ui-spec.md` 仅在无冲突时参考

## 环境

- macOS，工作目录：仓库根目录（`litra-readium/`）
- `gh` 已登录账号 `unfo17`
- 目标仓库：`litra-readium`（public；不存在则创建）
- 本机暂无 Java / Android SDK；需要时自行安装 JDK 17 和 Android SDK，接受 licenses，配置 `ANDROID_HOME` / `local.properties`
- 网络可用；使用 `gh`、`git`、`gradle` 完成闭环

## 本轮目标

**不要只做 M0。** 从 M0 开始，尽可能连续推进到 M1、M2、M3、M4、M5；直到 v0.1.0 可安装、可阅读、核心验收通过。把额度用在这件事上，不要提前收工。

必须完成的闭环：

1. GitHub public 仓库 `litra-readium` 创建并推送；
2. Android 工程可构建；
3. EPUB 导入、解析、封面；
4. 书库双列 + 列表、藏书选项、已读完时间线；
5. 阅读器（Readium EPUB），99% 规则；
6. 主页五个区块、欲读大封面、标记读完；
7. 搜索 + 设置；
8. 详情页、阅读目标；
9. 单元测试 + CI；
10. **只在最后创建一个 Release：`v0.1.0`**（不要里程碑 tag，不要 nightly/alpha/beta）。

## 硬规则

- 读到末尾最高 99%；不自动打勾；
- 99% 卡片只显示“点击标记读完”；
- 标记完成后 `FINISHED` 持久化，重读中间仍显示已读完；
- 书籍类型只有图书、漫画；
- 书库进度在封面外左下角，纯文字；
- 已读完显示“已读完”，不加对号；
- 藏书选项全屏覆盖底栏，选中加深，单按钮标记→删除；
- 已读完独立页，无底栏、无书库标题；
- 搜索无记录不渲染记录区块；
- 详情页海报延伸至状态栏，下半部分居中、矩形卡片；
- 不要使用 Apple 素材。

## Git / 发布

- 每个里程碑提交并推送；功能分支 + PR，可自我合并；
- Conventional Commits；
- 不要提交本机路径、密钥、`.DS_Store`、构建产物；
- 只有核心验收通过才打 `v0.1.0` tag 并创建 GitHub Release；
- 没有正式签名就用 debug APK，Release notes 注明 preview/debug signed；
- 只发一个版本；最多允许后续一个 `v0.1.1` 修复。

## 工作方式

- 不要等用户确认；遇到小决策按 product-goal 选择默认值并记录；
- 每完成一个阶段就构建、测试、提交、推送；
- 如果本机安装 SDK 失败，先用 GitHub Actions 构建，并继续写代码；
- 如果任务很长，优先保证仓库处于可构建状态，并把剩余工作写进 `docs/STATUS.md`；
- 结束时报告：仓库 URL、最新 commit、CI 状态、Release URL、APK 路径、完成了哪些验收、剩余风险。
