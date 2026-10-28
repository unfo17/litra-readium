package io.github.unfo17.litra

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.unfo17.litra.ui.*
import org.junit.Rule
import org.junit.Test

class HomeModulesTest {
    @get:Rule val compose = createComposeRule()
    @Test fun allEightEmptyAndNonEmptyCombinationsCollapseCompletely() {
        var mask by mutableIntStateOf(0)
        val names = listOf("继续阅读", "欲读清单", "之前读过")
        compose.setContent { LitraTheme { Column {
            names.forEachIndexed { index, name -> HomeModule(name, mask and (1 shl index) != 0) { Text("$name 内容") } }
            Text("阅读目标"); Text("今年读过 · 0 本")
        } } }
        repeat(8) { next ->
            compose.runOnIdle { mask = next }
            names.forEachIndexed { index, name ->
                if (next and (1 shl index) != 0) { compose.onNodeWithText(name).assertExists(); compose.onNodeWithText("$name 内容").assertExists() }
                else { compose.onNodeWithText(name).assertDoesNotExist(); compose.onNodeWithText("$name 内容").assertDoesNotExist() }
            }
            compose.onNodeWithText("阅读目标").assertExists(); compose.onNodeWithText("今年读过 · 0 本").assertExists()
        }
    }
}
