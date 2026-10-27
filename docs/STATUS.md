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
