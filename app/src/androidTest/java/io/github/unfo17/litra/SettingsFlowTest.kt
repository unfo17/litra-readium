package io.github.unfo17.litra

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import java.io.File
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*

class SettingsFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun appearancePersistsAndGoalRouteOpens() {
        lateinit var vm: LibraryViewModel
        compose.runOnUiThread { vm = ViewModelProvider(compose.activity)[LibraryViewModel::class.java] }
        val original = vm.preferences.value.appearance
        val originalTapPaging = vm.preferences.value.tapPaging
        try {
            compose.onNodeWithTag("tab-search").performClick()
            compose.onNodeWithContentDescription("设置").performClick()
            compose.onNodeWithTag("tab-search").assertDoesNotExist()
            compose.onNodeWithText("点按翻页").assertExists()
            compose.onNodeWithTag("setting-tap-paging-switch").performClick()
            compose.waitUntil(5000) { vm.preferences.value.tapPaging != originalTapPaging }
            compose.onNodeWithTag("setting-tap-paging-switch").performClick()
            compose.waitUntil(5000) { vm.preferences.value.tapPaging == originalTapPaging }
            androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().getExternalFilesDir(null)?.let { directory ->
                val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
                val screenshotFile = File(directory, "settings-tap-paging.png")
                screenshotFile.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                screenshot.recycle()
                val result = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cp ${screenshotFile.absolutePath} /sdcard/Download/settings-tap-paging.png")
                ParcelFileDescriptor.AutoCloseInputStream(result).use { it.readBytes() }
            }
            compose.onNodeWithText("深色").performClick()
            compose.waitUntil(5000) { vm.preferences.value.appearance == "DARK" }
            compose.onNodeWithText("阅读目标").performClick()
            compose.onNodeWithText("保存目标").assertExists()
            compose.onNodeWithContentDescription("返回").performClick()
            compose.onNodeWithText("阅读器排版").performClick()
            compose.onNodeWithText("连续滚动").assertExists()
            compose.onNodeWithText("完成").performClick()
            compose.onNodeWithText("开源许可").performScrollTo().performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithText("Litra — third-party notices", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("关闭").performClick()
        } finally { runBlocking { vm.preferencesRepository.text("appearance", original); vm.preferencesRepository.flag("tapPaging", originalTapPaging) } }
    }
}
