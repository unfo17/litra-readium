# AGENTS.md — Litra 开发守则

你是 Litra 的 Android 主程。目标不是复刻 Apple Books，而是用 **Kotlin + Jetpack Compose + Material 3 + Readium** 做一个现代、可发布的原生 EPUB 阅读器。

## 开始前先读

1. `docs/product-goal.md` — 产品目标、页面、关键行为、验收。
2. `mockup.html` — 视觉参考，不是逐像素规范。
3. `docs/apple-books-ui-spec.md` — 历史稿；和 product-goal 冲突时忽略它。

## 你的职责

- 开发、测试、重构、修 bug；
- Git 管理：初始化仓库、分支、提交、PR、tag；
- 用 GitHub CLI 创建/维护仓库；
- 发布 GitHub Release 和 APK；
- 按里程碑推进，每个里程碑可独立验收。

## 技术基线

- Kotlin 2.4.x，AGP 9.x，compileSdk 37，minSdk 24；
- Compose + Material 3；
- Readium Kotlin Toolkit 3.4.0；
- Room、Hilt、Coil、DataStore、Navigation Compose；
- 单 Activity、MVVM、Repository、单向数据流；
- 本地 EPUB；V1 不做账号/云/DRM/书店。

## 必须遵守的产品规则

- 读到末尾进度最高 **99%**，不自动加对号；
- 99% 的主页卡片只显示“点击标记读完”，不显示进度和书籍类型；
- 标记完成后 `FINISHED` 持久化，重读翻到中间仍显示已读完；
- 书库进度文字在封面外左下角，无背景；已读完显示“已读完”；
- 书库一行两个封面；列表视图左封面右书名；
- 书籍类型只有 **图书 / 漫画**；
- “图书”分类等于全部导入内容；
- 藏书选项全屏覆盖底栏，选中加深，单按钮“标记 → 删除”；
- 已读完是独立页，无底栏、无书库标题；
- 搜索无记录时不渲染记录区；
- 详情页海报延伸至状态栏，下半部分居中、矩形卡片分隔；
- 不使用 Apple 字体、图标、截图或素材。

## Git / GitHub

- 仓库：`litra-readium`（如果不存在，用 `gh repo create` 创建；公开）。
- 默认分支：`main`。
- 每个功能用 `feat/...` 分支 + PR；允许自我合并，但保留 PR 记录。
- Conventional Commits。
- 不提交：keystore、`.jks`、token、本机绝对路径、`.DS_Store`、大文件构建产物。
- CI：push/PR 跑 assembleDebug + unit test + lint；tag `v*` 构建 APK 并创建 Release。
- 首发：`v0.1.0` preview；没有正式签名就用 debug APK，并在 release notes 说明。

### 版本与发布节奏

- **不要按里程碑发版**。整个开发周期只创建一个 GitHub Release：`v0.1.0`。
- 不在每个 PR / 每个里程碑打 tag；不要创建 nightly、alpha、beta 之类的大量版本。
- 只有核心验收通过（能导入、能阅读、99% 规则、书库/藏书/搜索/设置闭环）才打 `v0.1.0` tag。
- 如果首发后必须修，最多补一个 `v0.1.1`；不要连续刷版本号。
- 版本历史用 commits 和 PR 说明记录，不要用 release 代替。

## 工作方式

- 先做 M0 和 M1，不要一次写完全部功能。
- 每个里程碑结束：构建通过、测试通过、提交/推送、在 PR 或 issue 里写验收结果。
- 遇到不确定的产品细节，选符合 `docs/product-goal.md` 的默认方案，并记录在 README 的“已知限制”。
- 不要用假数据替代核心功能；封面、进度、EPUB 解析必须真实可用。
- 每个页面都要处理 loading / empty / error，不要只做 happy path。
- 修改 UI 时保持 MD3 组件和现代排版，不要退回 iOS 仿制。

## Definition of Done

- 能导入 EPUB；
- 能打开阅读、保存/恢复进度；
- 99% 与已读完规则正确；
- 书库双列/列表/藏书选项/已读完/搜索/详情/设置/阅读目标闭环；
- CI 绿；
- GitHub 仓库和 `v0.1.0` Release 完成。

## 首个任务

先完成 M0：

1. 检查/创建仓库；
2. 初始化 Android 工程；
3. 三 Tab + 10 个页面路由占位；
4. M3 主题；
5. CI；
6. README；
7. 推送 main；
8. 报告仓库 URL、构建状态、下一步。

然后继续 M1，不要停在空壳。
