package io.github.unfo17.litra

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.github.unfo17.litra.data.BookCategory
import io.github.unfo17.litra.data.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class LibraryBulkSelectionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var vm: LibraryViewModel
    private lateinit var scope: Category
    private lateinit var target: Category
    private val books = mutableListOf<io.github.unfo17.litra.data.Book>()
    private val fixtures = mutableListOf<File>()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun prepareRealEpubs() {
        compose.runOnUiThread { vm = ViewModelProvider(compose.activity)[LibraryViewModel::class.java] }
        runBlocking {
            vm.preferencesRepository.flag("list", false)
            scope = Category(name = "批量筛选 ${System.nanoTime()}")
            target = Category(name = "批量目标 ${System.nanoTime()}")
            vm.repository.dao.category(scope)
            vm.repository.dao.category(target)
            repeat(3) { index ->
                val fixture = ImportPersistenceTest.fixture(context, 120 + index)
                fixtures += fixture
                val book = vm.repository.import(Uri.fromFile(fixture))
                books += book
                vm.repository.dao.addToCategory(BookCategory(book.id, scope.id))
            }
            vm.filter.value = scope.id
        }
    }

    @After fun cleanup() {
        runBlocking {
            books.forEach { vm.repository.dao.book(it.id)?.let { book -> vm.repository.delete(book) } }
            vm.repository.dao.deleteCategories(listOf(scope.id, target.id))
        }
        fixtures.forEach(File::delete)
    }

    @Test fun gridAndListSelectionSupportsBulkActionsConfirmationAndBack() {
        compose.onNodeWithTag("tab-library").performClick()
        compose.waitUntil(5_000) { vm.preferences.value.listView.not() && vm.books.value?.count { it.id in books.map { book -> book.id } } == 3 }
        compose.onNodeWithTag("library-grid").assertExists()

        // Existing non-selection long-press still opens the single-book menu.
        compose.onNodeWithTag("library-book-${books.first().id}").performTouchInput { longClick() }
        compose.onNodeWithText("加入欲读清单").assertExists()
        compose.onNodeWithText("关闭").performClick()

        compose.onNodeWithTag("library-enter-selection").performClick()
        compose.onNodeWithTag("library-batch-actions").assertIsNotEnabled()
        compose.onNodeWithTag("library-select-all").performClick()
        compose.onNodeWithTag("library-selection-count").assertTextEquals("已选 3 本")
        compose.onNodeWithTag("library-book-${books[0].id}").assertIsSelected()
        compose.waitForIdle()
        Thread.sleep(350)
        saveScreenshot("library-bulk-grid.png")

        compose.onNodeWithTag("library-batch-actions").performClick()
        compose.onNodeWithText("加入自定义分类").performClick()
        compose.onNodeWithText(target.name).performClick()
        compose.waitUntil(5_000) {
            runBlocking { vm.repository.memberships.first().count { it.categoryId == target.id && it.bookId in books.map { b -> b.id } } == 3 }
        }
        compose.onNodeWithText("已将 3 本加入「${target.name}」").assertExists()
        compose.runOnUiThread { vm.message.value = null }
        compose.waitForIdle()
        compose.onNodeWithTag("library-enter-selection").assertExists()

        compose.onNodeWithContentDescription("筛选、排序与视图").performClick()
        compose.onNodeWithText("切换为列表").performClick()
        compose.waitUntil(5_000) { vm.preferences.value.listView }
        compose.onNodeWithTag("library-list").assertExists()
        compose.onNodeWithTag("library-enter-selection").performClick()
        compose.onNodeWithTag("library-select-all").performClick()
        compose.onNodeWithTag("library-selection-count").assertTextEquals("已选 3 本")
        compose.onNodeWithTag("library-select-all").performClick()
        compose.onNodeWithTag("library-selection-count").assertTextEquals("已选 0 本")
        compose.onNodeWithTag("library-batch-actions").assertIsNotEnabled()
        compose.onNodeWithTag("library-book-${books[1].id}").performClick()
        compose.onNodeWithTag("library-selection-count").assertTextEquals("已选 1 本")
        compose.waitForIdle()
        Thread.sleep(350)
        saveScreenshot("library-bulk-list.png")

        compose.onNodeWithTag("library-batch-actions").performClick()
        compose.onNodeWithTag("batch-add-want").performClick()
        compose.waitUntil(5_000) { runBlocking { vm.repository.dao.book(books[1].id)?.status == "WANT_TO_READ" } }
        compose.onNodeWithText("已将 1 本加入欲读清单").assertExists()

        compose.onNodeWithTag("library-enter-selection").performClick()
        compose.onNodeWithTag("library-book-${books[0].id}").performClick()
        compose.onNodeWithTag("library-batch-actions").performClick()
        compose.onNodeWithTag("batch-remove-books").performClick()
        compose.onNodeWithText("取消").performClick()
        assertNotNull(runBlocking { vm.repository.dao.book(books[0].id) })
        compose.onNodeWithTag("library-batch-actions").performClick()
        compose.onNodeWithTag("batch-remove-books").performClick()
        compose.onNodeWithTag("confirm-batch-remove").performClick()
        compose.waitUntil(5_000) { runBlocking { vm.repository.dao.book(books[0].id) == null } }
        compose.onNodeWithText("已移除 1 本图书").assertExists()

        compose.onNodeWithTag("library-enter-selection").performClick()
        compose.onNodeWithTag("library-book-${books[2].id}").performClick()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("library-enter-selection").assertExists()
        compose.onNodeWithTag("library-selection-count").assertDoesNotExist()
    }

    private fun saveScreenshot(name: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /sdcard/$name").close()
    }
}
