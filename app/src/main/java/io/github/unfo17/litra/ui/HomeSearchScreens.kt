package io.github.unfo17.litra.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.*
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.unfo17.litra.LibraryViewModel
import io.github.unfo17.litra.data.*
import java.time.*

@Composable
fun HomeScreen(vm: LibraryViewModel, books: List<Book>?, read: (Book) -> Unit, detail: (Book) -> Unit, menu: (Book) -> Unit, goals: () -> Unit, library: () -> Unit) {
    val checkIns by vm.checkIns.collectAsStateWithLifecycle()
    val allGoals by vm.goals.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()
    val goal = allGoals.firstOrNull { it.effectiveAt <= now } ?: ReadingGoal(id = 0)
    val range = GoalPeriods.activeRange(goal, now, ZoneId.systemDefault())
    val checkIn = checkIns.find { it.period == goal.period && it.start == range.start }
    val year = LocalDate.now().year
    val thisYear = books.orEmpty().filter { it.status == "FINISHED" && it.finishedAt?.let { t -> Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).year == year } == true }
    Scaffold(topBar = { PageBar("主页") }) { padding ->
        if (books == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 28.dp)) {
            val reading = books.filter { it.status == "READING" }.take(10)
            val wanted = books.filter { it.status == "WANT_TO_READ" }
            val previous = books.filter { it.status == "FINISHED" }.take(10)
            item(key = "reading") { HomeModule("继续阅读", reading.isNotEmpty()) { HorizontalBooks(vm, reading, read, detail, menu) } }
            item(key = "wanted") { HomeModule("欲读清单", wanted.isNotEmpty()) {
                LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(wanted, key = { it.id }) { book -> Column(Modifier.width(156.dp).combinedClickable(onClickLabel = "查看《${book.title}》详情", onLongClickLabel = "更多操作", onClick = { detail(book) }, onLongClick = { menu(book) })) { Cover(book, vm.repository::file); Text(book.progressLabel, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.labelMedium) } }
                }
            } }
            item(key = "previous") { HomeModule("之前读过", previous.isNotEmpty()) { HorizontalBooks(vm, previous, read, detail, menu) } }
            item { SectionTitle("阅读目标") }
            item { Card(onClick = goals, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("${GoalPeriods.label(goal.period)} ${goal.minutes} 分钟", style = MaterialTheme.typography.titleLarge); Text("本周期 ${(checkIn?.seconds ?: 0) / 60} / ${goal.minutes} 分钟"); LinearProgressIndicator(progress = { ((checkIn?.seconds ?: 0).toFloat() / (goal.minutes * 60)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()); Text(if (checkIn?.completedAt != null) "本周期已达标" else "每一页都算数", style = MaterialTheme.typography.bodySmall) } } }
            item { SectionTitle("今年读过 · ${thisYear.size} 本") }
            item { if (thisYear.isEmpty()) Card(Modifier.padding(horizontal = 24.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) { EmptyState("给今年留下一本好书", "读完后标记完成，故事会在这里慢慢积累。", "打开书库", library) } else LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) { items(thisYear, key = { it.id }) { book -> Column(Modifier.width(120.dp).combinedClickable(onClickLabel = "查看《${book.title}》详情", onLongClickLabel = "更多操作", onClick = { detail(book) }, onLongClick = { menu(book) })) { Cover(book, vm.repository::file); Text("已读完", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall) } } } }
        }
    }
}

@Composable
fun HomeModule(title: String, visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Column { SectionTitle(title); content() }
    }
}
@Composable private fun SectionTitle(title: String) { Text(title, Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
@Composable private fun QuietEmpty(text: String) { Text(text, Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable
fun HorizontalBooks(vm: LibraryViewModel, books: List<Book>, read: (Book) -> Unit, detail: (Book) -> Unit, menu: (Book) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(books, key = { it.id }) { book ->
            Card(Modifier.widthIn(max = 350.dp).fillParentMaxWidth(.9f).combinedClickable(onClickLabel = "阅读《${book.title}》", onLongClickLabel = "更多操作", onClick = { read(book) }, onLongClick = { menu(book) })) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.width(90.dp).clickable(onClickLabel = "查看《${book.title}》详情") { detail(book) }) { Cover(book, vm.repository::file) }
                    Column(Modifier.weight(1f).heightIn(min = 136.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(book.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (book.authors.isNotBlank()) Text(book.authors, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        Spacer(Modifier.height(8.dp))
                        CompletionControl(book, { vm.finish(book) })
                    }
                }
            }
        }
    }
}

@Composable
fun CompletionControl(book: Book, finish: () -> Unit) {
    if (book.needsFinish) TextButton(onClick = finish, modifier = Modifier.testTag("finish-prompt-${book.id}").semantics { contentDescription = "标记《${book.title}》为已读完" }) { Text("点击标记读完") }
    else if (book.status == "FINISHED") Text("已读完", style = MaterialTheme.typography.bodySmall)
    else { Text("${book.kind} · ${book.progressLabel}", style = MaterialTheme.typography.labelMedium); LinearProgressIndicator(progress = { book.progress.toFloat() }, modifier = Modifier.fillMaxWidth()) }
}

@Composable
fun SearchScreen(vm: LibraryViewModel, books: List<Book>?, read: (Book) -> Unit, detail: (Book) -> Unit, menu: (Book) -> Unit, settings: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val history by vm.history.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val results = books?.filter { query.isNotBlank() && (it.title.contains(query.trim(), ignoreCase = true) || it.authors.contains(query.trim(), ignoreCase = true)) }
    Scaffold(topBar = { PageBar("搜索") }, floatingActionButton = { FloatingActionButton(onClick = settings) { Icon(Icons.Default.Settings, "设置") } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OutlinedTextField(query, { query = it.take(120) }, label = { Text("书名或作者") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { vm.search(query); keyboard?.hide() }), leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "清空搜索") } }) }
            if (query.isBlank() && history.isNotEmpty()) {
                item { Text("搜索记录", Modifier.testTag("search-history"), style = MaterialTheme.typography.titleLarge) }
                items(history, key = { it.query }) { item -> TextButton(onClick = { query = item.query; vm.search(query) }) { Icon(Icons.Default.History, null); Spacer(Modifier.width(12.dp)); Text(item.query) } }
            }
            if (query.isNotBlank()) {
                when {
                    results == null -> item { CircularProgressIndicator() }
                    results.isEmpty() -> item { EmptyState("没有找到图书", "试试其他书名或作者。") }
                    else -> items(results, key = { it.id }) { book ->
                        Row(Modifier.fillMaxWidth().combinedClickable(onClickLabel = "阅读《${book.title}》", onLongClickLabel = "更多操作", onClick = { vm.search(query); read(book) }, onLongClick = { menu(book) }).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(68.dp).clickable(onClickLabel = "查看《${book.title}》详情") { vm.search(query); detail(book) }) { Cover(book, vm.repository::file) }
                            Column(Modifier.weight(1f)) { Text(book.title, style = MaterialTheme.typography.titleMedium); Text(book.authors, style = MaterialTheme.typography.bodySmall); Text(book.progressLabel, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        }
    }
}
