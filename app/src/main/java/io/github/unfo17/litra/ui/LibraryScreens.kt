package io.github.unfo17.litra.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.unfo17.litra.LibraryViewModel
import io.github.unfo17.litra.data.*
import java.time.Instant
import java.time.ZoneId

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LibraryScreen(vm: LibraryViewModel, books: List<Book>?, list: Boolean, onView: (Boolean) -> Unit, onImport: () -> Unit, collections: () -> Unit, read: (Book) -> Unit, menu: (Book) -> Unit) {
    val filter by vm.filter.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val memberships by vm.memberships.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val importing by vm.importing.collectAsStateWithLifecycle()
    var options by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var batchMenu by remember { mutableStateOf(false) }
    var categoryPicker by remember { mutableStateOf(false) }
    var confirmBatchDelete by remember { mutableStateOf(false) }
    val filtered = books?.filter { book -> when (filter) { "图书" -> true; "欲读清单" -> book.status == "WANT_TO_READ"; "PDF", "有声书" -> false; else -> memberships.any { it.bookId == book.id && it.categoryId == filter } } }?.let { items -> when (prefs.sort) { "TITLE" -> items.sortedBy { it.title }; "IMPORTED" -> items.sortedByDescending { it.createdAt }; "PROGRESS" -> items.sortedByDescending { it.progress }; else -> items } }
    val visibleIds = filtered.orEmpty().mapTo(linkedSetOf()) { it.id }
    val allVisibleSelected = visibleIds.isNotEmpty() && selectedIds.containsAll(visibleIds)
    fun exitSelection() { selecting = false; selectedIds = emptySet() }
    fun toggleBook(book: Book) { selectedIds = if (book.id in selectedIds) selectedIds - book.id else selectedIds + book.id }
    BackHandler(enabled = selecting) { exitSelection() }
    Scaffold(topBar = {
        if (!selecting) PageBar("书库", actions = { TextButton(onClick = { selecting = true; selectedIds = emptySet() }, modifier = Modifier.testTag("library-enter-selection")) { Text("选择") } })
        else TopAppBar(
            navigationIcon = { IconButton(onClick = ::exitSelection, modifier = Modifier.testTag("library-exit-selection")) { Icon(Icons.Default.Close, "退出选择") } },
            title = { Text("已选 ${selectedIds.size} 本", modifier = Modifier.testTag("library-selection-count")) },
            actions = {
                TextButton(onClick = { selectedIds = if (allVisibleSelected) selectedIds - visibleIds else selectedIds + visibleIds }, enabled = visibleIds.isNotEmpty(), modifier = Modifier.testTag("library-select-all")) { Text(if (allVisibleSelected) "取消全选" else "全选") }
                Box {
                    TextButton(onClick = { batchMenu = true }, enabled = selectedIds.isNotEmpty(), modifier = Modifier.testTag("library-batch-actions")) { Text("操作"); Icon(Icons.Default.ExpandMore, null) }
                    DropdownMenu(expanded = batchMenu, onDismissRequest = { batchMenu = false }) {
                        DropdownMenuItem(text = { Text("加入自定义分类") }, onClick = { batchMenu = false; categoryPicker = true }, enabled = selectedIds.isNotEmpty())
                        DropdownMenuItem(text = { Text("加入欲读清单") }, onClick = { batchMenu = false; vm.batchWant(selectedIds); exitSelection() }, enabled = selectedIds.isNotEmpty(), modifier = Modifier.testTag("batch-add-want"))
                        DropdownMenuItem(text = { Text("移除图书") }, onClick = { batchMenu = false; confirmBatchDelete = true }, enabled = selectedIds.isNotEmpty(), modifier = Modifier.testTag("batch-remove-books"))
                    }
                }
            }
        )
    }, floatingActionButton = {
        if (!selecting) Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.End) {
            SmallFloatingActionButton(onClick = { options = true }) { Icon(Icons.Default.Tune, "筛选、排序与视图") }
            SmallFloatingActionButton(onClick = collections) { Icon(Icons.Default.FolderOpen, "藏书选项") }
            ExtendedFloatingActionButton(modifier = Modifier.semantics { contentDescription = if (importing) "正在导入 EPUB" else "导入 EPUB" }, onClick = { if (!importing) onImport() }, icon = { Icon(Icons.Default.Add, null) }, text = { Text(if (importing) "正在导入…" else "导入") })
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!selecting) TextButton(onClick = collections, modifier = Modifier.padding(horizontal = 12.dp)) { Text(categories.find { it.id == filter }?.name ?: filter); Icon(Icons.Default.ExpandMore, null) }
            if (importing) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                filtered == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                filtered.isEmpty() -> EmptyState("这里还没有图书", if (filter in listOf("PDF", "有声书")) "V1 暂不支持此格式，请导入 EPUB。" else "导入本地 EPUB，让下一段阅读从这里开始。", if (filter == "图书") "导入 EPUB" else null, onImport)
                list -> LazyColumn(Modifier.testTag("library-list"), contentPadding = PaddingValues(bottom = if (selecting) 24.dp else 230.dp)) {
                    items(filtered, key = { it.id }) { book ->
                        val isSelected = book.id in selectedIds
                        Row(Modifier.testTag("library-book-${book.id}").fillMaxWidth().semantics { selected = isSelected }.background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface).combinedClickable(onClickLabel = if (selecting) "选择《${book.title}》" else "阅读《${book.title}》", onLongClickLabel = "更多操作", onClick = { if (selecting) toggleBook(book) else read(book) }, onLongClick = { if (!selecting) menu(book) }).padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Cover(book, vm.repository::file, Modifier.width(76.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(book.title, style = MaterialTheme.typography.titleMedium); Text(book.authors); Text(book.progressLabel, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            if (selecting) SelectionMark(isSelected) else IconButton(onClick = { menu(book) }) { Icon(Icons.Default.MoreVert, "${book.title}的操作") }
                        }
                    }
                }
                else -> LazyVerticalGrid(GridCells.Fixed(2), modifier = Modifier.testTag("library-grid"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = if (selecting) 24.dp else 230.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    items(filtered, key = { it.id }) { book ->
                        val isSelected = book.id in selectedIds
                        Column(Modifier.testTag("library-book-${book.id}").semantics { selected = isSelected }.combinedClickable(onClickLabel = if (selecting) "选择《${book.title}》" else "阅读《${book.title}》", onLongClickLabel = "更多操作", onClick = { if (selecting) toggleBook(book) else read(book) }, onLongClick = { if (!selecting) menu(book) })) {
                            Box(Modifier.fillMaxWidth().then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium).padding(3.dp) else Modifier)) {
                                Cover(book, vm.repository::file, Modifier.fillMaxWidth())
                                if (selecting) SelectionMark(isSelected, Modifier.align(Alignment.TopEnd).padding(8.dp))
                            }
                            Text(book.progressLabel, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    if (options) AlertDialog(onDismissRequest = { options = false }, title = { Text("排序与视图") }, text = {
        Column {
            TextButton(onClick = { options = false; collections() }) { Text("筛选藏书") }
            TextButton(onClick = { onView(!list); options = false }) { Text(if (list) "切换为双列封面" else "切换为列表") }
            listOf("RECENT" to "最近阅读", "IMPORTED" to "导入时间", "TITLE" to "书名", "PROGRESS" to "阅读进度").forEach { (key, label) ->
                Row(Modifier.fillMaxWidth().clickable { vm.textPreference("sort", key); options = false }, verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = prefs.sort == key, onClick = { vm.textPreference("sort", key); options = false }); Text(label) }
            }
        }
    }, confirmButton = { TextButton(onClick = { options = false }) { Text("关闭") } })
    if (categoryPicker) AlertDialog(onDismissRequest = { categoryPicker = false }, title = { Text("加入自定义分类") }, text = {
        if (categories.isEmpty()) Text("请先在藏书选项中添加自定义分类。") else Column { categories.forEach { category -> TextButton(onClick = { vm.batchAddCategory(selectedIds, category); categoryPicker = false; exitSelection() }, modifier = Modifier.fillMaxWidth()) { Text(category.name) } } }
    }, confirmButton = { TextButton(onClick = { categoryPicker = false }) { Text("取消") } })
    if (confirmBatchDelete) AlertDialog(onDismissRequest = { confirmBatchDelete = false }, title = { Text("移除 ${selectedIds.size} 本图书？") }, text = { Text("此操作会删除 Litra 内的文件和阅读记录，原始文件不受影响。") }, confirmButton = { TextButton(onClick = { vm.batchDelete(selectedIds); confirmBatchDelete = false; exitSelection() }, modifier = Modifier.testTag("confirm-batch-remove")) { Text("移除") } }, dismissButton = { TextButton(onClick = { confirmBatchDelete = false }) { Text("取消") } })
}

@Composable
private fun SelectionMark(checked: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.size(28.dp), shape = MaterialTheme.shapes.small, color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = .94f), border = BorderStroke(2.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) {
        Box(contentAlignment = Alignment.Center) { if (checked) Icon(Icons.Default.Check, "已选", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(19.dp)) }
    }
}

@Composable
fun CollectionsScreen(vm: LibraryViewModel, back: () -> Unit, finished: () -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    var marking by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var add by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(topBar = { PageBar("", back) }, floatingActionButton = {
        ExtendedFloatingActionButton(modifier = Modifier.semantics { contentDescription = if (selected.isNotEmpty()) "删除分类" else "标记分类" }, onClick = { if (!marking) marking = true else if (selected.isNotEmpty()) confirmDelete = true else marking = false }, text = { Text(if (selected.isNotEmpty()) "删除分类" else "标记") }, icon = { Icon(if (selected.isNotEmpty()) Icons.Default.Delete else Icons.Default.Edit, null) })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 120.dp)) {
            items(listOf("欲读清单", "已读完", "图书", "有声书", "PDF")) { label ->
                ListItem(headlineContent = { Text(label) }, modifier = Modifier.semantics { this.selected = filter == label }.fillMaxWidth().background(if (filter == label) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface).clickable(enabled = !marking) { if (label == "已读完") finished() else { vm.filter.value = label; back() } }, colors = ListItemDefaults.colors(containerColor = if (filter == label) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface))
            }
            items(categories, key = { it.id }) { category ->
                val chosen = if (marking) category.id in selected else filter == category.id
                ListItem(headlineContent = { Text(category.name) }, leadingContent = { Checkbox(checked = category.id in selected, onCheckedChange = { marking = true; selected = if (it) selected + category.id else selected - category.id }) }, colors = ListItemDefaults.colors(containerColor = if (chosen) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface), modifier = Modifier.semantics { this.selected = chosen }.clickable {
                    if (marking) selected = if (category.id in selected) selected - category.id else selected + category.id else { vm.filter.value = category.id; back() }
                })
            }
            item { TextButton(onClick = { add = true }, modifier = Modifier.padding(12.dp)) { Icon(Icons.Default.Add, null); Text("添加分类") } }
        }
    }
    if (add) AlertDialog(onDismissRequest = { add = false }, title = { Text("添加分类") }, text = { OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, label = { Text("分类名称") }, singleLine = true) }, confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { vm.category(name); name = ""; add = false }) { Text("添加") } }, dismissButton = { TextButton(onClick = { add = false }) { Text("取消") } })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("删除 ${selected.size} 个分类？") }, text = { Text("图书文件和阅读记录会保留。") }, confirmButton = { TextButton(onClick = { vm.deleteCategories(selected.toList()); selected = emptySet(); marking = false; confirmDelete = false }) { Text("删除") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } })
}

@Composable
fun FinishedScreen(vm: LibraryViewModel, books: List<Book>?, back: () -> Unit, detail: (Book) -> Unit) {
    val finished = books?.filter { it.status == "FINISHED" }?.sortedByDescending { it.finishedAt }
    Scaffold(topBar = { PageBar("已读完", back) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp)) {
            if (finished == null) item { CircularProgressIndicator() }
            else if (finished.isEmpty()) item { EmptyState("把读完的故事留在这里", "阅读后手动标记完成，就会加入时间线。") }
            else finished.groupBy { Instant.ofEpochMilli(it.finishedAt ?: it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().toString() }.forEach { (date, books) ->
                item { Text(date, Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.titleMedium) }
                items(books, key = { it.id }) { book -> Row(Modifier.fillMaxWidth().clickable { detail(book) }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) { Cover(book, vm.repository::file, Modifier.width(84.dp)); Column { Text(book.title, style = MaterialTheme.typography.titleLarge); Text(book.authors); Text("已读完", Modifier.padding(top = 16.dp)) } } }
            }
        }
    }
}
