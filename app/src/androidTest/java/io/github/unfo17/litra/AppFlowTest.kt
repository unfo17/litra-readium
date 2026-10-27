package io.github.unfo17.litra

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import io.github.unfo17.litra.data.Book
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun tabsViewsHistoryAndManualCompletion() {
        lateinit var vm: LibraryViewModel
        compose.runOnUiThread { vm = ViewModelProvider(compose.activity)[LibraryViewModel::class.java] }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fixture = ImportPersistenceTest.fixture(context, 30)
        val book = runBlocking { vm.repository.dao.clearHistory(); vm.repository.import(Uri.fromFile(fixture)) }
        try {
            compose.onNodeWithTag("tab-search").performClick()
            compose.onNodeWithTag("search-history").assertDoesNotExist()
            compose.onNodeWithText("书名或作者").performTextInput(book.title)
            compose.onNodeWithText("书名或作者").performImeAction()
            compose.onNodeWithContentDescription("清空搜索").performClick()
            compose.waitUntil(5000) { vm.history.value.isNotEmpty() }
            compose.onNodeWithTag("search-history").assertExists()
            compose.onNodeWithTag("tab-library").performClick()
            compose.onNodeWithContentDescription("筛选、排序与视图").performClick()
            val target = if (vm.preferences.value.listView) "切换为双列封面" else "切换为列表"
            compose.onNodeWithText(target).performClick()
            compose.onNodeWithTag(if (target == "切换为列表") "library-list" else "library-grid").assertExists()
            compose.onNodeWithContentDescription("藏书选项").performClick()
            compose.onNodeWithTag("tab-library").assertDoesNotExist()
            compose.onNodeWithText("已读完").performClick()
            compose.onNodeWithTag("tab-library").assertDoesNotExist()
            compose.onAllNodesWithText("书库").assertCountEquals(0)
            compose.onNodeWithContentDescription("返回").performClick()
            compose.onNodeWithContentDescription("返回").performClick()
            runBlocking { vm.repository.progress(book.id, "{\"href\":\"EPUB/chapter.xhtml\",\"type\":\"application/xhtml+xml\",\"locations\":{\"totalProgression\":1.0}}", 1.0) }
            compose.onNodeWithTag("tab-home").performClick()
            compose.onNodeWithTag("finish-prompt-${book.id}").assertExists().performClick()
            compose.waitUntil(5000) { vm.books.value?.find { it.id == book.id }?.status == "FINISHED" }
            runBlocking { vm.repository.progress(book.id, "{\"href\":\"EPUB/chapter.xhtml\"}", .5) }
            assertEquals("FINISHED", runBlocking { vm.repository.dao.book(book.id)!!.status })
            compose.onNodeWithTag("finish-prompt-${book.id}").assertDoesNotExist()
        } finally {
            runBlocking { vm.repository.delete(vm.repository.dao.book(book.id) ?: book); vm.repository.dao.clearHistory() }
            fixture.delete()
        }
    }
}
