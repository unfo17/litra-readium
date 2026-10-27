package io.github.unfo17.litra.ui

import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.unfo17.litra.LibraryViewModel
import io.github.unfo17.litra.data.*
import java.time.*

@Composable
fun HomeScreen(vm: LibraryViewModel, books: List<Book>?, read: (Book) -> Unit, detail: (Book) -> Unit, menu: (Book) -> Unit, goals: () -> Unit, library: () -> Unit) {
    val checkIns by vm.checkIns.collectAsState()
    val allGoals by vm.goals.collectAsState()
    val now = System.currentTimeMillis()
    val goal = allGoals.firstOrNull { it.effectiveAt <= now } ?: ReadingGoal(id = 0)
    val range = GoalPeriods.activeRange(goal, now, ZoneId.systemDefault())
    val checkIn = checkIns.find { it.period == goal.period && it.start == range.start }
    val year = LocalDate.now().year
    val thisYear = books.orEmpty().filter { it.status == "FINISHED" && it.finishedAt?.let { t -> Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).year == year } == true }
    Scaffold(topBar = { PageBar("主页") }) { padding ->
        if (books == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 28.dp)) {
            item { SectionTitle("继续阅读") }
            item { val items = books.filter { it.status == "READING" }.take(10); if (items.isEmpty()) EmptyState("留一点时间给阅读", "从书库打开一本 EPUB，继续阅读会出现在这里。", "打开书库", library) else HorizontalBooks(vm, items, read, detail, menu) }
            item { SectionTitle("欲读清单") }
            item { val items = books.filter { it.status == "WANT_TO_READ" }; if (items.isEmpty()) QuietEmpty("长按图书，加入下一段阅读。") else LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) { items(items, key = { it.id }) { book -> Column(Modifier.width(180.dp).combinedClickable(onClick = { detail(book) }, onLongClick = { menu(book) })) { Cover(book, vm.repository::file); Text(book.progressLabel, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall) } } } }
            item { SectionTitle("之前读过") }
            item { val items = books.filter { it.status == "FINISHED" }.take(10); if (items.isEmpty()) QuietEmpty("读完一本，留下一个故事。") else HorizontalBooks(vm, items, read, detail, menu) }
            item { SectionTitle("阅读目标") }
            item { Card(onClick = goals, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("${GoalPeriods.label(goal.period)} ${goal.minutes} 分钟", style = MaterialTheme.typography.titleLarge); Text("本周期 ${(checkIn?.seconds ?: 0) / 60} / ${goal.minutes} 分钟"); LinearProgressIndicator(progress = { ((checkIn?.seconds ?: 0).toFloat() / (goal.minutes * 60)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()); Text(if (checkIn?.completedAt != null) "本周期已达标" else "每一页都算数", style = MaterialTheme.typography.bodySmall) } } }
            item { SectionTitle("今年读过 · ${thisYear.size} 本") }
            item { if (thisYear.isEmpty()) QuietEmpty("你的年度阅读记录，正在等待第一本。") else LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) { items(thisYear, key = { it.id }) { book -> Column(Modifier.width(120.dp).combinedClickable(onClick = { detail(book) }, onLongClick = { menu(book) })) { Cover(book, vm.repository::file); Text("已读完", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall) } } } }
        }
    }
}

@Composable private fun SectionTitle(title: String) { HorizontalDivider(Modifier.padding(top = 24.dp)); Text(title, Modifier.padding(horizontal = 20.dp, vertical = 20.dp), style = MaterialTheme.typography.headlineSmall) }
@Composable private fun QuietEmpty(text: String) { Text(text, Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable
fun HorizontalBooks(vm: LibraryViewModel, books: List<Book>, read: (Book) -> Unit, detail: (Book) -> Unit, menu: (Book) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(books, key = { it.id }) { book ->
            Card(Modifier.width(350.dp).combinedClickable(onClick = { read(book) }, onLongClick = { menu(book) })) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.width(90.dp).clickable { detail(book) }) { Cover(book, vm.repository::file) }
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
    if (book.needsFinish) TextButton(onClick = finish, modifier = Modifier.testTag("finish-prompt-${book.id}")) { Text("点击标记读完") }
    else if (book.status == "FINISHED") Text("已读完", style = MaterialTheme.typography.bodySmall)
    else { Text("${book.kind} · ${book.progressLabel}", style = MaterialTheme.typography.labelMedium); LinearProgressIndicator(progress = { book.progress.toFloat() }, modifier = Modifier.fillMaxWidth()) }
}

@Composable
fun SearchScreen(vm: LibraryViewModel, books: List<Book>?, read: (Book) -> Unit, detail: (Book) -> Unit, menu: (Book) -> Unit, settings: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val history by vm.history.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val results = books?.filter { query.isNotBlank() && (it.title.contains(query.trim(), ignoreCase = true) || it.authors.contains(query.trim(), ignoreCase = true)) }
    Scaffold(topBar = { PageBar("搜索") }, floatingActionButton = { FloatingActionButton(onClick = settings) { Icon(Icons.Default.Settings, "设置") } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OutlinedTextField(query, { query = it.take(120) }, label = { Text("书名或作者") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { vm.search(query); keyboard?.hide() }), leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "清空搜索") } }) }
            if (query.isBlank() && history.isNotEmpty()) {
                item { Text("搜索记录", Modifier.testTag("search-history"), style = MaterialTheme.typography.titleLarge) }
                items(history, key = { it.query }) { item -> TextButton(onClick = { query = item.query; vm.search(query) }) { Icon(Icons.Default.History, null); Spacer(Modifier.width(12.dp)); Text(item.query) } }
            }
            if (query.isNotBlank()) {
                when {
                    results == null -> item { CircularProgressIndicator() }
                    results.isEmpty() -> item { EmptyState("没有找到图书", "试试其他书名或作者。") }
                    else -> items(results, key = { it.id }) { book ->
                        Row(Modifier.fillMaxWidth().combinedClickable(onClick = { vm.search(query); read(book) }, onLongClick = { menu(book) }).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(68.dp).clickable { vm.search(query); detail(book) }) { Cover(book, vm.repository::file) }
                            Column(Modifier.weight(1f)) { Text(book.title, style = MaterialTheme.typography.titleMedium); Text(book.authors, style = MaterialTheme.typography.bodySmall); Text(book.progressLabel, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        }
    }
}
