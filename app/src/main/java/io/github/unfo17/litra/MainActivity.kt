package io.github.unfo17.litra

import android.os.Bundle
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
    }
}
