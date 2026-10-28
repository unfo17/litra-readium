package io.github.unfo17.litra

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ReaderChromeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun centerTapZoneHidesAndRestoresReaderChrome() {
        lateinit var vm: LibraryViewModel
        compose.runOnUiThread { vm = ViewModelProvider(compose.activity)[LibraryViewModel::class.java] }
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val fixture = ImportPersistenceTest.fixture(context, 74)
        val book = runBlocking { vm.repository.import(android.net.Uri.fromFile(fixture)) }
        try {
            compose.onNodeWithTag("tab-library").performClick()
            compose.onNodeWithContentDescription("《${book.title}》封面").performClick()
            compose.waitUntil(20_000) { compose.onAllNodesWithTag("reader-center-tap-zone").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("reader-top").assertExists()
            compose.onNodeWithTag("reader-bottom").assertExists()
            compose.onNodeWithTag("reader-center-tap-zone").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("reader-top").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("reader-top").assertDoesNotExist()
            compose.onNodeWithTag("reader-bottom").assertDoesNotExist()
            compose.onNodeWithTag("reader-center-tap-zone").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("reader-top").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("reader-top").assertExists()
            compose.onNodeWithTag("reader-bottom").assertExists()
        } finally {
            runBlocking { vm.repository.delete(vm.repository.dao.book(book.id) ?: book) }
            fixture.delete()
        }
    }
}
