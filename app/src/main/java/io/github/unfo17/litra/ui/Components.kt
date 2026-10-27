package io.github.unfo17.litra.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.unfo17.litra.data.Book
import java.io.File

@Composable
fun Cover(book: Book, file: (String) -> File, modifier: Modifier = Modifier) {
    Surface(modifier.aspectRatio(2f / 3f), shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        if (book.coverName != null) AsyncImage(model = file(book.coverName), contentDescription = "《${book.title}》封面", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        else Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) { Text(book.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageBar(title: String, back: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(title = { Text(title) }, navigationIcon = { if (back != null) IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }, actions = actions)
}

@Composable
fun EmptyState(title: String, description: String = "", action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (description.isNotBlank()) Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) FilledTonalButton(onClick = onAction) { Text(action) }
    }
}
