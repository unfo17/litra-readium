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

## M4 — 已完成构建与自动化验证

- 详情：全出血封面海报、居中信息、矩形简介/出版卡片、真实分类与元数据；可选择图书/漫画、分享信息。
- 设置：外观、独立阅读排版、常亮、历史开关、清除历史、关于；扩展 FAB 增加无障碍名称，大字体选项可换行。
- 构建、8 项单元测试、lint、R8 Release 构建、6 项模拟器测试均通过。
- 10 本导入测试混合 EPUB 2/3；另外验证固定版式渲染、设置与深色模式路由。
- 最终视觉验收、README 截图与唯一 v0.1.0 发布在 M5 完成。

## M5 — Preview 交付基线

- 本地最终命令：`python3 tools/build_with_curl.py assembleDebug testDebugUnitTest lintDebug assembleRelease connectedDebugAndroidTest --max-workers=4`，全部通过；8 项单元测试、6 项设备测试。
- 本机 JDK 17 已注册、SDK 37.0 已安装并接受 licenses；Java 直连 Maven 存在 TLS 握手问题，构建辅助脚本通过校验证书的 curl 访问同一官方源，仅在构建期间绑定 localhost，退出即关闭。常规 CI 无需此辅助脚本。
- README 含四张真实 EPUB 截图、构建说明与已知限制；CHANGELOG、发布说明、运行时依赖及字体版权声明已补齐，设置内可查看许可。
- 真实样书验收使用 API 35 ARM64 模拟器与 Project Gutenberg Alice、Frankenstein：SAF 多文件导入、真实封面和元数据、目录、阅读、恢复位置均可用。一本下载截断的样书被正确拒绝，损坏文件不会留下书库记录或私有文件。
- 手工操作证据：Alice 到末页为 READING / 0.99；主页只显示“点击标记读完”；手动完成后重新读到约 48%，数据库仍为 FINISHED，finishedAt 保留。正常退出恢复位置；前台会话时长自动落库。
- 手工分类验收：创建分类、标记、同一 FAB 切换为删除分类、确认删除，原两本书仍保留。已读完独立页、双列与列表、搜索、设置、目标由设备测试覆盖。
- 深色模式与字体缩放 2.0 已做模拟器视觉检查；系统栏图标、选项换行、排版滚动和触控按钮可用。扩展 FAB 与分类选中状态带无障碍语义；尚无多厂商真机/TalkBack 人工走查。
- 发布门槛：M5 PR 与 main 的构建、单元测试、lint、R8 检查通过后，才创建唯一 `v0.1.0` tag；tag 工作流生成 preview/debug signed APK、SHA256SUMS 和 GitHub Release。不创建里程碑 tag。
- 发布入口：[v0.1.0](https://github.com/unfo17/litra-readium/releases/tag/v0.1.0)。未覆盖风险与产品限制见 README“已知限制”。
- 最后复核补充：短暂数据库提交阶段不可取消，避免已提交书目在导入取消时丢失私有 EPUB；最后一次完整本地检查仍为 8 项 unit / 6 项 device 全通过。R8 压缩产物已用本地 debug 证书签名，实际安装、SAF 导入 Alice 并成功显示第一章正文，无 AndroidRuntime 崩溃记录。

## Post-release hardening — 未发布

- 详情页新增真实元数据标签（自定义分类、语言、格式、文件大小），类型切换仍独立保留。
- 主页横向卡片、书库双列/列表、搜索和欲读封面补充 `onClickLabel` / 长按语义；完成按钮带可读描述。
- Compose 状态改用 `collectAsStateWithLifecycle()`，依赖已在项目中显式存在。
- 移除 `INTERNET` 权限与 `usesCleartextTraffic`；本地 EPUB 的远程资源不再加载，已在 README 说明。
- 元数据标签与文件大小格式新增单元测试；版本保持 `0.1.0`，不创建 tag 或 Release。
- 本地 `assembleDebug testDebugUnitTest lintDebug assembleRelease` 通过，10 项单元测试通过；冷启动模拟器后 6 项设备测试全通过（包括 Readium 渲染/搜索）。

## v0.2.0 — UI refresh + missing features（`feat/v0.2.0`）

- 主页空区块完全折叠；阅读目标/今年读过常驻并显示今年数量和明确空状态。加入八种组合的 Compose 测试。
- Material 3 暖色统一、导航/主页显隐动画、Reader 淡入滑入工具栏和渐变底；支持窗口亮度、系统衬线/无衬线和跨书保存排版设置。
- API 35 设备上确认 Readium 不稳定转发中心点按，改用中央 36% 宽、30% 高透明点击区；两侧阅读、选取和链接仍可交互。中央区域内的点击会被用于切换工具栏。
- `ACTION_VIEW` / `SEND` / `SEND_MULTIPLE` 入口支持冷启动、暖启动与多 URI；检查 EPUB ZIP 内容和 `mimetype`，相同 SHA-256 打开已有书目。设备测试覆盖冷启动与 warm intent 去重。
- Settings 增加 SAF `.litrabackup` 导出/恢复：版本化 manifest、Room 全实体、EPUB/封面、校验哈希、zip-slip/大小/数量上限、阶段目录与事务合并；按 SHA-256 去重，完成状态保持，最新进度/完成时间优先。单测验证序列化、路径拒绝与冲突规则，设备测试以两本书、全部实体、重复恢复和故障回滚验证往返。
- README 已更新 7 张 API 35 实机截图：空/有内容主页、双列书库、Reader 显示/沉浸、详情、设置。图片中的样书为本地演示 EPUB，不随应用分发。
- 验证：`assembleDebug`、`testDebugUnitTest`（15 项）、`lintDebug`、`assembleRelease`、`compileDebugAndroidTestKotlin` 均通过；API 35 `connectedDebugAndroidTest` 10/10 通过。真实 EPUB 通过 `adb shell am start -a android.intent.action.VIEW ...` 导入并打开详情/阅读；中央点按隐藏系统栏与工具栏，`reader-center-tap-zone` 设备测试验证显隐。
- 已知限制：Readium WebView 不可靠转发中心点按，透明中心区会截获该区域文本选取/链接点按；API 35 冷启动模拟器第一次启用全屏会显示 Android 系统“一次性全屏”确认提示。其他厂商 WebView/真机尚未走查。
- 仍需完成：提交并推送功能分支，创建 PR，等待 CI 通过后合并 `main`。不创建新 tag/Release，版本继续为 `0.1.0`。
