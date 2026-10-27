# 阅读 App：严格按结构的三 Tab 设计稿

> 技术栈：Kotlin + Jetpack Compose + Material 3 + Readium Kotlin Toolkit  
> 这版**只按你给的树**：主页 4 块；书库 7 个藏书选项 + Scaffold 内容区；搜索框 + 搜索记录。  
> 阅读器是二级全屏页面，不占 Tab。  
> 不再额外增加“在读 / 试读 / 推荐 / 最近添加 / 浏览快捷入口 / 主页头像”等节点。

---

## 0. 结构（以此为准）

```text
阅读APP
├─主页
│  ├─继续阅读（卡片）
│  ├─之前读过（卡片）
│  ├─阅读目标（自定义阅读时长，周期性打卡记录）
│  └─今年读过
├─书库
│  ├─藏书选项（可点击）
│  │  ├─欲读清单
│  │  ├─已读完
│  │  ├─图书
│  │  ├─有声书
│  │  ├─PDF
│  │  ├─（自定义分类）
│  │  └─添加分类
│  └─Scaffold 导入与阅读过的（可排序，卡片）
└─搜索
   ├─搜索框
   └─搜索记录（没有就不显示）
```

### 0.1 节点定义

| Tab | 节点 | 定义 |
|---|---|---|
| 主页 | 继续阅读 | 当前 `READING` 状态的书，横向卡片，带进度 |
| 主页 | 之前读过 | 已有阅读记录、但不在“继续阅读”的书；V1 取 `FINISHED`，按最近阅读排序 |
| 主页 | 阅读目标 | 自定义周期（每天/每周/每月）和目标时长，自动累计阅读时长并生成周期性打卡记录 |
| 主页 | 今年读过 | 完成时间在本年的书，显示数量 + 卡片 |
| 书库 | 藏书选项 | 单选筛选器；点击切换当前藏书 |
| 书库 | 欲读清单 | `WANT_TO_READ` 状态，包含所有格式 |
| 书库 | 已读完 | `FINISHED` 状态，包含所有格式 |
| 书库 | 图书 | `EPUB` 格式的图书（V1 的主要阅读格式） |
| 书库 | 有声书 | `AUDIOBOOK` 格式；V1 保留入口，播放放 P1 |
| 书库 | PDF | `PDF` 格式 |
| 书库 | 自定义分类 | 用户创建的分类，多对多 |
| 书库 | 添加分类 | 弹窗输入名称，创建后追加到藏书选项 |
| 书库 | Scaffold 内容区 | 当前藏书下已导入/阅读过的书，可排序，卡片展示 |
| 搜索 | 搜索框 | 输入书名/作者，回车或输入后查书库 |
| 搜索 | 搜索记录 | 有记录才显示；没有记录时整块不渲染 |

> 搜索框输入后出现的“搜索结果”属于搜索框的行为，不在树里新增一级节点。

### 0.2 不新增的东西

- 不做主页“推荐 / 猜你喜欢 / 最近添加”。
- 不做书库“在读 / 试读版 / 全部”藏书选项；格式就是“图书 / 有声书 / PDF”。
- 不做搜索“浏览快捷入口 / 搜索建议 / 热门搜索”；只有搜索框和记录。
- 不做主页账户头像；需要设置入口时放在书库右上角菜单，不占主页结构。

---

## 1. 主页

### 1.1 布局

```text
┌─────────────────────────────────────────┐
│ 主页                                    │
│                                         │
│ 继续阅读                                │
│ ┌─────────────────────────────────────┐ │
│ │ ┌──────┐  三体                      │ │
│ │ │cover │  刘慈欣                    │ │
│ │ │82x123│  ▓▓▓▓░░░░ 32%             │ │
│ │ └──────┘                            │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ 之前读过                                │
│ ┌──────┐ ┌──────┐ ┌──────┐             │
│ │ 96x144│ │      │ │      │             │
│ └──────┘ └──────┘ └──────┘             │
│  活着     百年孤独  霍乱时期的爱情       │
│                                         │
│ 阅读目标                                │
│ ┌─────────────────────────────────────┐ │
│ │ 每天 30 分钟          连续 7 次      │ │
│ │ 18 / 30 分钟                         │ │
│ │ ▓▓▓▓▓▓░░░░░░                        │ │
│ │ 打卡  ● ● ● ● ○ ○ ○                 │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ 今年读过 3 本                           │
│ ┌──────┐ ┌──────┐ ┌──────┐             │
│ │  ✓   │ │  ✓   │ │  ✓   │             │
│ └──────┘ └──────┘ └──────┘             │
└─────────────────────────────────────────┘
```

主页只有这四个区块，按固定顺序排列：

1. 继续阅读
2. 之前读过
3. 阅读目标
4. 今年读过

### 1.2 继续阅读（卡片）

**数据**

- `status = READING`；
- `progress > 0`（刚打开还没翻页也可以显示，但按 `last_read_at` 排序）；
- 按 `last_read_at DESC`，最多 10 本。

**卡片**

- 横向 `LazyRow`；
- 单卡宽 300dp、高约 148dp；
- 左侧封面 82×123，右侧书名/作者/进度；
- 圆角 16dp，背景用分组背景色，不加厚阴影；
- 点击卡片：打开阅读器，恢复到上次 `Locator`；
- 长按卡片：打开图书操作（继续阅读 / 标记已读完 / 加入分类 / 移除）。

**空状态**

- 没有在读书：显示一张轻量空卡“还没有在读的书，去书库挑一本”；
- 点击空卡：跳到书库“图书”。

### 1.3 之前读过（卡片）

**数据**

- `status = FINISHED`；
- 按 `finished_at DESC` 或 `last_read_at DESC`，最多 10 本；
- 和“今年读过”允许重叠：一个表示“历史读过的书”，一个表示“本年读完的统计”。

**卡片**

- 横向 `LazyRow`；
- 小卡：封面 96×144，书名 2 行，作者 1 行；
- 已读完的封面右下角有勾选角标；
- 点击打开阅读器（已读完也可以重读）；
- 长按操作同上。

**空状态**

- 没有已读完的书：整块隐藏，不显示标题和空白。

### 1.4 阅读目标（自定义阅读时长，周期性打卡记录）

这是主页唯一一个“设置型”区块，不再拆成“每日目标 / 年度目标”两个概念。

**卡片内容**

```text
┌─────────────────────────────────────┐
│ 阅读目标                  连续 7 次  │
│ 每天 30 分钟                         │
│ 18 / 30 分钟                         │
│ ▓▓▓▓▓▓░░░░░░                        │
│ 最近打卡  ● ● ● ● ○ ○ ○            │
└─────────────────────────────────────┘
```

- 第一行：标题 + 连续打卡次数；
- 第二行：当前周期 + 目标时长（每天/每周/每月 + 分钟）；
- 第三行：当前周期已读分钟 / 目标分钟；
- 进度条；
- 最近 7 个周期的打卡点：实心=达标，空心=未达标/未开始；
- 点击卡片打开“阅读目标”弹层。

**目标弹层**

- 周期选择：`每天` / `每周` / `每月`；
- 目标时长：5–300 分钟，步进 5；
- 当前周期进度；
- 最近打卡记录列表：周期区间、已读分钟、是否达标、完成时间；
- 保存按钮；
- 修改目标只影响“下一个周期”，不重算历史记录。

**统计规则**

- 阅读器前台且屏幕亮起时计时；
- 单次会话 < 10 秒不计；
- 按本地时区计算周期：
  - 每天：当天 00:00–24:00；
  - 每周：周一 00:00–周日 24:00；
  - 每月：1 日 00:00–月末 24:00；
- 当前周期累计分钟数 ≥ 目标分钟数时，自动写入一条 `goal_check_ins`；
- 每个周期最多一条打卡记录；
- 连续次数：从当前周期往前，连续“已达标”的周期数；当前周期未达标但上一个周期达标时，显示上一周期的连续数；
- 修改周期后，当前周期从修改时重新开始，历史打卡保留。

**空状态**

- 首次使用：显示“设置一个阅读目标”，点击打开弹层；
- 目标默认值：每天 30 分钟（可改）。

### 1.5 今年读过

**数据**

- `status = FINISHED`；
- `finished_at` 在本地时区当年 1 月 1 日到次年 1 月 1 日之间；
- 按 `finished_at DESC`，最多 10 本；
- 标题右侧显示数量，例如“今年读过 3 本”。

**卡片**

- 小卡：封面 84×126，书名/作者；
- 封面右下角有“✓”角标；
- 点击打开阅读器；
- 没有书时显示“今年还没有读完的书”，不隐藏整个区块，保证结构稳定。

### 1.6 主页状态

- **首次启动、书库为空**：
  - 继续阅读显示空卡；
  - 之前读过隐藏；
  - 阅读目标显示“设置一个阅读目标”；
  - 今年读过显示 0 本。
- **加载中**：四个区块的标题照常显示，内容用骨架屏。
- **数据库异常**：顶部 `Snackbar` + 重试，不清空页面。

---

## 2. 书库

### 2.1 布局

```text
┌─────────────────────────────────────────┐
│ 书库                            ＋  ⇅   │  ← 导入 / 排序
│ [欲读清单] [已读完] [图书] [有声书]     │
│ [PDF] [自定义分类] [+ 添加分类]         │
│                                         │
│ 最近阅读 ▾                              │
│ ┌──────┐ ┌──────┐ ┌──────┐             │
│ │cover │ │cover │ │cover │             │
│ └──────┘ └──────┘ └──────┘             │
│  三体      活着      百年孤独           │
│  刘慈欣    余华      马尔克斯           │
│  ▓▓░32%   ✓已读完   ▓░░8%              │
└─────────────────────────────────────────┘
```

### 2.2 页面用 Scaffold

书库页面内部用一个 `Scaffold`：

```kotlin
Scaffold(
    topBar = {
        LibraryTopBar(
            onImport = { openDocumentPicker() },
            onSort = { sortMenuExpanded = true },
        )
    },
    containerColor = LocalBooksColors.current.background,
) { padding ->
    Column(Modifier.padding(padding)) {
        CollectionOptions(
            selected = selectedCollection,
            categories = categories,
            onSelect = { selectedCollection = it },
            onAddCategory = { showAddCategoryDialog = true },
        )
        SortLabel(sort = sort)
        BookGrid(
            books = books,
            onOpen = onOpenBook,
            onLongPress = { showBookActions(it) },
        )
    }
}
```

- `Scaffold` 只负责书库自己的 topBar 和内容；
- 底部三个 Tab 由外层根 `Scaffold` 负责，书库内部不再放底栏；
- topBar 右侧只有两个动作：导入、排序；
- 不增加“列表/网格切换”“多选”“编辑”按钮。

### 2.3 藏书选项

横向滚动，单选，点击立即筛选。

| 选项 | 类型 | 筛选规则 | 说明 |
|---|---|---|---|
| 欲读清单 | 状态 | `status = WANT_TO_READ` | 所有格式 |
| 已读完 | 状态 | `status = FINISHED` | 所有格式 |
| 图书 | 格式 | `format = EPUB` | 默认选中；V1 主要阅读格式 |
| 有声书 | 格式 | `format = AUDIOBOOK` | V1 只保留入口，播放 P1 |
| PDF | 格式 | `format = PDF` | 用 PDF 阅读器打开 |
| 自定义分类 | 归属 | `book_categories` 关联 | 可多个 |
| 添加分类 | 动作 | 弹窗创建 | 创建后追加到列表末尾、在“添加分类”之前 |

规则：

- 同一时间只选中一个藏书选项；
- 默认选中“图书”；
- 切换藏书时保留滚动位置和排序；
- 选中项用强调色胶囊，未选中用次级背景；
- “添加分类”始终在最后，图标 `+`；
- 自定义分类长按可以重命名/删除（删除分类不删除图书）；
- 不增加“全部 / 在读 / 试读版”选项。

### 2.4 Scaffold 内容区：导入与阅读过的书

**内容范围**

- 当前藏书下所有已导入的书；
- 包括未读、在读、已读完；
- 卡片上显示状态和进度，不额外分“已导入”“阅读过”两个列表。

**排序**

顶部一行显示当前排序，点击或在 topBar 排序图标打开菜单：

| 排序 | 规则 |
|---|---|
| 最近阅读 | `last_read_at DESC`，空值按 `created_at` |
| 最近添加 | `created_at DESC` |
| 书名 | 本地化 Collator，中文按拼音/系统排序 |
| 作者 | 本地化 Collator |

- 默认“最近阅读”；
- 每个藏书单独记忆排序；
- 排序变化后卡片网格直接重排，不做整页转场。

**卡片**

- 手机 3 列，平板 5 列；
- 封面 2:3，圆角 4dp，轻阴影；
- 书名 13sp Semibold，最多 2 行；
- 作者 12sp 次级色，最多 1 行；
- 封面底部 2dp 进度条；
- 已读完：封面右下角勾选角标；
- 有声书：封面角标耳机图标（如果 V1 显示）；
- PDF：封面角标 `PDF`；
- 长按：图书操作弹层。

**空状态**

| 藏书 | 空状态 |
|---|---|
| 欲读清单 | “还没有想读的书。在图书里长按一本书，选择加入欲读清单。” |
| 已读完 | “读完的书会出现在这里。” |
| 图书 | “书库还是空的。导入 EPUB，开始建立书架。” + 导入按钮 |
| 有声书 | “暂不支持有声书播放。” |
| PDF | “还没有 PDF。导入 PDF 后会出现在这里。” |
| 自定义分类 | “这个分类还没有书。长按图书，选择加入分类。” |

### 2.5 导入

入口：

- 书库 topBar 的 `＋`；
- 图书/PDF 空状态里的“导入”按钮；
- 系统分享/打开方式。

流程：

1. `OpenMultipleDocuments` 选择文件；
2. 接受 `application/epub+zip` 和 `application/pdf`；
3. 复制到缓存，计算 SHA-256 查重；
4. 用 Readium 打开，读取标题、作者、封面；
5. 移动到应用私有目录，写 Room；
6. 关闭 `Publication`；
7. 如果导入格式不是当前藏书：
   - 例如当前在“图书”，导入的是 PDF；
   - 显示 `Snackbar`：“已导入 1 本 PDF，可在 PDF 中查看”；
   - 不强制切换藏书，避免打断用户。

错误提示：

- 文件损坏；
- 有 DRM；
- 格式不支持；
- 重复；
- 存储空间不足。

### 2.6 图书操作

长按卡片打开 `ModalBottomSheet`：

- 头部：封面 + 书名 + 作者 + 格式/状态；
- 打开 / 继续阅读；
- 加入欲读清单 / 移出欲读清单；
- 标记已读完 / 标记未读完；
- 加入分类（多选自定义分类）；
- 图书信息；
- 从书库移除（二次确认，删除文件和记录）。

不做多选批量操作；这是 V1 的边界。

### 2.7 添加分类

- 点击 `+ 添加分类` 打开 `AlertDialog`；
- 输入名称，1–20 字；
- 校验重名；
- 创建后写入 `categories`，并自动选中新分类；
- 新分类显示在自定义分类之后、“添加分类”之前；
- 长按自定义分类可以重命名/删除。

---

## 3. 搜索

### 3.1 布局

有搜索记录：

```text
┌─────────────────────────────────────────┐
│ ┌─────────────────────────────────────┐ │
│ │ 🔍 搜索书名、作者                   │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ 搜索记录                          清除  │
│ 🕘 三体                             ×   │
│ 🕘 余华                             ×   │
│ 🕘 百年孤独                         ×   │
└─────────────────────────────────────────┘
```

没有搜索记录：

```text
┌─────────────────────────────────────────┐
│ ┌─────────────────────────────────────┐ │
│ │ 🔍 搜索书名、作者                   │ │
│ └─────────────────────────────────────┘ │
│                                         │
│              （空白，不显示“搜索记录”）  │
└─────────────────────────────────────────┘
```

- 搜索页只有搜索框；
- 不加“搜索”大标题、不加“浏览”快捷入口、不加“猜你想搜”；
- 有记录才显示“搜索记录”区块；
- 没有记录就什么都不显示，保持空白，不出现“暂无搜索记录”提示。

### 3.2 搜索记录

- 最多 10 条；
- 按 `last_used_at DESC`；
- 点击记录：把词填回搜索框并执行搜索；
- 右侧 `×` 删除单条；
- 右上角“清除”清空全部，二次确认；
- 只在“提交搜索”或“点击搜索结果”时写入，不在每次输入时写入；
- 没有记录时整块不渲染。

### 3.3 搜索结果

输入非空后，搜索框下方显示结果；这是搜索框的行为，不是新增页面节点。

- 结果行：封面 48×72 + 书名 + 作者 + 状态/进度；
- 书名命中高亮；
- 点击打开阅读器；
- 无结果：“没有找到‘xxx’” + 修改关键词提示；
- 结果最多 50 条；
- 不加筛选 tab、不加排序菜单、不加搜索建议。

### 3.4 搜索范围

V1 只搜：

- 书名；
- 作者；
- 自定义分类名称（可选）。

不搜正文。正文搜索在阅读器里的“书内搜索”，不要混在一起。

### 3.5 搜索状态

| 状态 | 条件 | UI |
|---|---|---|
| 初始 | 查询为空，有记录 | 搜索框 + 搜索记录 |
| 初始无记录 | 查询为空，无记录 | 只有搜索框 |
| 结果 | 查询非空，有匹配 | 结果列表 |
| 无结果 | 查询非空，无匹配 | 空状态 |
| 错误 | 查询异常 | 错误提示 + 重试 |

---

## 4. 视觉：照 Apple Books 的气质实现

### 4.1 关键词

- 大标题、宽留白、轻阴影；
- 封面是主角，卡片容器退后；
- 半透明/浅灰底栏，细分隔线；
- 强调色只用于选中、进度、关键数字；
- 不用 Material 默认紫色、不用大圆角彩色卡片。

### 4.2 颜色

| Token | Light | Dark | 用途 |
|---|---|---|---|
| `accent` | `#C2410C` | `#FF9F0A` | 选中、进度、链接 |
| `background` | `#FFFFFF` | `#000000` | 页面背景 |
| `groupedBackground` | `#F2F2F7` | `#1C1C1E` | 目标卡、搜索框、继续阅读卡 |
| `label` | `#000000` | `#FFFFFF` | 主文字 |
| `secondaryLabel` | `#3C3C43` 60% | `#EBEBF5` 60% | 作者、元信息 |
| `separator` | `#3C3C43` 29% | `#545C5C5E` | 发丝线 |
| `destructive` | `#FF3B30` | `#FF453A` | 删除 |
| `tabBar` | `#FFFFFF` 96% | `#000000` 96% | 底栏 |

不要打包 SF Pro；用系统默认字体。中文不要负字距，英文大标题可以 `-0.4.sp`。

### 4.3 字号

| 角色 | 字号/行高 | 字重 | 用途 |
|---|---|---|---|
| 大标题 | 34 / 41 | Bold | 主页、书库 |
| 区块标题 | 22 / 28 | Bold | 继续阅读、之前读过、阅读目标、今年读过 |
| 卡片标题 | 17 / 22 | Semibold | 继续阅读卡、结果行 |
| 小卡标题 | 13 / 17 | Semibold | 之前读过、今年读过 |
| 正文 | 17 / 24 | Regular | 说明 |
| 元信息 | 13 / 18 | Regular | 作者、进度 |
| 角标 | 11 / 13 | Medium | PDF、已读完 |
| Tab | 10 / 12 | Medium | 底部 |

### 4.4 组件映射

| Apple Books 元素 | Compose 实现 | 注意 |
|---|---|---|
| 底部 Tab Bar | 自定义 `Row` + `navigationBarsPadding` | 不用 M3 `NavigationBar` 的 80dp 和胶囊指示器 |
| 大标题 | 自定义 `LargeTitleHeader` | 不用 `LargeTopAppBar`，高度和字重不对 |
| 书库藏书胶囊 | 横向 `LazyRow` + 自定义 `Pill` | 单选，选中强调色 |
| 书库内容区 | `Scaffold` + `LazyVerticalGrid` | 3 列卡片，轻阴影 |
| 继续阅读卡 | 横向 `LazyRow` + 左右布局卡 | 300dp 宽，封面 + 信息 |
| 阅读目标卡 | 自定义卡 + 进度条 + 打卡点 | 点击打开 `ModalBottomSheet` |
| 搜索框 | `BasicTextField` 自定义 | 不用 M3 `SearchBar` |
| 搜索记录 | `LazyColumn` + 时钟图标 | 无记录整块不渲染 |
| 图书操作 | `ModalBottomSheet` | 长按触发，带触觉 |
| 分类弹窗 | `AlertDialog` | 简单输入 + 校验 |
| 毛玻璃 | 可选 Haze | `Modifier.blur` 不能做背景模糊 |

---

## 5. Compose 实现

### 5.1 工程结构

```text
app/src/main/java/com/litra/reader/
├─ MainActivity.kt
├─ LitraApp.kt
├─ core/
│  ├─ designsystem/
│  │  ├─ BooksTheme.kt
│  │  ├─ BooksType.kt
│  │  └─ components/
│  │     ├─ BooksTabBar.kt
│  │     ├─ LargeTitleHeader.kt
│  │     ├─ BookCover.kt
│  │     ├─ ThinProgressBar.kt
│  │     ├─ SearchField.kt
│  │     └─ BookActionsSheet.kt
│  └─ navigation/
├─ data/
│  ├─ db/
│  ├─ repository/
│  ├─ readium/
│  └─ preferences/
├─ domain/
└─ feature/
   ├─ home/
   ├─ library/
   ├─ search/
   └─ reader/
```

### 5.2 主题

```kotlin
@Immutable
data class BooksColors(
    val groupedBackground: Color,
    val background: Color,
    val label: Color,
    val secondaryLabel: Color,
    val separator: Color,
    val accent: Color,
    val tabBar: Color,
    val destructive: Color,
)

val LightBooksColors = BooksColors(
    groupedBackground = Color(0xFFF2F2F7),
    background = Color(0xFFFFFFFF),
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    separator = Color(0x4A3C3C43),
    accent = Color(0xFFC2410C),
    tabBar = Color(0xFFFFFFFF),
    destructive = Color(0xFFFF3B30),
)

val DarkBooksColors = BooksColors(
    groupedBackground = Color(0xFF1C1C1E),
    background = Color(0xFF000000),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0xB3EBEBF5),
    separator = Color(0x545C5C5E),
    accent = Color(0xFFFF9F0A),
    tabBar = Color(0xFF000000),
    destructive = Color(0xFFFF453A),
)

val LocalBooksColors = staticCompositionLocalOf { LightBooksColors }
```

```kotlin
private val LightScheme = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0x993C3C43),
    outline = Color(0x4D3C3C43),
    surfaceTint = Color.Transparent,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFF9F0A),
    onPrimary = Color(0xFF3B1D00),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xB3EBEBF5),
    outline = Color(0x545C5C5E),
    surfaceTint = Color.Transparent,
)

private val BooksTypography = Typography(
    headlineSmall = BooksType.sectionTitle,
    titleLarge = BooksType.listTitle,
    bodyLarge = BooksType.body,
    labelSmall = BooksType.tab,
)

private val BooksShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun LitraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val books = if (darkTheme) DarkBooksColors else LightBooksColors
    CompositionLocalProvider(LocalBooksColors provides books) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = BooksTypography,
            shapes = BooksShapes,
            content = content,
        )
    }
}
```

### 5.3 底部 Tab Bar

```kotlin
enum class BooksTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val route: Any,
) {
    Home("主页", Icons.Outlined.Home, Icons.Filled.Home, HomeRoute),
    Library("书库", Icons.Outlined.MenuBook, Icons.Filled.MenuBook, LibraryRoute),
    Search("搜索", Icons.Outlined.Search, Icons.Filled.Search, SearchRoute),
}
```

```kotlin
@Composable
fun BooksTabBar(
    selected: BooksTab,
    onSelect: (BooksTab) -> Unit,
) {
    val colors = LocalBooksColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.tabBar.copy(alpha = 0.96f))
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            thickness = Dp.Hairline,
            color = colors.separator,
        )
        Row(Modifier.fillMaxWidth().height(49.dp)) {
            BooksTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(tab) },
                            role = Role.Tab,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                        contentDescription = null,
                        tint = if (isSelected) colors.accent else colors.secondaryLabel,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = tab.label,
                        style = BooksType.tab,
                        color = if (isSelected) colors.accent else colors.secondaryLabel,
                    )
                }
            }
        }
    }
}
```

### 5.4 主页

```kotlin
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenBook: (String) -> Unit,
    onOpenGoal: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            LargeTitleHeader(title = "主页")
        }

        item {
            SectionHeader(title = "继续阅读")
        }
        item {
            ContinueReadingRail(
                books = state.continuing,
                onOpen = onOpenBook,
                onEmptyClick = onOpenLibrary,
            )
        }

        if (state.previouslyRead.isNotEmpty()) {
            item {
                SectionHeader(title = "之前读过")
            }
            item {
                MiniBookRail(
                    books = state.previouslyRead,
                    onOpen = onOpenBook,
                )
            }
        }

        item {
            SectionHeader(title = "阅读目标")
        }
        item {
            ReadingGoalCard(
                goal = state.goal,
                checkIns = state.recentCheckIns,
                onClick = onOpenGoal,
            )
        }

        item {
            SectionHeader(
                title = "今年读过",
                trailing = "${state.readThisYear.size} 本",
            )
        }
        item {
            MiniBookRail(
                books = state.readThisYear,
                onOpen = onOpenBook,
            )
        }
    }
}
```

继续阅读卡片：

```kotlin
@Composable
fun ContinueReadingCard(
    book: BookUi,
    onClick: () -> Unit,
) {
    val colors = LocalBooksColors.current
    Row(
        Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.groupedBackground)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(
            coverPath = book.coverPath,
            title = book.title,
            progress = null,
            modifier = Modifier.width(82.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = BooksType.listTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = book.authors,
                style = BooksType.caption,
                color = colors.secondaryLabel,
                maxLines = 1,
            )
            Spacer(Modifier.height(10.dp))
            ThinProgressBar(progress = book.progress)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${(book.progress * 100).roundToInt()}%",
                style = BooksType.caption,
                color = colors.accent,
            )
        }
    }
}
```

阅读目标卡：

```kotlin
@Composable
fun ReadingGoalCard(
    goal: GoalUi,
    checkIns: List<CheckInUi>,
    onClick: () -> Unit,
) {
    val colors = LocalBooksColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.groupedBackground)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("阅读目标", style = BooksType.listTitle)
            Spacer(Modifier.weight(1f))
            Text("连续 ${goal.streak} 次", style = BooksType.caption, color = colors.secondaryLabel)
        }
        Spacer(Modifier.height(6.dp))
        Text("${goal.period.label} ${goal.targetMinutes} 分钟", style = BooksType.caption, color = colors.secondaryLabel)
        Spacer(Modifier.height(8.dp))
        Text("${goal.minutes} / ${goal.targetMinutes} 分钟", style = BooksType.sectionTitle)
        Spacer(Modifier.height(8.dp))
        ThinProgressBar(progress = goal.minutes.toFloat() / goal.targetMinutes)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            checkIns.takeLast(7).forEach { checkIn ->
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (checkIn.completed) colors.accent
                            else colors.separator
                        )
                )
            }
        }
    }
}
```

`BooksType` 定义：

```kotlin
object BooksType {
    private val base = TextStyle(fontFamily = FontFamily.Default)

    val largeTitle = base.copy(
        fontSize = 34.sp,
        lineHeight = 41.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
    )
    val sectionTitle = base.copy(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    )
    val listTitle = base.copy(
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val body = base.copy(fontSize = 17.sp, lineHeight = 24.sp)
    val gridTitle = base.copy(
        fontSize = 13.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val caption = base.copy(fontSize = 13.sp, lineHeight = 18.sp)
    val tab = base.copy(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium)
}
```

### 5.5 书库

```kotlin
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onSelectCollection: (CollectionKey) -> Unit,
    onAddCategory: () -> Unit,
    onImport: () -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onOpenBook: (String) -> Unit,
    onLongPressBook: (String) -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LibraryTopBar(
                onImport = onImport,
                onSort = { sortMenuExpanded = true },
            )
        },
        containerColor = LocalBooksColors.current.background,
    ) { padding ->
        Column(Modifier.padding(padding)) {
            CollectionOptions(
                selected = state.selectedCollection,
                categories = state.categories,
                onSelect = onSelectCollection,
                onAddCategory = onAddCategory,
            )
            SortLabel(sort = state.sort)
            BookGrid(
                books = state.books,
                onOpen = onOpenBook,
                onLongPress = onLongPressBook,
            )
        }

        DropdownMenu(
            expanded = sortMenuExpanded,
            onDismissRequest = { sortMenuExpanded = false },
        ) {
            SortOrder.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.label) },
                    onClick = {
                        onSortChange(sort)
                        sortMenuExpanded = false
                    },
                )
            }
        }
    }
}
```

藏书胶囊：

```kotlin
@Composable
fun CollectionOptions(
    selected: CollectionKey,
    categories: List<CategoryUi>,
    onSelect: (CollectionKey) -> Unit,
    onAddCategory: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(BuiltInCollection.entries) { collection ->
            CollectionPill(
                text = collection.label,
                selected = selected == CollectionKey.BuiltIn(collection),
                onClick = { onSelect(CollectionKey.BuiltIn(collection)) },
            )
        }
        items(categories, key = { it.id }) { category ->
            CollectionPill(
                text = category.name,
                selected = selected == CollectionKey.Category(category.id),
                onClick = { onSelect(CollectionKey.Category(category.id)) },
            )
        }
        item {
            AddCategoryPill(onClick = onAddCategory)
        }
    }
}
```

内置藏书枚举：

```kotlin
enum class BuiltInCollection(val label: String) {
    WANT_TO_READ("欲读清单"),
    FINISHED("已读完"),
    BOOKS("图书"),
    AUDIOBOOKS("有声书"),
    PDFS("PDF"),
}

sealed interface CollectionKey {
    data class BuiltIn(val collection: BuiltInCollection) : CollectionKey
    data class Category(val id: String) : CollectionKey
}
```

书库网格：

```kotlin
@Composable
fun BookGrid(
    books: List<BookUi>,
    onOpen: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 96.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(books, key = { it.id }) { book ->
            BookGridItem(
                book = book,
                onClick = { onOpen(book.id) },
                onLongClick = { onLongPress(book.id) },
            )
        }
    }
}
```

### 5.6 搜索

```kotlin
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    history: List<SearchHistoryUi>,
    results: List<BookUi>,
    onDeleteHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenBook: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
        )

        when {
            query.isNotBlank() -> {
                SearchResults(
                    query = query,
                    results = results,
                    onOpenBook = onOpenBook,
                )
            }
            history.isNotEmpty() -> {
                SearchHistory(
                    history = history,
                    onDelete = onDeleteHistory,
                    onClear = onClearHistory,
                )
            }
            else -> {
                // 没有搜索记录：保持空白，不渲染“搜索记录”区块
            }
        }
    }
}
```

搜索框：

```kotlin
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val colors = LocalBooksColors.current
    val keyboard = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = BooksType.body.copy(color = colors.label),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.groupedBackground),
        decorationBox = { innerTextField ->
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.secondaryLabel,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "搜索书名、作者",
                            style = BooksType.body,
                            color = colors.secondaryLabel,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "清空",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        },
    )
}
```

搜索记录：

```kotlin
@Composable
fun SearchHistory(
    history: List<SearchHistoryUi>,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("搜索记录", style = BooksType.sectionTitle)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClear) {
                Text("清除", style = BooksType.caption)
            }
        }
        history.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = LocalBooksColors.current.secondaryLabel,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = item.query,
                    style = BooksType.body,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onDelete(item.query) }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "删除记录",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
```

### 5.7 导航

```kotlin
@Serializable data object HomeRoute
@Serializable data object LibraryRoute
@Serializable data object SearchRoute
@Serializable data class ReaderRoute(val bookId: String)
```

```kotlin
@Composable
fun LitraApp() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val showTabBar = entry?.destination?.let { dest ->
        dest.hasRoute<HomeRoute>() ||
            dest.hasRoute<LibraryRoute>() ||
            dest.hasRoute<SearchRoute>()
    } ?: true

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showTabBar) {
                BooksTabBar(
                    selected = currentTab(entry),
                    onSelect = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onOpenBook = { navController.navigate(ReaderRoute(it)) },
                    onOpenLibrary = { navController.navigate(LibraryRoute) },
                    onOpenGoal = { /* show goal sheet */ },
                )
            }
            composable<LibraryRoute> {
                LibraryScreen(
                    onOpenBook = { navController.navigate(ReaderRoute(it)) },
                )
            }
            composable<SearchRoute> {
                SearchScreen(
                    onOpenBook = { navController.navigate(ReaderRoute(it)) },
                )
            }
            composable<ReaderRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ReaderRoute>()
                ReaderScreen(
                    bookId = route.bookId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
```

```kotlin
private fun currentTab(entry: NavBackStackEntry?): BooksTab =
    when {
        entry?.destination?.hasRoute<LibraryRoute>() == true -> BooksTab.Library
        entry?.destination?.hasRoute<SearchRoute>() == true -> BooksTab.Search
        else -> BooksTab.Home
    }
```

### 5.8 通用组件

```kotlin
@Composable
fun LargeTitleHeader(title: String) {
    Text(
        text = title,
        style = BooksType.largeTitle,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
    )
}

@Composable
fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = BooksType.sectionTitle)
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = trailing,
                style = BooksType.caption,
                color = LocalBooksColors.current.secondaryLabel,
            )
        }
    }
}

@Composable
fun ThinProgressBar(progress: Float) {
    val value = progress.coerceIn(0f, 1f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(LocalBooksColors.current.separator),
    ) {
        Box(
            Modifier
                .fillMaxWidth(value)
                .fillMaxHeight()
                .background(LocalBooksColors.current.accent),
        )
    }
}

@Composable
fun BookCover(
    coverPath: String?,
    title: String,
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier
            .aspectRatio(2f / 3f)
            .shadow(
                elevation = 7.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.20f),
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (coverPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(coverPath))
                    .crossfade(true)
                    .build(),
                contentDescription = "《$title》封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(title.take(1), style = BooksType.sectionTitle)
            }
        }
        if (progress != null && progress > 0f) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.Black.copy(alpha = 0.12f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(LocalBooksColors.current.accent),
                )
            }
        }
    }
}
```

书库卡片：

```kotlin
@Composable
fun BookGridItem(
    book: BookUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        BookCover(
            coverPath = book.coverPath,
            title = book.title,
            progress = book.progress,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = book.title,
            style = BooksType.gridTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = book.authors,
            style = BooksType.caption,
            color = LocalBooksColors.current.secondaryLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
```

> `BooksType.gridTitle` 是 13sp Semibold；`combinedClickable` 在部分 Compose 版本需要 `@OptIn(ExperimentalFoundationApi::class)`。

---

## 6. Readium 接入

阅读器不在三个 Tab 的树里，但从主页/书库/搜索点书必须能打开。

### 6.1 依赖

截至 2026-10，Readium 3.4.0：

```kotlin
dependencies {
    implementation("org.readium.kotlin-toolkit:readium-shared:3.4.0")
    implementation("org.readium.kotlin-toolkit:readium-streamer:3.4.0")
    implementation("org.readium.kotlin-toolkit:readium-navigator:3.4.0")

    // EPUB 的 Compose 导航器（实验 API）
    implementation("org.readium.kotlin-toolkit:readium-navigator-web-reflowable:3.4.0")
    implementation("org.readium.kotlin-toolkit:readium-navigator-web-fixedlayout:3.4.0")

    // PDF 可选
    // implementation("org.readium.kotlin-toolkit:readium-adapter-pdfium:3.4.0")
}
```

要求：`minSdk 24`、`compileSdk 37`、core library desugaring。  
新 Compose 导航器目前是 **Experimental API**，锁定 3.4.0，封装一层再升级。

### 6.2 打开出版物

```kotlin
@Singleton
class ReadiumService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(
        contentResolver = context.contentResolver,
        httpClient = httpClient,
    )
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context = context,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
        ),
    )

    suspend fun open(file: File): Publication =
        withContext(Dispatchers.IO) {
            val asset = assetRetriever.retrieve(file.toUrl()).getOrThrow()
            publicationOpener.open(asset, allowUserInteraction = false).getOrThrow()
        }
}
```

导入时读取：

```kotlin
suspend fun import(uri: Uri): ImportResult {
    val temp = copyToCache(uri)
    val publication = readium.open(temp)
    try {
        val title = publication.metadata.title
        val authors = publication.metadata.authors.joinToString("、") { it.name }
        val cover = publication.coverFitting(Size(600, 900))
        // 移动到 files/books/{id}.epub，写封面、写 Room
    } finally {
        publication.close()
    }
}
```

### 6.3 阅读器

EPUB 用 Compose 导航器：

```kotlin
@OptIn(ExperimentalReadiumApi::class)
@Composable
fun EpubRendition(
    publication: Publication,
    initialLocator: Locator?,
    onProgress: (Locator) -> Unit,
) {
    val app = LocalContext.current.applicationContext as Application
    val factory = remember(publication) {
        ReflowableWebRenditionFactory(
            application = app,
            publication = publication,
        )
    }

    var state by remember(publication) {
        mutableStateOf<ReflowableWebRenditionState?>(null)
    }

    LaunchedEffect(factory) {
        val f = factory ?: return@LaunchedEffect
        state = f.createRenditionState(
            initialLocation = initialLocator?.let { ReflowableWebGoLocation(it) },
            initialPreferences = ReflowableWebPreferences.LightTheme,
        ).getOrNull()
    }

    val rendition = state ?: return
    ReflowableWebRendition(
        state = rendition,
        modifier = Modifier.fillMaxSize(),
    )

    val controller = rendition.controller
    LaunchedEffect(controller) {
        if (controller == null) return@LaunchedEffect
        snapshotFlow { controller.location }
            .map { it.toLocator() }
            .distinctUntilChanged()
            .debounce(500)
            .collect(onProgress)
    }
}
```

- `createRenditionState` 是 `suspend`；
- `controller` 第一次布局后才可用，必须判空；
- 不要放进 `LazyColumn`；
- 退出阅读器先离开 Compose 组合，再 `publication.close()`。

### 6.4 进度保存与恢复

保存完整 `Locator`：

```kotlin
bookRepository.saveProgress(
    bookId = bookId,
    locatorJson = locator.toJSON().toString(),
    totalProgression = locator.locations.totalProgression ?: 0.0,
    lastReadAt = System.currentTimeMillis(),
)
```

恢复：

```kotlin
val initialLocator = book.currentLocatorJson
    ?.let { runCatching { Locator.fromJSON(JSONObject(it)) }.getOrNull() }
```

- 主页/书库卡片只用 `total_progression`；
- 退出时立即保存一次，防抖只用于频繁滚动；
- 到 99% 自动标记 `FINISHED` 并写 `finished_at`。

### 6.5 目录、书内搜索、阅读设置

- 目录：递归拍平 `publication.tableOfContents`，点击 `controller.goTo(ReflowableWebGoLocation(href = link.url()))`；
- 书内搜索：`publication.search(query)`，用 `iterator.next()` 分页，点击结果跳转 `ReflowableWebGoLocation(locator)`；
- 阅读设置：`controller.preferences = controller.preferences.copy(fontSize = ..., lineHeight = ..., scroll = ..., backgroundColor = ..., textColor = ...)`；
- 阅读器主题独立于 App 深色模式。

### 6.6 PDF 与有声书

- **PDF**：新 Compose 导航器只支持 EPUB；PDF 用 `PdfNavigatorFragment`（PDFium），通过 `AndroidView`/`FragmentContainerView` 承载，阅读器外壳共用；
- **有声书**：书库入口保留，V1 不做播放；如果导入有声书，卡片可显示，点击提示“有声书播放将在后续版本支持”；
- 如果 V1 不想出现“半成品”，可以在导入时不接受 AUDIOBOOK，但书库入口仍保留空状态。

---

## 7. 数据模型

### 7.1 表

**books**

| 字段 | 说明 |
|---|---|
| `id` | UUID |
| `title` | 书名 |
| `authors` | 作者，连接字符串 |
| `format` | `EPUB` / `PDF` / `AUDIOBOOK` |
| `status` | `UNREAD` / `WANT_TO_READ` / `READING` / `FINISHED` |
| `file_path` | 私有目录文件 |
| `cover_path` | 封面文件 |
| `file_hash` | 查重 |
| `total_progression` | 0..1 |
| `current_locator_json` | 完整 Locator |
| `created_at` | 导入时间 |
| `last_read_at` | 最近阅读 |
| `finished_at` | 读完时间 |

**categories**

| 字段 | 说明 |
|---|---|
| `id` | UUID |
| `name` | 分类名 |
| `created_at` | 创建时间 |

**book_categories**

| 字段 | 说明 |
|---|---|
| `book_id` | 图书 |
| `category_id` | 分类 |
| `added_at` | 加入时间 |

**reading_goals**

| 字段 | 说明 |
|---|---|
| `id` | 固定 1 |
| `period` | `DAILY` / `WEEKLY` / `MONTHLY` |
| `target_minutes` | 目标分钟 |
| `updated_at` | |

**reading_sessions**

| 字段 | 说明 |
|---|---|
| `id` | UUID |
| `book_id` | 图书 |
| `started_at` | 开始 |
| `duration_seconds` | 前台阅读秒数 |
| `local_day` | `yyyy-MM-dd` |

**goal_check_ins**

| 字段 | 说明 |
|---|---|
| `id` | UUID |
| `period` | `DAILY` / `WEEKLY` / `MONTHLY` |
| `period_start` | 周期开始 |
| `period_end` | 周期结束 |
| `target_minutes` | 当时目标 |
| `minutes` | 实际分钟 |
| `completed_at` | 达标时间 |
| 唯一约束 | `(period, period_start)` |

**search_history**

| 字段 | 说明 |
|---|---|
| `query` | 主键 |
| `last_used_at` | 最近使用 |

### 7.2 关键查询

```sql
-- 继续阅读
SELECT * FROM books
WHERE status = 'READING'
ORDER BY last_read_at DESC
LIMIT 10;

-- 之前读过
SELECT * FROM books
WHERE status = 'FINISHED'
ORDER BY last_read_at DESC
LIMIT 10;

-- 今年读过
SELECT * FROM books
WHERE status = 'FINISHED'
  AND finished_at >= :yearStart
  AND finished_at < :yearEnd
ORDER BY finished_at DESC;

-- 书库：图书
SELECT * FROM books
WHERE format = 'EPUB'
ORDER BY last_read_at DESC, created_at DESC;

-- 书库：欲读清单
SELECT * FROM books
WHERE status = 'WANT_TO_READ'
ORDER BY created_at DESC;

-- 搜索
SELECT * FROM books
WHERE title LIKE '%' || :q || '%'
   OR authors LIKE '%' || :q || '%'
ORDER BY last_read_at DESC
LIMIT 50;
```

### 7.3 状态流转

| 当前 | 事件 | 新状态 |
|---|---|---|
| `UNREAD` | 打开阅读 | `READING` |
| `WANT_TO_READ` | 打开阅读 | `READING` |
| `READING` | 进度 ≥ 99% / 手动标记 | `FINISHED` |
| `FINISHED` | 重新打开并回翻 | `READING` |
| 任意 | 加入欲读清单 | `WANT_TO_READ` |
| 任意 | 从书库移除 | 删除 |

### 7.4 阅读目标与打卡

```kotlin
enum class GoalPeriod { DAILY, WEEKLY, MONTHLY }

data class GoalPeriodRange(val start: Long, val end: Long)

fun periodRange(period: GoalPeriod, now: Long, zone: ZoneId): GoalPeriodRange {
    val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val startDate = when (period) {
        GoalPeriod.DAILY -> date
        GoalPeriod.WEEKLY -> date.with(DayOfWeek.MONDAY)
        GoalPeriod.MONTHLY -> date.withDayOfMonth(1)
    }
    val endDate = when (period) {
        GoalPeriod.DAILY -> startDate.plusDays(1)
        GoalPeriod.WEEKLY -> startDate.plusWeeks(1)
        GoalPeriod.MONTHLY -> startDate.plusMonths(1)
    }
    return GoalPeriodRange(
        start = startDate.atStartOfDay(zone).toInstant().toEpochMilli(),
        end = endDate.atStartOfDay(zone).toInstant().toEpochMilli(),
    )
}
```

```kotlin
suspend fun onReadingSessionFinished(bookId: String, seconds: Long) {
    val now = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    readingSessionDao.insert(
        ReadingSessionEntity(
            bookId = bookId,
            startedAt = now - seconds * 1000,
            durationSeconds = seconds,
            localDay = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toString(),
        )
    )

    val goal = readingGoalDao.get() ?: return
    val range = periodRange(goal.period, now, zone)
    val minutes = readingSessionDao.sumMinutes(range.start, range.end) / 60
    if (minutes >= goal.targetMinutes) {
        goalCheckInDao.insertIfAbsent(
            GoalCheckInEntity(
                period = goal.period,
                periodStart = range.start,
                periodEnd = range.end,
                targetMinutes = goal.targetMinutes,
                minutes = minutes,
                completedAt = now,
            )
        )
    }
}
```

- `goal_check_ins` 对 `(period, period_start)` 建唯一索引，重复触发不会重复插入；
- 打卡历史按 `period_start DESC` 取最近 N 条；
- 连续次数在查询后按周期向前数，不要存冗余字段，避免修改目标后不一致。

---

## 8. 实施顺序

| 阶段 | 内容 | 验收 |
|---|---|---|
| P0 | 工程、主题、三 Tab、空壳页面 | 三个 Tab 可切换，结构正确 |
| P1 | 书库 Scaffold、藏书选项、导入、卡片、排序、添加分类 | 导入 EPUB/PDF，切换藏书正确，排序生效 |
| P2 | 阅读器、进度保存、目录、阅读设置 | 退出重进恢复位置，EPUB 可读 |
| P3 | 主页四块、阅读目标与打卡、今年读过 | 阅读时长累计，目标达标生成打卡 |
| P4 | 搜索框、搜索记录、结果、空状态 | 有记录显示，无记录不显示；结果可打开 |
| P5 | PDF 阅读、有声书占位、打磨 | PDF 可读；视觉/无障碍验收 |

先做书库和阅读器，再做主页目标，因为主页依赖阅读会话数据。

---

## 9. 验收清单

- [ ] 主页严格只有四个区块，顺序为：继续阅读、之前读过、阅读目标、今年读过。
- [ ] 书库严格只有：欲读清单、已读完、图书、有声书、PDF、自定义分类、添加分类；没有“全部/在读/试读”。
- [ ] 书库使用 `Scaffold`，内容区卡片可排序。
- [ ] 搜索只有搜索框；有记录才显示“搜索记录”，没有就不渲染。
- [ ] 阅读目标可设置每天/每周/每月和分钟数；达标自动生成打卡记录。
- [ ] 导入 EPUB 后出现在“图书”；导入 PDF 后出现在“PDF”。
- [ ] 点击书能打开阅读器；退出重进恢复位置。
- [ ] 没有多余的主页推荐、账户头像、浏览快捷入口。

---

## 10. 参考

- Apple Support：[在 iPhone 上的“图书” App 中阅读图书](https://support.apple.com/guide/iphone/iphc1af7c57/ios)
- Apple Support：[在 iPhone 上的“图书” App 中整理图书](https://support.apple.com/guide/iphone/iphab219b91/ios)
- Apple Support：[在 iPhone 上的“图书” App 中设定阅读目标](https://support.apple.com/guide/iphone/iph6013e96f4/ios)
- Readium Kotlin Toolkit 3.4.0：[Release](https://github.com/readium/kotlin-toolkit/releases/tag/3.4.0)
- Readium：[Compose Web Navigators](https://github.com/readium/kotlin-toolkit/blob/3.4.0/docs/guides/navigator/web-navigators.md)
- Readium：[Navigator 总览](https://github.com/readium/kotlin-toolkit/blob/3.4.0/docs/guides/navigator/navigator.md)

> 代码是结构示意，Readium API 以 3.4.0 为准；实际编译时按版本微调构造参数。
