package io.github.unfo17.litra.ui

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import coil3.compose.AsyncImage
import io.github.unfo17.litra.BuildConfig
import io.github.unfo17.litra.LibraryViewModel
import io.github.unfo17.litra.data.*

@Composable
fun DetailScreen(vm: LibraryViewModel, book: Book?, loading: Boolean, back: () -> Unit, read: (Book) -> Unit) {
    val categories by vm.categories.collectAsState()
    val members by vm.memberships.collectAsState()
    val context = LocalContext.current
    val background = MaterialTheme.colorScheme.surface
    Scaffold(contentWindowInsets = WindowInsets(0), floatingActionButton = { if (book != null) ExtendedFloatingActionButton(modifier = Modifier.semantics { contentDescription = "阅读《${book.title}》" }, onClick = { read(book) }, icon = { Icon(Icons.Default.MenuBook, null) }, text = { Text("阅读") }) }) { padding ->
        if (book == null) Column(Modifier.statusBarsPadding().padding(padding)) { PageBar("图书信息", back); if (loading) CircularProgressIndicator() else EmptyState("图书已移除", "返回书库选择其他图书。") }
        else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 140.dp)) {
                item {
                    Box(Modifier.fillMaxWidth().height(390.dp)) {
                        if (book.coverName != null) AsyncImage(vm.repository.file(book.coverName), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .16f), Color.Transparent, background))))
                        Cover(book, vm.repository::file, Modifier.align(Alignment.Center).width(160.dp).padding(top = 32.dp))
                        FilledIconButton(onClick = back, modifier = Modifier.statusBarsPadding().padding(12.dp).align(Alignment.TopStart)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(book.title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                        if (book.authors.isNotBlank()) Text(book.authors, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        Text(book.progressLabel, color = MaterialTheme.colorScheme.primary)
                        Text((listOf(book.kind) + categories.filter { category -> members.any { it.bookId == book.id && it.categoryId == category.id } }.map { it.name }).joinToString(" · "), textAlign = TextAlign.Center)
                        RectangularCard("内容简介", if (book.description.isBlank()) "这本 EPUB 未提供内容简介。" else HtmlCompat.fromHtml(book.description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString())
                        RectangularCard("出版信息", listOfNotNull(book.publisher.takeIf { it.isNotBlank() }?.let { "出版者：$it" }, book.language.takeIf { it.isNotBlank() }?.let { "语言：$it" }, "格式：${book.format}", "文件大小：${"%.2f".format(book.size / 1024.0 / 1024.0)} MB").joinToString("\n"))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("图书", "漫画").forEach { kind -> FilterChip(selected = book.kind == kind, onClick = { vm.action { vm.repository.dao.kind(book.id, kind) } }, label = { Text(kind) }) } }
                        TextButton(onClick = {
                            val text = listOf(book.title, book.authors).filter { it.isNotBlank() }.joinToString(" — ")
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "分享图书信息"))
                        }) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(8.dp)); Text("分享图书信息") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RectangularCard(title: String, text: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun SettingsScreen(vm: LibraryViewModel, back: () -> Unit, goals: () -> Unit) {
    val prefs by vm.preferences.collectAsState()
    var clear by remember { mutableStateOf(false) }
    var reader by remember { mutableStateOf(false) }
    Scaffold(topBar = { PageBar("设置", back) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item { ListItem(headlineContent = { Text("阅读目标") }, supportingContent = { Text("为每天、每周或每月留出阅读时间") }, modifier = Modifier.clickable(onClick = goals), leadingContent = { Icon(Icons.Default.Timer, null) }) }
            item { HorizontalDivider(); Text("外观", Modifier.padding(20.dp), style = MaterialTheme.typography.titleMedium) }
            item { FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("SYSTEM" to "跟随系统", "LIGHT" to "浅色", "DARK" to "深色").forEach { (id, label) -> FilterChip(selected = prefs.appearance == id, onClick = { vm.textPreference("appearance", id) }, label = { Text(label) }) } } }
            item { ListItem(headlineContent = { Text("阅读器排版") }, supportingContent = { Text("阅读背景独立于 App 外观") }, modifier = Modifier.clickable { reader = true }, leadingContent = { Icon(Icons.Default.TextFields, null) }) }
            item { ListItem(headlineContent = { Text("阅读时保持屏幕常亮") }, trailingContent = { Switch(prefs.keepAwake, { vm.flagPreference("awake", it) }) }) }
            item { ListItem(headlineContent = { Text("保存搜索记录") }, trailingContent = { Switch(prefs.rememberSearch, { vm.flagPreference("history", it) }) }) }
            item { ListItem(headlineContent = { Text("清除搜索记录") }, modifier = Modifier.clickable { clear = true }, leadingContent = { Icon(Icons.Default.History, null) }) }
            item { HorizontalDivider(); ListItem(headlineContent = { Text("关于 Litra") }, supportingContent = { Text("${BuildConfig.VERSION_NAME} preview\n本地 EPUB 阅读器 · Kotlin / Material 3 / Readium\n不收集数据，无账号与云服务。仅支持无 DRM EPUB 2/3。") }) }
            item { ListItem(headlineContent = { Text("开源许可") }, supportingContent = { Text("Readium：BSD-3-Clause；AndroidX / Coil / Hilt：Apache-2.0。完整依赖声明见项目 THIRD_PARTY_NOTICES.md。") }) }
        }
    }
    if (clear) AlertDialog(onDismissRequest = { clear = false }, title = { Text("清除搜索记录？") }, text = { Text("图书和阅读进度会保留。") }, confirmButton = { TextButton(onClick = { vm.action { vm.repository.dao.clearHistory() }; clear = false }) { Text("清除") } }, dismissButton = { TextButton(onClick = { clear = false }) { Text("取消") } })
    if (reader) AlertDialog(onDismissRequest = { reader = false }, title = { Text("阅读器排版") }, text = { Column(Modifier.verticalScroll(rememberScrollState())) { ReaderPreferences(vm) } }, confirmButton = { TextButton(onClick = { reader = false }) { Text("完成") } })
}
