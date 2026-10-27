package io.github.unfo17.litra.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.*
import io.github.unfo17.litra.LibraryViewModel
import io.github.unfo17.litra.data.Book

val routes = listOf("home", "library", "library-list", "collections", "search", "finished", "detail/{id}", "reader/{id}", "goals", "settings")

@Composable
fun LitraApp(vm: LibraryViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val books by vm.books.collectAsState()
    val preferences by vm.preferences.collectAsState()
    val error by vm.error.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var selected by remember { mutableStateOf<Book?>(null) }
    var delete by remember { mutableStateOf<Book?>(null) }
    var categoryBook by remember { mutableStateOf<Book?>(null) }
    val categories by vm.categories.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { if (it.isNotEmpty()) vm.import(it) }
    val onImport = { picker.launch(arrayOf("application/epub+zip", "application/octet-stream")) }
    val read: (Book) -> Unit = { nav.navigate("reader/${it.id}") }
    val detail: (Book) -> Unit = { nav.navigate("detail/${it.id}") }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.message.value = null } }
    LitraTheme(preferences.appearance) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
            if (route in listOf("home", "library", "library-list", "search")) NavigationBar {
                val tabs = listOf(Triple("home", "主页", Icons.Default.Home), Triple("library", "书库", Icons.Default.LocalLibrary), Triple("search", "搜索", Icons.Default.Search))
                tabs.forEach { (id, label, icon) -> NavigationBarItem(modifier = Modifier.testTag("tab-$id"), selected = route == id || id == "library" && route == "library-list", onClick = { nav.navigate(if (id == "library" && preferences.listView) "library-list" else id) { launchSingleTop = true; popUpTo("home") } }, icon = { Icon(icon, null) }, label = { Text(label) }) }
            }
        }, contentWindowInsets = WindowInsets(0)) { padding ->
            NavHost(nav, startDestination = "home", modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
                composable("home") { HomeScreen(vm, books, read, detail, { selected = it }, { nav.navigate("goals") }, { nav.navigate(if (preferences.listView) "library-list" else "library") }) }
                listOf("library", "library-list").forEach { id -> composable(id) {
                    LibraryScreen(vm, books, id == "library-list", onView = { list -> vm.flagPreference("list", list); nav.navigate(if (list) "library-list" else "library") { popUpTo(id) { inclusive = true } } }, onImport, { nav.navigate("collections") }, read, { selected = it })
                } }
                composable("collections") { CollectionsScreen(vm, { nav.popBackStack() }, { nav.navigate("finished") }) }
                composable("finished") { FinishedScreen(vm, books, { nav.popBackStack() }, detail) }
                composable("search") { SearchScreen(vm, books, read, detail, { selected = it }, { nav.navigate("settings") }) }
                composable("detail/{id}") { e -> DetailScreen(vm, books?.find { it.id == e.arguments?.getString("id") }, books == null, { nav.popBackStack() }, read) }
                composable("reader/{id}") { e -> ReaderScreen(e.arguments?.getString("id").orEmpty(), vm, { nav.popBackStack() }) }
                composable("goals") { GoalsScreen(vm, { nav.popBackStack() }) }
                composable("settings") { SettingsScreen(vm, { nav.popBackStack() }, { nav.navigate("goals") }) }
            }
        }
        selected?.let { book -> AlertDialog(onDismissRequest = { selected = null }, title = { Text(book.title) }, text = { Column {
            TextButton(onClick = { selected = null; read(book) }) { Text("阅读") }
            if (book.status != "FINISHED") TextButton(onClick = { vm.want(book); selected = null }) { Text("加入欲读清单") }
            TextButton(onClick = { vm.finish(book); selected = null }) { Text("标记已读完") }
            TextButton(onClick = { categoryBook = book; selected = null }) { Text("加入分类") }
            TextButton(onClick = { selected = null; detail(book) }) { Text("图书信息") }
            TextButton(onClick = { delete = book; selected = null }) { Text("移除") }
        } }, confirmButton = { TextButton(onClick = { selected = null }) { Text("关闭") } }) }
        delete?.let { book -> AlertDialog(onDismissRequest = { delete = null }, title = { Text("移除《${book.title}》？") }, text = { Text("此操作会删除 Litra 内的文件和阅读记录，原始文件不受影响。") }, confirmButton = { TextButton(onClick = { vm.delete(book); delete = null }) { Text("移除") } }, dismissButton = { TextButton(onClick = { delete = null }) { Text("取消") } }) }
        categoryBook?.let { book -> AlertDialog(onDismissRequest = { categoryBook = null }, title = { Text("加入分类") }, text = { Column {
            if (categories.isEmpty()) Text("请先在藏书选项中添加分类。")
            categories.forEach { category -> TextButton(onClick = { vm.addCategory(book, category); categoryBook = null }) { Text(category.name) } }
        } }, confirmButton = { TextButton(onClick = { categoryBook = null }) { Text("关闭") } }) }
        error?.let { AlertDialog(onDismissRequest = { vm.error.value = null }, title = { Text("操作未完成") }, text = { Text(it) }, confirmButton = { TextButton(onClick = { vm.error.value = null }) { Text("知道了") } }) }
    }
}
