package io.github.unfo17.litra

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.github.unfo17.litra.data.Book
import io.github.unfo17.litra.data.Category
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class BookMetadataEditTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var vm: LibraryViewModel
    private lateinit var book: Book
    private lateinit var category: Category
    private lateinit var fixture: File
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun importRealEpub() {
        compose.runOnUiThread { vm = ViewModelProvider(compose.activity)[LibraryViewModel::class.java] }
        fixture = ImportPersistenceTest.fixture(context, 87)
        runBlocking {
            book = vm.repository.import(Uri.fromFile(fixture))
            vm.repository.progress(book.id, "{\"href\":\"EPUB/chapter.xhtml\",\"locations\":{\"totalProgression\":0.42}}", .42)
            vm.repository.finish(book.id)
            category = Category(name = "Metadata edit ${System.nanoTime()}")
            vm.repository.dao.category(category)
            vm.repository.dao.addToCategory(io.github.unfo17.litra.data.BookCategory(book.id, category.id))
        }
    }

    @After fun cleanup() {
        runBlocking { vm.repository.dao.book(book.id)?.let { vm.repository.delete(it) }; vm.repository.dao.deleteCategories(listOf(category.id)) }
        fixture.delete()
    }

    @Test fun editIsValidatedPersistedAndReflectedInLibraryHomeAndSearch() {
        compose.onNodeWithTag("tab-search").performClick()
        compose.onNodeWithText("书名或作者").performTextInput(book.title)
        compose.onAllNodesWithText(book.title).onLast().performTouchInput { longClick() }
        compose.onNodeWithText("图书信息").performClick()
        compose.onNodeWithTag("edit-book-metadata").performClick()

        compose.onNodeWithTag("edit-book-title").performTextClearance()
        compose.onNodeWithTag("save-book-metadata").assertIsNotEnabled()
        compose.onNodeWithTag("edit-book-title").performTextInput("  Edited Litra Book  ")
        compose.onNodeWithTag("edit-book-authors").performTextClearance()
        compose.onNodeWithTag("edit-book-kind-漫画").performClick()
        compose.onNodeWithTag("save-book-metadata").performClick()
        compose.waitUntil(5_000) { vm.books.value?.find { it.id == book.id }?.title == "Edited Litra Book" }
        val edited = runBlocking { vm.repository.dao.book(book.id)!! }
        assertEquals("Edited Litra Book", edited.title)
        assertEquals("", edited.authors)
        assertEquals("漫画", edited.kind)
        assertEquals(.42, edited.progress, 0.0)
        assertEquals("FINISHED", edited.status)
        assertEquals(book.hash, edited.hash)
        assertTrue(runBlocking { vm.repository.memberships.first().any { it.bookId == book.id && it.categoryId == category.id } })
        val publication = runBlocking { vm.repository.readium.open(vm.repository.file(edited.fileName)) }
        try { assertEquals("Litra EPUB 87", publication.metadata.title) } finally { publication.close() }

        compose.onNodeWithText("Edited Litra Book", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("类型 · 漫画", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithTag("tab-library").performClick()
        assertTrue(compose.onAllNodesWithText("Edited Litra Book").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithTag("tab-home").performClick()
        assertTrue(compose.onAllNodesWithText("Edited Litra Book").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithTag("tab-search").performClick()
        compose.onNodeWithText("书名或作者").performTextClearance()
        compose.onNodeWithText("书名或作者").performTextInput("Edited Litra Book")
        assertTrue(compose.onAllNodesWithText("Edited Litra Book").fetchSemanticsNodes().isNotEmpty())

        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { vm.books.value?.find { it.id == book.id }?.title == "Edited Litra Book" }
        compose.onNodeWithTag("tab-search").performClick()
        compose.onNodeWithText("书名或作者").performTextClearance()
        compose.onNodeWithText("书名或作者").performTextInput("Edited Litra Book")
        compose.onAllNodesWithText("Edited Litra Book").onLast().performTouchInput { longClick() }
        compose.onNodeWithText("图书信息").performClick()
        compose.onNodeWithText("Edited Litra Book", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("类型 · 漫画", useUnmergedTree = true).assertExists()
        compose.waitForIdle()
        Thread.sleep(800) // let the detail navigation fade finish before capturing the screen
        compose.waitForIdle()
        saveScreenshot("metadata-edit-detail.png")
    }

    private fun saveScreenshot(name: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /sdcard/$name").close()
    }
}
