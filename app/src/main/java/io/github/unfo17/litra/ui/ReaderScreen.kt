@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package io.github.unfo17.litra.ui

import android.app.Application
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.unfo17.litra.*
import io.github.unfo17.litra.data.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.navigator.web.reflowable.*
import org.readium.navigator.web.reflowable.preferences.ReflowableWebPreferences
import org.readium.navigator.web.fixedlayout.*
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.shared.publication.*

@Composable
fun ReaderScreen(id: String, library: LibraryViewModel, back: () -> Unit) {
    val context = LocalContext.current
    val vm: ReaderViewModel = viewModel(key = id, factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ReaderViewModel(context.applicationContext as Application, library.repository, id) as T
    })
    val publication by vm.publication.collectAsState()
    val reflowable by vm.reflowable.collectAsState()
    val fixed by vm.fixed.collectAsState()
    val error by vm.error.collectAsState()
    val book by vm.book.collectAsState()
    val locator by vm.current.collectAsState()
    val progress by vm.progress.collectAsState()
    val prefs by library.preferences.collectAsState()
    val bookmarks by vm.bookmarks.collectAsState()
    val scope = rememberCoroutineScope()
    var sheet by remember { mutableStateOf<String?>(null) }
    var more by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    val leave = { if (!leaving) { leaving = true; scope.launch { vm.saveNow(); back() } }; Unit }
    BackHandler { if (sheet != null) sheet = null else leave() }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, prefs.keepAwake) {
        val window = (context as? MainActivity)?.window
        if (prefs.keepAwake) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) library.action { vm.saveNow() }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    val controller = reflowable?.controller
    val fixedController = fixed?.controller
    LaunchedEffect(controller) { controller?.let { c -> snapshotFlow { c.location.toLocator() to !c.canMoveForward }.distinctUntilChanged().collect { (location, end) -> vm.location(location, end) } } }
    LaunchedEffect(fixedController) { fixedController?.let { c -> snapshotFlow { c.location.toLocator() to !c.canMoveForward }.distinctUntilChanged().collect { (location, end) -> vm.location(location, end) } } }
    LaunchedEffect(controller, prefs) {
        controller?.let { c ->
            val theme = when (prefs.readerTheme) { "DARK" -> ReflowableWebPreferences.DarkTheme; "LIGHT" -> ReflowableWebPreferences.LightTheme; else -> ReflowableWebPreferences.SepiaTheme }
            c.preferences = theme.copy(fontSize = prefs.fontSize, lineHeight = prefs.lineHeight, minMargins = prefs.margins, textAlign = if (prefs.justify) TextAlign.JUSTIFY else TextAlign.START, scroll = prefs.scroll)
        }
    }
    Scaffold(topBar = {
        PageBar(book?.title ?: "阅读器", leave) {
            IconButton(onClick = vm::bookmark, enabled = locator != null) { Icon(Icons.Default.BookmarkAdd, "添加书签") }
            Box { IconButton(onClick = { more = true }) { Icon(Icons.Default.MoreVert, "更多") }; DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                DropdownMenuItem(text = { Text("标记已读完") }, onClick = { book?.let(library::finish); more = false })
                DropdownMenuItem(text = { Text("排版设置") }, onClick = { sheet = "排版"; more = false })
            } }
        }
    }, bottomBar = {
        Surface(tonalElevation = 2.dp) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("${locator?.title ?: "阅读进度"} · ${BookRules.percent(progress)}%", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                var slider by remember { mutableFloatStateOf(0f) }; var dragging by remember { mutableStateOf(false) }
                Slider(modifier = Modifier.testTag("reader-progress"), value = if (dragging) slider else (locator?.locations?.totalProgression ?: 0.0).toFloat(), onValueChange = { dragging = true; slider = it }, onValueChangeFinished = { scope.launch { vm.seek(slider); dragging = false } }, enabled = publication != null)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { scope.launch { vm.previous() } }, enabled = publication != null) { Icon(Icons.AutoMirrored.Filled.NavigateBefore, "上一页") }
                    listOf("目录" to Icons.AutoMirrored.Filled.List, "搜索" to Icons.Default.Search, "书签" to Icons.Default.Bookmarks, "排版" to Icons.Default.TextFields).forEach { (label, icon) -> IconButton(onClick = { sheet = label }, enabled = publication != null) { Icon(icon, label) } }
                    IconButton(onClick = { scope.launch { vm.next() } }, enabled = publication != null) { Icon(Icons.AutoMirrored.Filled.NavigateNext, "下一页") }
                }
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                error != null -> EmptyState("无法打开图书", error.orEmpty(), "重试", vm::load)
                publication == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                reflowable != null -> key(reflowable) { ReflowableWebRendition(reflowable!!, Modifier.fillMaxSize()) }
                fixed != null -> key(fixed) { FixedWebRendition(fixed!!, Modifier.fillMaxSize()) }
            }
        }
    }
    if (sheet != null) ModalBottomSheet(onDismissRequest = { sheet = null }) {
        Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).padding(horizontal = 20.dp)) {
            Text(sheet.orEmpty(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 12.dp))
            when (sheet) {
                "目录" -> {
                    fun flatten(links: List<Link>, depth: Int = 0): List<Pair<Link, Int>> = links.flatMap { listOf(it to depth) + flatten(it.children, depth + 1) }
                    val toc = publication?.tableOfContents.orEmpty().let { if (it.isEmpty()) publication?.readingOrder.orEmpty() else it }
                    LazyColumn { items(flatten(toc)) { (link, depth) -> TextButton(onClick = { scope.launch { vm.go(link); sheet = null } }, modifier = Modifier.padding(start = (depth * 16).dp)) { Text(link.title ?: link.href.toString()) } } }
                }
                "书签" -> if (bookmarks.isEmpty()) EmptyState("还没有书签", "点击顶部书签按钮保存当前位置。") else LazyColumn { items(bookmarks, key = { it.id }) { mark -> ListItem(headlineContent = { Text(mark.label) }, modifier = Modifier.fillMaxWidth(), leadingContent = { TextButton(onClick = { scope.launch { Locator.fromJSON(JSONObject(mark.locator))?.let { vm.go(it) }; sheet = null } }) { Text("前往") } }, trailingContent = { IconButton(onClick = { vm.deleteBookmark(mark) }) { Icon(Icons.Default.Delete, "删除书签") } }) } }
                "搜索" -> {
                    var query by remember { mutableStateOf("") }
                    val results by vm.results.collectAsState(); val searching by vm.searching.collectAsState(); val searchError by vm.searchError.collectAsState()
                    OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("搜索书中内容") }, singleLine = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { vm.search(query) }) { Icon(Icons.Default.Search, "开始书内搜索") } })
                    if (searching) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (searchError != null) Text(searchError.orEmpty(), Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.error)
                    if (!searching && query.isNotBlank() && results.isEmpty()) Text("没有匹配内容", Modifier.padding(vertical = 16.dp))
                    LazyColumn { items(results) { result -> TextButton(onClick = { scope.launch { vm.go(result); sheet = null } }) { Text("${result.text.before.orEmpty()}${result.text.highlight.orEmpty()}${result.text.after.orEmpty()}", maxLines = 4) } } }
                }
                "排版" -> ReaderPreferences(library, fixed != null)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
fun ReaderPreferences(vm: LibraryViewModel, fixed: Boolean = false) {
    val p by vm.preferences.collectAsState()
    Column(Modifier.fillMaxWidth()) {
        if (fixed) Text("固定版式保留原书排版。")
        else {
            Text("字号"); Slider(p.fontSize.toFloat(), { vm.action { vm.preferencesRepository.number("font", it.toDouble()) } }, valueRange = 0.6f..2.5f)
            Text("行距"); Slider(p.lineHeight.toFloat(), { vm.action { vm.preferencesRepository.number("line", it.toDouble()) } }, valueRange = 1f..2.5f)
            Text("页边距"); Slider(p.margins.toFloat(), { vm.action { vm.preferencesRepository.number("margins", it.toDouble()) } }, valueRange = 0f..3f)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("LIGHT" to "白色", "SEPIA" to "纸色", "DARK" to "夜间").forEach { (id, label) -> FilterChip(selected = p.readerTheme == id, onClick = { vm.textPreference("readerTheme", id) }, label = { Text(label) }) } }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("两端对齐", Modifier.weight(1f)); Switch(p.justify, { vm.flagPreference("justify", it) }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("连续滚动", Modifier.weight(1f)); Switch(p.scroll, { vm.flagPreference("scroll", it) }) }
        }
    }
}
