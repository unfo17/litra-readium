Litra v0.1.0 preview — 原生 Android 本地 EPUB 阅读器。

**preview / debug signed**：本次 APK 使用构建环境的 debug 证书，非正式商店签名。Android 7.0/API 24 及以上。安装与功能验证基于 API 35 ARM64 模拟器；不同 debug 证书不能覆盖升级，卸载会清空应用内数据，请保留原始 EPUB。

### 本次功能

- 导入无 DRM EPUB 2/3、元数据/封面提取、查重与持久化。
- 双列/列表书库、分类、欲读、已读完时间线和详情。
- Readium 重排/固定版式阅读，目录、书内搜索、书签、排版与恢复位置。
- 读到末尾最高 99%；手动标记读完后，即使重读中间也保留已读完。
- 五区块主页、搜索与历史、设置、每天/每周/每月目标和周期打卡。

### 验证与限制

8 项单元测试、6 项设备测试、本地 debug/单元测试/lint/R8 验证通过；Actions 在发布时重新执行构建、单元测试、lint 和 R8。实际导入并阅读 Project Gutenberg EPUB，验证 99% 与恢复位置。

仅支持未加密 EPUB；PDF/有声书暂不支持。Readium Compose 导航器为实验 API。无账号、云同步或数据导出；进程被强制终止时可能丢失最后不足 15 秒的阅读计时。完整限制、功能说明、截图与第三方声明见 README。

随附 `SHA256SUMS.txt` 可校验 APK。本开发周期仅发布此一个版本，未创建里程碑 tag 或 nightly/alpha/beta Release。
