package io.github.unfo17.litra

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import org.junit.*

class SettingsFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun appearancePersistsAndGoalRouteOpens() {
        lateinit var vm: LibraryViewModel
        compose.runOnUiThread { vm = ViewModelProvider(compose.activity)[LibraryViewModel::class.java] }
        val original = vm.preferences.value.appearance
        try {
            compose.onNodeWithTag("tab-search").performClick()
            compose.onNodeWithContentDescription("设置").performClick()
            compose.onNodeWithTag("tab-search").assertDoesNotExist()
            compose.onNodeWithText("深色").performClick()
            compose.waitUntil(5000) { vm.preferences.value.appearance == "DARK" }
            compose.onNodeWithText("阅读目标").performClick()
            compose.onNodeWithText("保存目标").assertExists()
            compose.onNodeWithContentDescription("返回").performClick()
            compose.onNodeWithText("阅读器排版").performClick()
            compose.onNodeWithText("连续滚动").assertExists()
            compose.onNodeWithText("完成").performClick()
        } finally { runBlocking { vm.preferencesRepository.text("appearance", original) } }
    }
}
