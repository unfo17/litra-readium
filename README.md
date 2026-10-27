# Litra

原生 Android 本地 EPUB 阅读器。Kotlin + Jetpack Compose + Material 3 + Readium 3.4.0。

## 构建

JDK 17、Android SDK 37.0、Build Tools 36。将 SDK 配置在 `ANDROID_HOME` 或本地 `local.properties`（不提交）。

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew assembleRelease
```

最低 Android 7.0（API 24）。默认分支 `main`；功能使用 `feat/...` + PR，Conventional Commits。CI 检查构建、测试、lint 和 R8。只在核心验收完成后发布 `v0.1.0` preview。

## 产品规则

读到末尾最多 99%；只有手动标记才保存 `FINISHED`，重读不回退。书库双列封面和列表，进度在封面外。主页、书库、搜索为三个主入口，其余页面全屏。

## 已知限制

目前处于 M0 初始化。仅承诺本地无 DRM EPUB；不做账号、云同步、书店、PDF 或有声书。
