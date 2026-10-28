package io.github.unfo17.litra

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.unfo17.litra.ui.LitraApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: LibraryViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LitraApp(viewModel) }
        if (savedInstanceState == null) processExternalIntent(intent)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processExternalIntent(intent)
    }
    internal fun processExternalIntent(intent: Intent) {
        val uris = ExternalBooks.uris(intent)
        if (uris.isNotEmpty()) viewModel.openExternal(uris)
    }
}

object ExternalBooks {
    @Suppress("DEPRECATION")
    fun uris(intent: Intent): List<Uri> {
        if (intent.action !in listOf(Intent.ACTION_VIEW, Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) return emptyList()
        return buildList {
            intent.data?.let(::add)
            intent.clipData?.let { clip -> repeat(clip.itemCount) { clip.getItemAt(it).uri?.let(::add) } }
            if (intent.action != Intent.ACTION_SEND_MULTIPLE) (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let(::add)
            if (intent.action == Intent.ACTION_SEND_MULTIPLE) intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(::addAll)
        }.filter { it.scheme in listOf("content", "file") }.distinct().take(100)
    }
}
