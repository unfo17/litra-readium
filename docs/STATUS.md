# 开发状态

## M0 — 已完成本地验证

- Android 工程：Kotlin 2.4.20、AGP 9.3.1、Gradle 9.7、compile/target SDK 37、minSdk 24。
- 三 Tab、十路由、Material 3 主题；CI 配置 debug 构建、单元测试、lint、R8 构建。
- 本地 `assembleDebug testDebugUnitTest lintDebug` 全部通过。
- 未创建 tag 或 Release；M1–M5 将按功能 PR 继续。

## M1 — 已完成本地验证

- SAF 多文件 EPUB 导入、Readium 元数据/封面、SHA-256 查重、Room 持久化。
- 双列/列表、排序、全屏藏书选项、自定义分类和已读完时间线。
- 本地 `assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest` 全部通过。
- 模拟器集成测试实际导入 10 本有效 EPUB；验证封面、查重、分类级联、历史最多 10 条、重启数据与 `FINISHED` 保持。

## M2 — 已完成本地与模拟器验证

- Readium 3.4.0 Compose 重排/固定版式导航器；目录、书内搜索、书签、排版与独立阅读主题。
- 完整 Locator 恢复；连续保存串行化；末页显示 99%，不会自动标记完成。
- `assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest` 全部通过。
- 导航器集成测试实际渲染 EPUB、翻到末页、保存/恢复 Locator 并检索正文；模拟器 SAF 导入并阅读 Project Gutenberg《Alice's Adventures in Wonderland》，末页数据库为 READING / 0.99。

## M3 — 已完成本地与模拟器验证

- 五区块主页、欲读大封面、99% 手动完成控件；本地书名/作者搜索与最多十条历史。
- 每天/每周/每月目标；前台阅读每 15 秒记账并在离开时补记；跨午夜拆分会话，历史周期保留目标快照。
- 目标修改于当前周期结束后生效，切换周期的首个周期可为部分周期。
- 构建、8 项单元测试、lint、4 项模拟器测试通过，覆盖三 Tab、视图切换、独立已读完页、搜索历史隐藏/显示、完成状态与目标周期。
