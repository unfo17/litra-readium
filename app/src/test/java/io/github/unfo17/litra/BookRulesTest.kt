package io.github.unfo17.litra

import io.github.unfo17.litra.data.*
import org.junit.Assert.*
import org.junit.Test

class BookRulesTest {
    private fun book(status: String = "READING", progress: Double = 0.0) = Book(title = "Test", authors = "", fileName = "test.epub", hash = "test", status = status, progress = progress)
    @Test fun endIs99AndNeedsManualCompletion() {
        assertEquals(0.99, BookRules.progress(1.0), 0.0)
        assertEquals("99%", book(progress = BookRules.progress(1.0)).progressLabel)
        assertTrue(book(progress = 0.99).needsFinish)
        assertFalse(book(progress = 0.98).needsFinish)
    }
    @Test fun finishedSurvivesRereadingAndHasNoCompletionPrompt() {
        assertEquals("FINISHED", BookRules.readingStatus("FINISHED"))
        assertEquals("已读完", book("FINISHED", 0.5).progressLabel)
        assertFalse(book("FINISHED", 0.99).needsFinish)
        assertEquals("READING", BookRules.readingStatus("WANT_TO_READ"))
    }
    @Test fun invalidProgressIsBounded() {
        assertEquals(0.0, BookRules.progress(Double.NaN), 0.0)
        assertEquals(0.0, BookRules.progress(-1.0), 0.0)
        assertEquals(0.99, BookRules.progress(2.0), 0.0)
    }
    @Test fun detailTagsUseRealMetadataAndDeduplicate() {
        val book = Book(title = "Test", authors = "Author", fileName = "test.epub", hash = "test", kind = "漫画", language = "en", format = "epub", size = 2L * 1024 * 1024)
        assertEquals(listOf("科幻", "语言 en", "EPUB", "2.0 MB"), book.detailTags(listOf(" 科幻 ", "科幻")))
    }
    @Test fun detailTagsSkipMissingMetadata() {
        val book = Book(title = "Test", authors = "", fileName = "test.epub", hash = "test", format = "")
        assertTrue(book.detailTags(emptyList()).isEmpty())
        assertEquals("1 KB", fileSizeLabel(1024))
    }
    @Test fun editableMetadataIsTrimmedAndRestrictedToSupportedKinds() {
        assertEquals("New title", BookMetadataRules.title("  New title  "))
        assertTrue(runCatching { BookMetadataRules.title(" \n ") }.isFailure)
        assertEquals("", BookMetadataRules.authors("  "))
        assertEquals("漫画", BookMetadataRules.kind("漫画"))
        assertEquals("图书", BookMetadataRules.kind("图书"))
        assertTrue(runCatching { BookMetadataRules.kind("PDF") }.isFailure)
    }
}
