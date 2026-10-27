package io.github.unfo17.litra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import io.github.unfo17.litra.ui.LitraTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LitraTheme { LitraApp() } }
    }
}

val routes = listOf("home", "library", "library-list", "collections", "search", "finished", "detail/{id}", "reader/{id}", "goals", "settings")
@Composable fun LitraApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    Scaffold(bottomBar = {
        if (route in listOf("home", "library", "search")) NavigationBar {
            listOf("home" to "主页", "library" to "书库", "search" to "搜索").forEach { (id, label) ->
                NavigationBarItem(selected = route == id, onClick = { nav.navigate(id) { launchSingleTop = true; popUpTo("home") } }, icon = { Text(label.take(1)) }, label = { Text(label) })
            }
        }
    }) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            routes.forEach { id -> composable(id) { Text("Litra · $id") } }
        }
    }
}
