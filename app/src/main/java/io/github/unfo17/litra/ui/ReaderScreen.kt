@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package io.github.unfo17.litra.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.app.Application
import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.awaitCancellation
import androidx.lifecycle.repeatOnLifecycle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.readium.r2.navigator.preferences.FontFamily
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.readium.navigator.web.reflowable.*
import org.readium.navigator.web.reflowable.preferences.ReflowableWebPreferences
import org.readium.navigator.web.fixedlayout.*
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.shared.publication.*

@Composable
fun ReaderScreen(id: String, library: LibraryViewModel, back: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val vm: ReaderViewModel = viewModel(key = id, factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ReaderViewModel(context.applicationContext as Application, library.repository, id) as T
    })
    val publication by vm.publication.collectAsStateWithLifecycle()
    val reflowable by vm.reflowable.collectAsStateWithLifecycle()
    val fixed by vm.fixed.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val book by vm.book.collectAsStateWithLifecycle()
    val locator by vm.current.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val prefs by library.preferences.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var chrome by rememberSaveable(id) { mutableStateOf(true) }
    val view = LocalView.current
    val readerDark = prefs.readerTheme == "DARK"
    DisposableEffect(activity) {
        val window = activity?.window
        val oldBrightness = window?.attributes?.screenBrightness ?: -1f
        onDispose {
            window?.let { w ->
                w.attributes = w.attributes.apply { screenBrightness = oldBrightness }
                WindowCompat.getInsetsController(w, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    SideEffect {
        activity?.window?.let { w ->
            w.attributes = w.attributes.apply { screenBrightness = prefs.brightness.toFloat().coerceIn(-1f, 1f) }
            WindowCompat.getInsetsController(w, view).apply {
                isAppearanceLightStatusBars = !readerDark
                isAppearanceLightNavigationBars = !readerDark
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (chrome) show(WindowInsetsCompat.Type.systemBars()) else hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    var sheet by remember { mutableStateOf<String?>(null) }
    var more by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    val leave = { if (!leaving) { leaving = true; scope.launch { vm.saveNow(); back() } }; Unit }
    BackHandler { if (sheet != null) sheet = null else leave() }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, prefs.keepAwake) {
        val window = activity?.window
        if (prefs.keepAwake) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) library.action { vm.saveNow() }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    val controller = reflowable?.controller
    val fixedController = fixed?.controller
    fun currentPageLocation() = controller?.location?.toLocator() ?: fixedController?.location?.toLocator()
    LaunchedEffect(controller, fixedController, owner, sheet) {
        if ((controller != null || fixedController != null) && sheet == null) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var elapsed = SystemClock.elapsedRealtime()
            fun flush() {
                val seconds = (SystemClock.elapsedRealtime() - elapsed) / 1000
                if (seconds > 0) {
                    elapsed += seconds * 1000
                    val started = System.currentTimeMillis() - seconds * 1000
                    library.action { library.goalsRepository.record(id, started, seconds) }
                }
            }
            try { while (isActive) { delay(15000); flush() } } finally { flush() }
        }
    }
    LaunchedEffect(controller) { controller?.let { c -> snapshotFlow { c.location.toLocator() to !c.canMoveForward }.distinctUntilChanged().collect { (location, end) -> vm.location(location, end) } } }
    LaunchedEffect(fixedController) { fixedController?.let { c -> snapshotFlow { c.location.toLocator() to !c.canMoveForward }.distinctUntilChanged().collect { (location, end) -> vm.location(location, end) } } }
    LaunchedEffect(controller, prefs) {
        controller?.let { c ->
            val theme = when (prefs.readerTheme) { "DARK" -> ReflowableWebPreferences.DarkTheme; "LIGHT" -> ReflowableWebPreferences.LightTheme; else -> ReflowableWebPreferences.SepiaTheme }
            c.preferences = theme.copy(fontFamily = if (prefs.fontFamily == "SANS") FontFamily.SANS_SERIF else FontFamily.SERIF, fontSize = prefs.fontSize, lineHeight = prefs.lineHeight, minMargins = prefs.margins, textAlign = if (prefs.justify) TextAlign.JUSTIFY else TextAlign.START, scroll = prefs.scroll)
        }
    }
    val paper = when (prefs.readerTheme) { "DARK" -> Color(0xFF1C1B19); "LIGHT" -> Color.White; else -> Color(0xFFFAF4E8) }
    val previousPage = {
        val before = currentPageLocation()
        if (before != null) scope.launch {
            vm.previous()
            val moved = withTimeoutOrNull(1_500) { snapshotFlow { currentPageLocation() }.first { it != before } }
            if (moved != null) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val nextPage = {
        val before = currentPageLocation()
        if (before != null) scope.launch {
            vm.next()
            val moved = withTimeoutOrNull(1_500) { snapshotFlow { currentPageLocation() }.first { it != before } }
            if (moved != null) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    Box(Modifier.fillMaxSize().background(paper).testTag("reader-page").semantics {
        customActions = listOf(CustomAccessibilityAction(if (chrome) "隐藏阅读工具栏" else "显示阅读工具栏") { chrome = !chrome; true })
    }) {
        when {
            error != null -> EmptyState("无法打开图书", error.orEmpty(), "重试", vm::load)
            publication == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            reflowable != null -> key(reflowable) { ReflowableWebRendition(reflowable!!, Modifier.fillMaxSize(), windowInsets = WindowInsets(top = 28.dp, bottom = 28.dp)) }
            fixed != null -> key(fixed) { FixedWebRendition(fixed!!, Modifier.fillMaxSize()) }
        }
        if (publication != null) ReaderTapZones(
            tapPagingEnabled = prefs.tapPaging && (controller != null || fixedController != null),
            onPreviousPage = previousPage,
            onNextPage = nextPage,
            onToggleChrome = { chrome = !chrome },
            chromeVisible = chrome,
        )
        AnimatedVisibility(chrome, modifier = Modifier.align(Alignment.TopCenter), enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }) {
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(paper, paper.copy(alpha = .96f), paper.copy(alpha = .85f)))).testTag("reader-top")) {
        PageBar(book?.title ?: "阅读器", leave) {
            IconButton(onClick = vm::bookmark, enabled = locator != null) { Icon(Icons.Default.BookmarkAdd, "添加书签") }
            Box { IconButton(onClick = { more = true }) { Icon(Icons.Default.MoreVert, "更多") }; DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                DropdownMenuItem(text = { Text("标记已读完") }, onClick = { book?.let(library::finish); more = false })
                DropdownMenuItem(text = { Text("排版设置") }, onClick = { sheet = "排版"; more = false })
            } }
        }
            }
        }
        AnimatedVisibility(chrome, modifier = Modifier.align(Alignment.BottomCenter), enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .96f), tonalElevation = 2.dp, modifier = Modifier.testTag("reader-bottom")) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("${locator?.title ?: publication?.tableOfContents?.let { flattenToc(it) }?.find { it.url().removeFragment() == locator?.href?.removeFragment() }?.title ?: "阅读进度"} · ${BookRules.percent(progress)}%", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                var slider by remember { mutableFloatStateOf(0f) }; var dragging by remember { mutableStateOf(false) }
                Slider(modifier = Modifier.testTag("reader-progress"), value = if (dragging) slider else (locator?.locations?.totalProgression ?: 0.0).toFloat(), onValueChange = { dragging = true; slider = it }, onValueChangeFinished = { scope.launch { vm.seek(slider); dragging = false } }, enabled = publication != null)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { scope.launch { vm.previous() } }, enabled = publication != null) { Icon(Icons.AutoMirrored.Filled.NavigateBefore, "上一页") }
                    listOf("目录" to Icons.AutoMirrored.Filled.List, "搜索" to Icons.Default.Search, "书签" to Icons.Default.Bookmarks, "排版" to Icons.Default.TextFields).forEach { (label, icon) -> IconButton(onClick = { sheet = label }, enabled = publication != null) { Icon(icon, label) } }
                    IconButton(onClick = { scope.launch { vm.next() } }, enabled = publication != null) { Icon(Icons.AutoMirrored.Filled.NavigateNext, "下一页") }
                }
            }
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
                    val results by vm.results.collectAsStateWithLifecycle(); val searching by vm.searching.collectAsStateWithLifecycle(); val searchError by vm.searchError.collectAsStateWithLifecycle()
                    OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("搜索书中内容") }, singleLine = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { vm.search(query) }) { Icon(Icons.Default.Search, "开始书内搜索") } })
                    if (searching) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (searchError != null) Text(searchError.orEmpty(), Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.error)
                    if (!searching && query.isNotBlank() && results.isEmpty()) Text("没有匹配内容", Modifier.padding(vertical = 16.dp))
                    LazyColumn { items(results) { result -> TextButton(onClick = { scope.launch { vm.go(result); sheet = null } }) { Text("${result.text.before.orEmpty()}${result.text.highlight.orEmpty()}${result.text.after.orEmpty()}", maxLines = 4) } } }
                }
                "排版" -> Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) { ReaderPreferences(library, fixed != null) }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
internal fun ReaderTapZones(
    tapPagingEnabled: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleChrome: () -> Unit,
    chromeVisible: Boolean,
) {
    Box(Modifier.fillMaxSize()) {
        if (tapPagingEnabled) {
            Box(Modifier.align(Alignment.CenterStart).fillMaxWidth(.318f).fillMaxHeight()
                .testTag("reader-left-tap-zone")
                .semantics { contentDescription = "点击上一页" }
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, onClick = onPreviousPage))
            Box(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.318f).fillMaxHeight()
                .testTag("reader-right-tap-zone")
                .semantics { contentDescription = "点击下一页" }
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, onClick = onNextPage))
        }
        Box(Modifier.align(Alignment.Center).fillMaxWidth(.36f).fillMaxHeight(.30f)
            .background(Color.Transparent).testTag("reader-center-tap-zone")
            .clickable(onClickLabel = if (chromeVisible) "隐藏阅读工具栏" else "显示阅读工具栏", indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, onClick = onToggleChrome))
    }
}

@Composable
fun ReaderPreferences(vm: LibraryViewModel, fixed: Boolean = false) {
    val p by vm.preferences.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth()) {
        Text("屏幕亮度", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) { Text("跟随系统", Modifier.weight(1f)); Switch(p.brightness < 0, { vm.action { vm.preferencesRepository.number("brightness", if (it) -1.0 else .5) } }) }
        if (p.brightness >= 0) Slider(p.brightness.toFloat(), { vm.action { vm.preferencesRepository.number("brightness", it.toDouble()) } }, valueRange = .05f..1f)
        if (fixed) Text("固定版式保留原书排版。")
        else {
            Text("字体", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("SERIF" to "系统衬线", "SANS" to "系统无衬线").forEach { (id, label) -> FilterChip(selected = p.fontFamily == id, onClick = { vm.textPreference("fontFamily", id) }, label = { Text(label) }) } }
            Text("字号"); Slider(p.fontSize.toFloat(), { vm.action { vm.preferencesRepository.number("font", it.toDouble()) } }, valueRange = 0.6f..2.5f)
            Text("行距"); Slider(p.lineHeight.toFloat(), { vm.action { vm.preferencesRepository.number("line", it.toDouble()) } }, valueRange = 1f..2.5f)
            Text("页边距"); Slider(p.margins.toFloat(), { vm.action { vm.preferencesRepository.number("margins", it.toDouble()) } }, valueRange = 0f..3f)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("LIGHT" to "白色", "SEPIA" to "纸色", "DARK" to "夜间").forEach { (id, label) -> FilterChip(selected = p.readerTheme == id, onClick = { vm.textPreference("readerTheme", id) }, label = { Text(label) }) } }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("两端对齐", Modifier.weight(1f)); Switch(p.justify, { vm.flagPreference("justify", it) }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("连续滚动", Modifier.weight(1f)); Switch(p.scroll, { vm.flagPreference("scroll", it) }) }
        }
    }
}

private fun flattenToc(links: List<Link>): List<Link> = links.flatMap { listOf(it) + flattenToc(it.children) }
