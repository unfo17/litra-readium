# GPT 启动提示（复制给 GPT）

你负责开发 Litra，一个原生 Android EPUB 阅读器。请先完整读取：

1. `AGENTS.md`
2. `docs/product-goal.md`
3. `mockup.html`（视觉参考，不是像素规范）

然后开始 M0 + M1。不要只搭空壳。

## 你的目标

用 **Kotlin + Jetpack Compose + Material 3 + Readium 3.4.0** 实现一个可安装、可导入、可阅读、可发布到 GitHub 的 Android 应用。

## 本轮任务

### M0 — 仓库与工程骨架

- 检查当前目录是否已经是 git 仓库；
- 如果没有 GitHub 仓库，使用已登录的 `gh` 创建 **public** 仓库 `litra-readium`，配置 origin，推送 main；
- 初始化 Android 工程：
  - Kotlin 2.4.x，AGP 9.x，compileSdk 37，minSdk 24；
  - Compose + Material 3；
  - 单 Activity；
  - 三 Tab：主页 / 书库 / 搜索；
  - 10 个页面路由占位（主页、书库双列、书库列表、藏书选项、搜索、已读完、书籍详情、阅读器、阅读目标、设置）；
  - 自定义暖色 M3 主题，不要默认紫色；
  - 底部 NavigationBar 用 M3 组件；
- 添加 `.gitignore`，排除 `.DS_Store`、build、local.properties、keystore；
- 添加 GitHub Actions：assembleDebug + unit test + lint；
- README：项目介绍、截图/原型链接、构建方式、当前状态；
- 首次提交并推送 main。

### M1 — 书库、导入、藏书

- Room 数据层：books、categories、book_categories、reading_sessions、goal_check_ins、search_history；
- SAF 导入 EPUB：
  - 复制到 app 私有目录；
  - 用 Readium 解析标题、作者、封面、语言、标识；
  - 查重、错误处理、封面落盘；
- 书库双列封面：
  - 一行两个；
  - 不显示书名/作者；
  - 进度文字在封面外左下角；
  - 已读完显示“已读完”，不显示 100%，不加对号；
  - “图书”=全部导入；
- 书库列表视图：左封面、右书名；
- 右下角 FAB：导入、筛选/排序/视图、藏书选项；
- 藏书选项页：
  - 全屏覆盖底栏；
  - 返回箭头，不显示“书库”；
  - 无“系统分类/我的分类”标题；
  - 选中分类加深；
  - 自定义分类复选框；
  - 单按钮“标记 → 删除”；
- 已读完独立页：无底栏、无书库标题，时间线；
- 每完成一个可运行阶段就提交、推送；保留 PR 记录。

## 关键规则

- 读到末尾进度最高 **99%**；不自动加对号；
- 99% 的主页卡片只显示“点击标记读完”；
- 标记完成后状态持久为 `FINISHED`，重读翻到中间也保持“已读完”；
- 书籍类型只有 **图书 / 漫画**；
- 不要使用 Apple 字体、图标、截图或素材；
- 不要提交密钥、签名文件、本机路径。

## 交付时报告

1. GitHub 仓库 URL；
2. main 最新提交；
3. CI 状态；
4. 已完成的里程碑；
5. 构建命令和安装包路径；
6. 已知限制和下一步。

如果 `gh` 未登录或没有权限创建仓库，先停下来说明具体错误，不要伪造仓库 URL。
